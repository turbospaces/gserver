package com.katesoft.gserver.transport;

import static com.katesoft.gserver.core.Encryptors.encode;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static java.lang.System.currentTimeMillis;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;

import org.apache.commons.chain.Chain;
import org.apache.commons.chain.impl.ChainBase;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import com.katesoft.gserver.api.AbstractProtocolException;
import com.katesoft.gserver.api.DeadConnectionException;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.api.UserConnection.UserConnectionStub;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;
import com.katesoft.gserver.core.Encryptors;
import com.katesoft.gserver.core.NetworkCommandContext;
import com.katesoft.gserver.spi.PlatformContext;

@Sharable
public class ChannelDispatchHandler extends SimpleChannelInboundHandler<Object> implements Closeable, Supplier<ChannelGroup> {
    private static final Logger LOGGER = LoggerFactory.getLogger( ChannelDispatchHandler.class );
    private static AttributeKey<SocketUserConnection> USER_CONNECTION_ATTR = new AttributeKey<SocketUserConnection>( "x-user-connection" );
    private static AttributeKey<WebSocketServerHandshaker> WS_HANDSHAKER_ATTR = new AttributeKey<WebSocketServerHandshaker>( "x-ws-handshaker" );

    private final TextEncryptor encryptor = Encryptors.textEncryptor( ChannelDispatchHandler.class.getName(), false );
    private final ConcurrentMap<String, SocketUserConnection> connections = Maps.newConcurrentMap();
    private final ChannelGroup channelGroup;
    private final AtomicLong increment;
    private final PlatformContext platformInterface;

    public ChannelDispatchHandler(EventLoopGroup eventGroup, PlatformContext platformInterface) {
        this.platformInterface = platformInterface;
        this.increment = new AtomicLong();
        this.channelGroup = new DefaultChannelGroup( eventGroup.next() );
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        SocketChannel ch = (SocketChannel) ctx.pipeline().channel();
        String id = encode( encryptor, String.valueOf( currentTimeMillis() ), String.valueOf( increment.getAndIncrement() ) );
        SocketUserConnection uc = new SocketUserConnection( ch, id );
        ch.attr( USER_CONNECTION_ATTR ).set( uc );
        connections.put( id, uc );
        channelGroup.add( ch );
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SocketUserConnection c = ctx.channel().attr( USER_CONNECTION_ATTR ).get();
        LOGGER.error( String.format( "Unhandled exception occured in UserConnection=(%s) loop", c.id() ), cause );
        super.exceptionCaught( ctx, cause );
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SocketUserConnection c = ctx.channel().attr( USER_CONNECTION_ATTR ).get();
        super.channelInactive( ctx );
        LOGGER.info( "channel={} close for UserConnection=({}). active connections left={}", ctx.channel(), c.id(), get().size() );
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        SocketUserConnection userConnection = ctx.channel().attr( USER_CONNECTION_ATTR ).get();
        if ( msg instanceof BaseCommand ) {
            userConnection.setConnectionType( ConnectionType.TCP );
            onMessage( (BaseCommand) msg, userConnection, ctx );
        }
        else if ( msg instanceof FullHttpRequest ) {
            FullHttpRequest httpMsg = (FullHttpRequest) msg;
            WebSocketServerHandshaker handshaker = new WebSocketServerHandshakerFactory(
                    "ws://" + httpMsg.headers().get( HOST ) + "/websockets",
                    null,
                    false ).newHandshaker( httpMsg );
            if ( handshaker == null ) {
                WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse( ctx.channel() );
            }
            else {
                handshaker.handshake( ctx.channel(), httpMsg );
                ctx.channel().attr( WS_HANDSHAKER_ATTR ).set( handshaker );
            }
            userConnection.setConnectionType( ConnectionType.WEBSOCKETS );
        }
        else if ( msg instanceof WebSocketFrame ) {
            WebSocketFrame frame = (WebSocketFrame) msg;
            if ( frame instanceof CloseWebSocketFrame ) {
                LOGGER.debug( "ws connection({}) closed", userConnection.id() );
                frame.retain();
                Attribute<WebSocketServerHandshaker> attr = ctx.attr( WS_HANDSHAKER_ATTR );
                if ( attr != null ) {
                    WebSocketServerHandshaker wsHandshaker = attr.get();
                    try {
                        if ( wsHandshaker != null ) {
                            wsHandshaker.close( ctx.channel(), (CloseWebSocketFrame) frame );
                        }
                    }
                    finally {
                        ctx.attr( WS_HANDSHAKER_ATTR ).remove();
                    }
                }
            }
            else if ( frame instanceof PingWebSocketFrame ) {
                frame.content().retain();
                ctx.channel().write( new PongWebSocketFrame( frame.content() ) );
                return;
            }
            else if ( frame instanceof TextWebSocketFrame ) {
                String text = ( (TextWebSocketFrame) frame ).text();
                LOGGER.debug( "ws({})={}", userConnection.id(), text );
                Builder bcmdb = BaseCommand.newBuilder();
                JsonFormat.merge( text, platformInterface.commandsCodec().extensionRegistry(), bcmdb );
                onMessage( bcmdb.build(), userConnection, ctx );
            }
            else if ( frame instanceof BinaryWebSocketFrame ) {
                byte[] data = frame.content().array();
                BaseCommand bcmd = BaseCommand.parseFrom( data, platformInterface.commandsCodec().extensionRegistry() );
                onMessage( bcmd, userConnection, ctx );
            }
        }
    }
    @Override
    public void close() {
        for ( UserConnection userConnection : connections.values() ) {
            try {
                userConnection.close();
            }
            catch ( Exception e ) {
                LOGGER.error( e.getMessage(), e );
            }
        }
    }
    @Override
    public ChannelGroup get() {
        return channelGroup;
    }
    UserConnection find(String id) {
        return connections.get( id );
    }
    UserConnection find(final SocketChannel sch) {
        return Iterables.find( connections.values(), new Predicate<SocketUserConnection>() {
            @Override
            public boolean apply(@Nullable SocketUserConnection input) {
                return input.get() == sch;
            }
        } ).get().attr( USER_CONNECTION_ATTR ).get();
    }
    protected void onMessage(final BaseCommand cmd, final SocketUserConnection uc, final ChannelHandlerContext channelCtx) {
        uc.inboundCommands.offer( cmd );

        try {
            BaseCommand poll = null;
            ReentrantLock lock = uc.cmdLock;
            if ( lock.isLocked() ) {
                LOGGER.warn(
                        "Locking EventLoop({}) due to slow inbound messages processing(size={})!",
                        uc.get().eventLoop(),
                        uc.inboundCommands.size() );
            }
            lock.lock();
            try {
                while ( ( poll = uc.inboundCommands.poll() ) != null ) {
                    LOGGER.debug( "onMessage(connection={})={}", uc.id(), poll );

                    // ~~~ build chain ~~~
                    NetworkCommandContext ncmd = new NetworkCommandContext( cmd, platformInterface.commandsCodec(), uc );
                    Chain chain = new ChainBase();
                    chain.addCommand( platformInterface.platformCommandsInterpreter() );
                    if ( uc.associatedPlayer() != null ) {
                        chain.addCommand( uc.associatedPlayer() );
                    }

                    // ~~~ execute command ~~~
                    // ~~~ catch common exceptions ~~~
                    try {
                        ncmd.recognizeCmd();
                        boolean processed = chain.execute( ncmd );
                        if ( !processed ) {
                            throw new AbstractProtocolException.UnknownCommadException( cmd );
                        }
                    }
                    catch ( AbstractProtocolException ex ) {
                        LOGGER.error( String.format( "ProtocolException=UUID[%s], msg=%s", ex.getUuid().toString(), ex.getMessage() ), ex );
                        uc.writeAsync( com.katesoft.gserver.commands.Commands.Exception
                                .newBuilder()
                                .setHeaders( ncmd.getCmd().getHeaders() )
                                .setMsg( ex.getMessage() )
                                .setQualifier( ex.getClass().getName() )
                                .setStacktrace( ExceptionUtils.getStackTrace( ex ) )
                                .setUuid( ex.getUuid().toString() )
                                .build() );
                    }
                    catch ( DataRetrievalFailureException ex ) {
                        LOGGER.error( ex.getMessage(), ex );
                        throw ex;
                    }
                    catch ( ConcurrencyFailureException ex ) {
                        LOGGER.error( ex.getMessage(), ex );
                        throw ex;
                    }
                    catch ( DeadConnectionException ex ) {
                        LOGGER.error( ex.getMessage(), ex );
                        LOGGER.error( "closing connection = {} due to failed game login procedure", uc );
                        uc.close();
                    }
                    catch ( Throwable ex ) {
                        LOGGER.error( ex.getMessage(), ex );
                    }
                }
            }
            finally {
                lock.unlock();
            }
        }
        finally {
            channelCtx.flush();
        }
    }

    static final class SocketUserConnection extends UserConnectionStub implements Supplier<SocketChannel> {
        private final SocketChannel ch;
        private final Queue<BaseCommand> inboundCommands = new ConcurrentLinkedQueue<BaseCommand>();
        private final ReentrantLock cmdLock = new ReentrantLock();

        public SocketUserConnection(SocketChannel ch, String id) {
            super( id );
            this.ch = ch;
        }
        @Override
        public InetSocketAddress remoteAddress() {
            return ch.remoteAddress();
        }
        @Override
        public SocketChannel get() {
            return ch;
        }
        @Override
        public Future<Void> writeAsync(Message message) {
            Future<Void> f = null;
            switch ( connectionType ) {
                case TCP: {
                    LOGGER.debug( "sending TCP response={}", message );
                    f = ch.write( message );
                    break;
                }
                case WEBSOCKETS: {
                    String json = JsonFormat.printToString( message );
                    LOGGER.debug( "sending ws response={}", json );
                    ch.write( new TextWebSocketFrame( json ) );
                    break;
                }
                case FLASH:
                    LOGGER.debug( "sending flash response={}", message );
                    f = ch.write( message );
                    break;
            }
            return f;
        }
        @Override
        public void writeSync(Message message) {
            writeAsync( message ).awaitUninterruptibly();
        }
        @Override
        public void close() {
            ch.close().awaitUninterruptibly();
        }
    }
}

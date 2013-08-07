package com.katesoft.gserver.transport;

import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static com.katesoft.gserver.core.Encryptors.encode;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
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

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.chain.Chain;
import org.apache.commons.chain.impl.ChainBase;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
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
import com.katesoft.gserver.domain.Entities.ServerSettings;
import com.katesoft.gserver.spi.PlatformContext;
import com.katesoft.gserver.transport.ChannelDispatchHandler.SocketUserConnection;

@Sharable
public class ChannelDispatchHandler extends SimpleChannelInboundHandler<Object> implements Closeable,
                                                                               Supplier<ConcurrentMap<String, SocketUserConnection>> {
    private static final Logger LOGGER = LoggerFactory.getLogger( ChannelDispatchHandler.class );
    private static final AttributeKey<WebSocketServerHandshaker> WS_HANDSHAKER_ATTR = new AttributeKey<WebSocketServerHandshaker>( "x-ws-handshaker" );

    private final TextEncryptor encryptor = Encryptors.textEncryptor( ChannelDispatchHandler.class.getName(), false );
    private final ConcurrentMap<String, SocketUserConnection> connections = Maps.newConcurrentMap();
    private final AtomicLong increment = new AtomicLong();

    private final PlatformContext platformInterface;
    private final ServerSettings settings;

    public ChannelDispatchHandler(PlatformContext platformInterface, ServerSettings s) {
        this.platformInterface = platformInterface;
        this.settings = s;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String id = encode( encryptor, String.valueOf( currentTimeMillis() ), String.valueOf( increment.getAndIncrement() ) );
        {
            SocketUserConnection uc = new SocketUserConnection( (SocketChannel) ctx.channel(), id );
            SocketUserConnection.associateConnection( ctx, uc );
            connections.put( id, uc );
            LOGGER.info( "channel={} activated, corresponding UserConnection({})", ctx.channel(), uc.id() );
        }
        super.channelActive( ctx );
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SocketUserConnection c = SocketUserConnection.getConnection( ctx );
        LOGGER.error( String.format( "Unhandled exception occured in UserConnection=(%s) loop", c.id() ), cause );
        super.exceptionCaught( ctx, cause );
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SocketUserConnection c = SocketUserConnection.getConnection( ctx );
        connections.remove( c.id() );
        LOGGER.info( "channel={} close for UserConnection=({}). active connections left={}", ctx.channel(), c.id(), connections.size() );
        super.channelInactive( ctx );
    }
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        final SocketUserConnection userConnection = SocketUserConnection.getConnection( ctx );
        if ( msg instanceof BaseCommand ) {
            userConnection.setConnectionType( ConnectionType.TCP );
            onMessage( (BaseCommand) msg, userConnection );
        }
        else if ( msg instanceof FullHttpRequest ) {
            FullHttpRequest httpMsg = (FullHttpRequest) msg;
            String url = "ws://" + httpMsg.headers().get( HOST ) + settings.getWsContextPath();
            WebSocketServerHandshaker handshaker = new WebSocketServerHandshakerFactory( url, null, true ).newHandshaker( httpMsg );
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
                CloseWebSocketFrame f = (CloseWebSocketFrame) frame.retain();
                Attribute<WebSocketServerHandshaker> attr = ctx.channel().attr( WS_HANDSHAKER_ATTR );
                if ( attr != null ) {
                    WebSocketServerHandshaker wsHandshaker = attr.get();
                    try {
                        if ( wsHandshaker != null ) {
                            wsHandshaker.close( ctx.channel(), f );
                            LOGGER.debug( "ws connection({}) closed", userConnection.id() );
                        }
                    }
                    finally {
                        ctx.channel().attr( WS_HANDSHAKER_ATTR ).remove();
                    }
                }
            }
            else if ( frame instanceof PingWebSocketFrame ) {
                ctx.channel().write( new PongWebSocketFrame( frame.content().retain() ) );
                return;
            }
            else if ( frame instanceof TextWebSocketFrame ) {
                String text = ( (TextWebSocketFrame) frame ).text();
                LOGGER.debug( "ws({})={}", userConnection.id(), text );
                Builder bcmdb = BaseCommand.newBuilder();
                JsonFormat.merge( text, platformInterface.commandsCodec().extensionRegistry(), bcmdb );
                onMessage( bcmdb.build(), userConnection );
            }
            else if ( frame instanceof BinaryWebSocketFrame ) {
                byte[] data = frame.content().array();
                BaseCommand bcmd = BaseCommand.parseFrom( data, platformInterface.commandsCodec().extensionRegistry() );
                onMessage( bcmd, userConnection );
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
    public UserConnection awaitForClientHandshake(SocketChannel clientChannel) {
        for ( ;; ) {
            for ( SocketUserConnection c : connections.values() ) {
                SocketAddress remoteAddress = c.remoteAddress();
                if ( clientChannel.localAddress().equals( remoteAddress ) ) {
                    return c;
                }
            }
            sleepUninterruptibly( 1, MILLISECONDS );
        }
    }
    @Override
    public ConcurrentMap<String, SocketUserConnection> get() {
        return connections;
    }
    protected void onMessage(BaseCommand cmd, final SocketUserConnection uc) {
        uc.inboundCommands.add( cmd );

        BaseCommand poll = null;
        Lock lock = uc.cmdLock;
        lock.lock();
        try {
            while ( ( poll = (BaseCommand) uc.inboundCommands.poll() ) != null ) {
                LOGGER.debug( "onMessage(connection={})={}", uc.id(), poll );

                // ~~~ build chain ~~~
                NetworkCommandContext ncmd = new NetworkCommandContext( poll, platformInterface.commandsCodec(), uc );
                Chain chain = new ChainBase();
                chain.addCommand( platformInterface.platformCommandsInterpreter() );
                if ( uc.player() != null ) {
                    chain.addCommand( uc.player() );
                }

                // ~~~ execute command ~~~
                // ~~~ catch common exceptions ~~~
                try {
                    ncmd.recognizeCmd();
                    boolean processed = chain.execute( ncmd );
                    if ( !processed ) {
                        throw new AbstractProtocolException.UnknownCommadException( poll );
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
            uc.channel.flush();
        }
    }

    static final class SocketUserConnection extends UserConnectionStub {
        private static AttributeKey<SocketUserConnection> USER_CONNECTION_ATTR = new AttributeKey<SocketUserConnection>( "x-user-connection" );

        private final SocketChannel channel;
        private final Queue<Message> inboundCommands = new ConcurrentLinkedQueue<Message>();
        private final ReentrantLock cmdLock = new ReentrantLock();

        public SocketUserConnection(SocketChannel ch, String id) {
            super( id );
            this.channel = ch;
        }
        @Override
        public InetSocketAddress remoteAddress() {
            return channel.remoteAddress();
        }
        @Override
        public Future<Void> writeAsync(Message message) {
            Object toSend = message;
            switch ( connectionType ) {
                case TCP: {
                    LOGGER.debug( "sending TCP response={}", message );
                    break;
                }
                case WEBSOCKETS: {
                    String json = JsonFormat.printToString( message );
                    toSend = new TextWebSocketFrame( json );
                    LOGGER.debug( "sending ws response={}", json );
                    break;
                }
                case FLASH: {
                    LOGGER.debug( "sending flash response={}", message );
                    break;
                }
            }
            System.out.println( channel.eventLoop().inEventLoop() );
            return channel.write( toSend );
        }
        @Override
        public void writeSync(Message message) {
            try {
                getUninterruptibly( writeAsync( message ) );
            }
            catch ( ExecutionException e ) {
                Throwables.propagate( e );
            }
        }
        @Override
        public void close() {
            channel.close().awaitUninterruptibly();
        }
        public static void associateConnection(ChannelHandlerContext ctx, SocketUserConnection uc) {
            ctx.channel().attr( USER_CONNECTION_ATTR ).set( uc );
        }
        public static SocketUserConnection getConnection(ChannelHandlerContext ctx) {
            return ctx.channel().attr( USER_CONNECTION_ATTR ).get();
        }
    }
}

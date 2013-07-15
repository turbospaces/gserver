package com.katesoft.gserver.transport;

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import io.netty.channel.Channel;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import com.google.common.base.Supplier;
import com.googlecode.protobuf.format.JsonFormat;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.api.UserConnection.UserConnectionStub;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;
import com.katesoft.gserver.core.Encryptors;

@Sharable
class RootDispatchHandler extends SimpleChannelInboundHandler<Object> implements Closeable, Supplier<ChannelGroup> {
    private static final Logger LOGGER = LoggerFactory.getLogger( RootDispatchHandler.class );
    private static AttributeKey<SocketUserConnection> USER_CONNECTION_ATTR = new AttributeKey<SocketUserConnection>( "x-user-connection" );
    private static AttributeKey<WebSocketServerHandshaker> WS_HANDSHAKER_ATTR = new AttributeKey<WebSocketServerHandshaker>( "x-ws-handshaker" );

    private final ChannelGroup connections;
    private final TransportMessageListener eventBus;

    RootDispatchHandler(TransportMessageListener ml, EventLoopGroup eventGroup) {
        this.eventBus = ml;
        this.connections = new DefaultChannelGroup( eventGroup.next() );
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        SocketChannel ch = (SocketChannel) ctx.pipeline().channel();
        SocketUserConnection uc = new SocketUserConnection( ch, connections );
        ch.attr( USER_CONNECTION_ATTR ).set( uc );
        connections.add( ch );
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
            eventBus.onMessage( (BaseCommand) msg, userConnection );
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
                JsonFormat.merge( text, eventBus.extentionRegistry(), bcmdb );
                eventBus.onMessage( bcmdb.build(), userConnection );
            }
            else if ( frame instanceof BinaryWebSocketFrame ) {
                byte[] data = frame.content().array();
                BaseCommand bcmd = BaseCommand.parseFrom( data, eventBus.extentionRegistry() );
                eventBus.onMessage( bcmd, userConnection );
            }
        }
    }
    @Override
    public void close() {
        connections.close();
    }
    @Override
    public ChannelGroup get() {
        return connections;
    }
    UserConnection find(String id) {
        Integer x = Integer.parseInt( Encryptors.decode( SocketUserConnection.ENCRYPTOR, id )[1] );
        Channel ch = connections.find( x );
        return ch.attr( USER_CONNECTION_ATTR ).get();
    }
    UserConnection find(SocketChannel sch) {
        Channel ch = connections.find( sch.id() );
        return ch.attr( USER_CONNECTION_ATTR ).get();
    }

    static final class SocketUserConnection extends UserConnectionStub {
        private static final TextEncryptor ENCRYPTOR = Encryptors.textEncryptor( SocketUserConnection.class.getName(), false );

        private final SocketChannel ch;
        private final ChannelGroup connections;

        public SocketUserConnection(SocketChannel ch, ChannelGroup connections) {
            super( Encryptors.encode( ENCRYPTOR, SocketUserConnection.class.getSimpleName(), String.valueOf( ch.id() ) ) );
            this.ch = ch;
            this.connections = connections;
        }
        @Override
        public InetSocketAddress remoteAddress() {
            return ch.remoteAddress();
        }
        @Override
        public Future<Void> writeAsync(BaseCommand message) {
            Future<Void> f = null;
            switch ( connectionType ) {
                case TCP: {
                    if ( message.getDebug() ) {
                        LOGGER.debug( "sending TCP response={}", message );
                    }
                    f = ch.write( message );
                    break;
                }
                case WEBSOCKETS: {
                    String json = JsonFormat.printToString( message );
                    if ( message.getDebug() ) {
                        LOGGER.debug( "sending ws response={}", json );
                    }
                    ch.write( new TextWebSocketFrame( json ) );
                    break;
                }
                case FLASH:
                    if ( message.getDebug() ) {
                        LOGGER.debug( "sending flash response={}", message );
                    }
                    f = ch.write( message );
                    break;
            }
            return f;
        }
        @Override
        public void writeSync(BaseCommand message) {
            writeAsync( message ).awaitUninterruptibly();
        }
        @Override
        public Future<Void> writeAllAsync(BaseCommand message) {
            return connections.write( message );
        }
        @Override
        public void close() {
            ch.close().awaitUninterruptibly();
        }
    }
}

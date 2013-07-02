package com.katesoft.gserver.transport;

import static com.google.common.base.Objects.toStringHelper;
import static com.katesoft.gserver.misc.Misc.getPid;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static java.lang.System.currentTimeMillis;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
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
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.util.concurrent.Atomics;
import com.googlecode.protobuf.format.JsonFormat;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;

@Sharable
class RootDispatchHandler extends ChannelInboundMessageHandlerAdapter<Object> implements Closeable, Supplier<ChannelGroup> {
    private static final Logger LOGGER = LoggerFactory.getLogger( RootDispatchHandler.class );
    private static AttributeKey<SocketUserConnection> USER_CONNECTION_ATTR = new AttributeKey<SocketUserConnection>( "x-user-connection" );
    private static AttributeKey<WebSocketServerHandshaker> WS_HANDSHAKER_ATTR = new AttributeKey<WebSocketServerHandshaker>( "x-ws-handshaker" );

    private final ChannelGroup connections = new DefaultChannelGroup();
    private final TransportMessageListener eventBus;

    RootDispatchHandler(TransportMessageListener ml) {
        this.eventBus = ml;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        SocketChannel ch = (SocketChannel) ctx.pipeline().channel();
        SocketUserConnection uc = new SocketUserConnection( ch, connections );
        ch.attr( USER_CONNECTION_ATTR ).set( uc );
        connections.add( ch );
    }
    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        SocketUserConnection userConnection = ctx.channel().attr( USER_CONNECTION_ATTR ).get();
        if ( msg instanceof BaseCommand ) {
            userConnection.connectionType = ConnectionType.TCP;
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
            userConnection.connectionType = ConnectionType.WEBSOCKETS;
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
        Integer x = SocketUserConnection.decodeId( id );
        Channel ch = connections.find( x );
        return ch.attr( USER_CONNECTION_ATTR ).get();
    }
    UserConnection find(SocketChannel sch) {
        Channel ch = connections.find( sch.id() );
        return ch.attr( USER_CONNECTION_ATTR ).get();
    }

    static final class SocketUserConnection implements UserConnection {
        private static final BasicTextEncryptor ENCRYPTOR = new BasicTextEncryptor();
        static {
            ENCRYPTOR.setPassword( SocketUserConnection.class.getSimpleName() );
        }
        private final String id;
        private final SocketChannel ch;
        private final ChannelGroup connections;
        private final AtomicReference<Player> player = Atomics.newReference();
        private ConnectionType connectionType = ConnectionType.TCP;

        public SocketUserConnection(SocketChannel ch, ChannelGroup connections) {
            this.ch = ch;
            this.connections = connections;
            this.id = encodeId( ch );
        }
        @Override
        public String id() {
            return id;
        }
        @Override
        public String toString() {
            return toStringHelper( this ).add( "id", id() ).add( "accepted", new Date( socketAcceptTimestamp() ) ).toString();
        }
        private static String encodeId(SocketChannel ch) {
            /**
             * 1. timestamp
             * 2. netty socket connection id
             * 3. process id
             */
            String id = currentTimeMillis() + ":" + ch.id() + ":" + getPid();
            return ENCRYPTOR.encrypt( id );
        }
        private static Integer decodeId(String id) {
            String[] items = ENCRYPTOR.decrypt( id ).split( ":" );
            return Integer.parseInt( items[1] );
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
        public long socketAcceptTimestamp() {
            String[] items = ENCRYPTOR.decrypt( id ).split( ":" );
            return Long.parseLong( items[0] );
        }
        @Override
        public Player asociatePlayer(Player p) {
            Player prev = player.get();
            player.compareAndSet( prev, p );
            return prev;
        }
        @Override
        public Player getAssociatedPlayer() {
            return player.get();
        }
        @Override
        public ConnectionType connectionType() {
            return connectionType;
        }
        @Override
        public void close() {
            ch.close().awaitUninterruptibly();
        }
    }
}

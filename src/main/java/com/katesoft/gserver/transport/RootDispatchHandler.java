package com.katesoft.gserver.transport;

import static com.google.common.base.Objects.toStringHelper;
import static com.katesoft.gserver.misc.Misc.getPid;
import static java.lang.System.currentTimeMillis;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;

import java.io.Closeable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.jasypt.util.text.BasicTextEncryptor;

import com.google.common.base.Supplier;
import com.google.common.util.concurrent.Atomics;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;

@Sharable
class RootDispatchHandler extends ChannelInboundMessageHandlerAdapter<BaseCommand> implements Closeable, Supplier<ChannelGroup> {
    private static AttributeKey<SocketUserConnection> USER_CONNECTION_ATTR = new AttributeKey<SocketUserConnection>( "x-user-connection" );

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
    public void messageReceived(ChannelHandlerContext ctx, BaseCommand cmd) throws Exception {
        SocketUserConnection userConnection = ctx.channel().attr( USER_CONNECTION_ATTR ).get();
        userConnection.lastActivity = System.currentTimeMillis();
        eventBus.onMessage( cmd, userConnection );
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
        private long lastActivity;
        private final AtomicReference<Player> player = Atomics.newReference();

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
        public Future<Void> writeAsync(Object message) {
            return ch.write( message );
        }
        @Override
        public void writeSync(Object message) {
            writeAsync( message ).awaitUninterruptibly();
        }
        @Override
        public Future<Void> writeAllAsync(Object message) {
            return connections.write( message );
        }
        @Override
        public long socketAcceptTimestamp() {
            String[] items = ENCRYPTOR.decrypt( id ).split( ":" );
            return Long.parseLong( items[0] );
        }
        @Override
        public long socketLastActivityTimestamp() {
            return lastActivity;
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
        public void close() {
            ch.close().awaitUninterruptibly();
        }
    }
}

package com.katesoft.gserver.api;

import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import com.katesoft.gserver.transport.ConnectionType;

public interface UserConnection extends Closeable {
    String id();
    String clientPlatform();
    InetSocketAddress remoteAddress();
    Player player();
    Future<Void> writeAsync(Message message);
    void writeSync(Message message);
    ConnectionType connectionType();
    void addConnectionCloseHook(Runnable r);
    /**
     * close persistent user connection manually.</p>
     * 
     * <b>NOTE</b>: it's recommended to not close connection directly, but rather throw {@link DeadConnectionException}.
     * </p>
     */
    @Override
    void close();
    void asociatePlayer(Player p);
    void setClientPlatform(String platform);

    public static class UserConnectionStub implements UserConnection {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        protected final String id;
        protected ConnectionType connectionType = ConnectionType.TCP;
        protected Player player;
        protected String clientPlatform;
        protected final Set<Runnable> closeHooks = Sets.newCopyOnWriteArraySet();

        public UserConnectionStub() {
            this( UUID.randomUUID().toString() );
        }
        protected UserConnectionStub(String id) {
            this.id = id;
        }
        @Override
        public final String id() {
            return id;
        }
        @Override
        public InetSocketAddress remoteAddress() {
            return new InetSocketAddress( 1 );
        }
        @Override
        public final void asociatePlayer(Player p) {
            this.player = p;
        }
        @Override
        public String clientPlatform() {
            return clientPlatform;
        }
        @Override
        public void setClientPlatform(String platform) {
            this.clientPlatform = platform;
        }
        @Override
        public final Player player() {
            return player;
        }
        public void setConnectionType(ConnectionType connectionType) {
            this.connectionType = connectionType;
        }
        @Override
        public final ConnectionType connectionType() {
            return connectionType;
        }
        @Override
        public final void addConnectionCloseHook(Runnable r) {
            closeHooks.add( r );
        }
        @Override
        public Future<Void> writeAsync(Message message) {
            logger.trace( "writing reply={} async", message );
            Void v = null;
            return immediateCheckedFuture( v );
        }
        @Override
        public void writeSync(Message message) {
            logger.trace( "writing reply={} sync", message );
        }
        @Override
        public void close() {}
        @Override
        public String toString() {
            return toStringHelper( this ).add( "id", id() ).add( "remoteAddress", remoteAddress() ).toString();
        }
    }
}

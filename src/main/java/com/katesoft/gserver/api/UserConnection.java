package com.katesoft.gserver.api;

import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Atomics;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.transport.ConnectionType;

public interface UserConnection extends Closeable {
    String id();
    InetSocketAddress remoteAddress();
    Player asociatePlayer(Player p);
    Player associatedPlayer();
    Future<Void> writeAsync(BaseCommand message);
    void writeSync(BaseCommand message);
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

    public static class UserConnectionStub implements UserConnection {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        protected final String id;
        protected ConnectionType connectionType = ConnectionType.TCP;
        protected final AtomicReference<Player> player = Atomics.newReference();
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
        public final Player asociatePlayer(Player p) {
            Player prev = player.get();
            player.compareAndSet( prev, p );
            return prev;
        }
        @Override
        public final Player associatedPlayer() {
            return player.get();
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
        public Future<Void> writeAsync(BaseCommand message) {
            logger.trace( "writing reply={} async", message );
            Void v = null;
            return immediateCheckedFuture( v );
        }
        @Override
        public void writeSync(BaseCommand message) {
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

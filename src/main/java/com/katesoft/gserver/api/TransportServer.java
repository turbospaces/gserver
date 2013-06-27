package com.katesoft.gserver.api;

import static com.google.common.net.HostAndPort.fromParts;
import static com.katesoft.gserver.misc.Misc.nextAvailablePort;
import static com.katesoft.gserver.misc.Misc.shortHostname;

import java.io.Closeable;

import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;
import com.katesoft.gserver.transport.TransportMessageListener;

public interface TransportServer<ChannelType> extends Closeable {
    void startServer(TransportServerSettings settings, TransportMessageListener rootMessageListener) throws Exception;
    UserConnection getUserConnection(String id);
    int connectionsCount();
    UserConnection awaitForClientHandshake(ChannelType channel);

    public static final class TransportServerSettings {
        public HostAndPort tcp;
        public Optional<HostAndPort> websockets;

        public static TransportServerSettings avail() {
            HostAndPort tcp = fromParts( shortHostname(), nextAvailablePort() );
            HostAndPort webSocket = fromParts( shortHostname(), nextAvailablePort() );

            TransportServer.TransportServerSettings settings = new TransportServer.TransportServerSettings();
            settings.tcp = tcp;
            settings.websockets = Optional.of( webSocket );

            return settings;
        }
    }
}

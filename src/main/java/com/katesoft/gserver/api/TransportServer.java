package com.katesoft.gserver.api;

import java.io.Closeable;

import com.katesoft.gserver.domain.Entities.ServerSettings;
import com.katesoft.gserver.misc.Misc;
import com.katesoft.gserver.spi.PlatformContext;

public interface TransportServer<ChannelType> extends Closeable {
    void startServer(ServerSettings settings, PlatformContext platform) throws Exception;
    UserConnection getUserConnection(String id);
    int connectionsCount();
    UserConnection awaitForClientHandshake(ChannelType channel);

    public static class Util {
        public static ServerSettings avail() {
            return ServerSettings
                    .newBuilder()
                    .setTcpBindAddress( "localhost:" + Misc.nextAvailablePort() )
                    .setWebsocketsBindAddress( "localhost:" + Misc.nextAvailablePort() )
                    .build();
        }
    }
}

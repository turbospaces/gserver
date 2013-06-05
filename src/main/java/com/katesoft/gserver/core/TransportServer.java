package com.katesoft.gserver.core;

import java.io.Closeable;

import com.google.common.net.HostAndPort;

public interface TransportServer extends Closeable {
    void startServer(HostAndPort binding) throws Exception;
    UserConnection getUserConnection(String id);
    int connectionsCount();
}

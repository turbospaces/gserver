package com.katesoft.gserver.api;

import java.io.Closeable;

import com.google.common.net.HostAndPort;
import com.katesoft.gserver.transport.TransportMessageListener;

public interface TransportServer extends Closeable {
    void startServer(HostAndPort tcp, HostAndPort websockets, TransportMessageListener rootMessageListener) throws Exception;
    UserConnection getUserConnection(String id);
    int connectionsCount();
}

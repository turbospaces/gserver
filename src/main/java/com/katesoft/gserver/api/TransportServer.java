package com.katesoft.gserver.api;

import java.io.Closeable;

import com.google.common.net.HostAndPort;
import com.katesoft.gserver.transport.MessageListener;

public interface TransportServer extends Closeable {
    void startServer(HostAndPort binding, MessageListener rootMessageListener) throws Exception;
    UserConnection getUserConnection(String id);
    int connectionsCount();
}

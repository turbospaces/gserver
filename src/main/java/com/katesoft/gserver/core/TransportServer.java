package com.katesoft.gserver.core;

import java.io.Closeable;

import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;

public interface TransportServer extends Closeable {
    void startServer(HostAndPort binding) throws Exception;
    Optional<UserConnection> find(String id);
    int connectionsCount();
}

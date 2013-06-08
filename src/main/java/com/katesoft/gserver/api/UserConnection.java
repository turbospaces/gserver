package com.katesoft.gserver.api;

import io.netty.util.concurrent.Future;

import java.io.Closeable;

public interface UserConnection extends Closeable {
    String id();
    Player asociatePlayer(Player p);
    Player getAssociatedPlayer();
    long socketAcceptTimestamp();
    long socketLastActivityTimestamp();
    Future<Void> writeAsync(Object message);
    void writeSync(Object message);
    Future<Void> writeAllAsync(Object message);
}

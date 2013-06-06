package com.katesoft.gserver.api;

import io.netty.util.concurrent.Future;

import java.io.Closeable;

public interface UserConnection extends Closeable {
   String id();
    long socketAcceptTimestamp();
    long socketLastActivityTimestamp();
    Future<Void> writeAsync(Object message);
    void writeSync(Object message) throws InterruptedException;
    Future<Void> writeAllAsync(Object message);
}

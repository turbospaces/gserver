package com.katesoft.gserver.api;

import java.io.Closeable;

import com.google.common.collect.ImmutableSet;

public interface GameRoom extends Closeable {
    boolean join(Player player);
    void leave(Player player);
    ImmutableSet<Player> players();
    Game getGame();
    /**
     * close game room, un-associate all players from this room.
     */
    @Override
    void close();
}
package com.katesoft.gserver.api;

import java.io.Closeable;

public interface PlayerSession extends Closeable {
    String id();
    long inactivityTimeoutSeconds();
    UserConnection getAssociatedUserConnection();
    Game getAssociatedGame();
    Player getPlayer();
    /**
     * close player session, persist the necessary data if needed in order to continue
     * game play.
     */
    @Override
    void close();
}

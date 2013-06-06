package com.katesoft.gserver.api;

public interface PlayerSession {
    UserConnection getAssociatedUserConnection();
    Game getAssociatedGame();
}

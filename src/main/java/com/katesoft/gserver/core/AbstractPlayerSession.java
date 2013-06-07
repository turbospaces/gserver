package com.katesoft.gserver.core;

import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;

public abstract class AbstractPlayerSession implements PlayerSession {
    private final UserConnection userConnection;
    private final Game game;
    private final Player player;

    public AbstractPlayerSession(UserConnection userConnection, Game game, Player p) {
        this.userConnection = userConnection;
        this.game = game;
        this.player = p;
    }
    @Override
    public Player getPlayer() {
        return player;
    }
    @Override
    public UserConnection getAssociatedUserConnection() {
        return userConnection;
    }
    @Override
    public Game getAssociatedGame() {
        return game;
    }
    @Override
    public void close() {}
}

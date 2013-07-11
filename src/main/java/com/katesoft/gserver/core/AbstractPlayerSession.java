package com.katesoft.gserver.core;

import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coins;
import com.katesoft.gserver.domain.GameBO;

public abstract class AbstractPlayerSession implements PlayerSession {
    private final String sessionId;
    private final UserConnection userConnection;
    private final Game game;
    private final GameBO gameDefinition;
    private final Player player;
    private final BetLimits betLimits;
    private final Coins coins;

    public AbstractPlayerSession(String sessionId,
                                 UserConnection userConnection,
                                 Game game,
                                 GameBO gameDefinition,
                                 Player player,
                                 BetLimits betLimits,
                                 Coins coins) {
        this.userConnection = userConnection;
        this.game = game;
        this.gameDefinition = gameDefinition;
        this.player = player;
        this.betLimits = betLimits;
        this.coins = coins;
        this.sessionId = sessionId;
    }
    @Override
    public final String id() {
        return sessionId;
    }
    @Override
    public final Player getPlayer() {
        return player;
    }
    @Override
    public final UserConnection getAssociatedUserConnection() {
        return userConnection;
    }
    @Override
    public final Game getAssociatedGame() {
        return game;
    }
    @Override
    public final BetLimits getBetLimits() {
        return betLimits;
    }
    @Override
    public final GameBO getAssociatedGameDefinition() {
        return gameDefinition;
    }
    @Override
    public final Coins getCoins() {
        return coins;
    }
    @Override
    public void close() {
        getAssociatedGame().close();
    }
}

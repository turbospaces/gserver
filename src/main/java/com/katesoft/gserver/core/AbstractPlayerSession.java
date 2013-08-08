package com.katesoft.gserver.core;

import com.google.common.base.Objects;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coins;
import com.katesoft.gserver.domain.GameBO;

public abstract class AbstractPlayerSession implements PlayerSession {
    private final String sessionId;
    private final Game game;
    private final GameBO gameDefinition;
    private final Player player;
    private final BetLimits betLimits;
    private final Coins coins;
    private UserConnection userConnection;

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
    public final UserConnection getUserConnection() {
        return userConnection;
    }
    @Override
    public final Game getGame() {
        return game;
    }
    @Override
    public final BetLimits getBetLimits() {
        return betLimits;
    }
    @Override
    public final GameBO getGameMeta() {
        return gameDefinition;
    }
    @Override
    public final Coins getCoins() {
        return coins;
    }
    @Override
    public void close() {
        getGame().close();
    }
    public void setUserConnection(UserConnection userConnection) {
        this.userConnection = userConnection;
    }
    @Override
    public String toString() {
        return Objects
                .toStringHelper( this )
                .add( "sessionId", id() )
                .add( "player", getPlayer() )
                .add( "game", getGame() )
                .add( "betLimits", getBetLimits() )
                .add( "coins", getCoins() )
                .toString();
    }
}

package com.katesoft.gserver.core;

import com.google.common.collect.ImmutableSet;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coin;
import com.katesoft.gserver.domain.GameBO;

public abstract class AbstractPlayerSession implements PlayerSession {
    private final UserConnection userConnection;
    private final Game game;
    private final Player player;
    private final BetLimits betLimits;
    private final GameBO gameDefinition;
    private final ImmutableSet<Coin> coins;

    public AbstractPlayerSession(UserConnection userConnection,
                                 Game game,
                                 GameBO gameDefinition,
                                 Player p,
                                 BetLimits betLimits,
                                 ImmutableSet<Coin> coins) {
        this.userConnection = userConnection;
        this.game = game;
        this.gameDefinition = gameDefinition;
        this.player = p;
        this.betLimits = betLimits;
        this.coins = coins;
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
    public void close() {
        getAssociatedGame().close();
    }
    @Override
    public BetLimits getBetLimits() {
        return betLimits;
    }
    @Override
    public GameBO getAssociatedGameDefinition() {
        return gameDefinition;
    }
    @Override
    public ImmutableSet<Coin> getCoins() {
        return coins;
    }
}

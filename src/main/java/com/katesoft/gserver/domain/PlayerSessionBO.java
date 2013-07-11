package com.katesoft.gserver.domain;

import com.google.common.collect.ImmutableSet;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coin;

public class PlayerSessionBO implements BO {
    public final String sessionId;
    public final String userConnectionId;
    public final BetLimits betLimits;
    public final ImmutableSet<Coin> coins;
    public final GameBO gameDefinition;

    public PlayerSessionBO(PlayerSession session) {
        this.sessionId = session.id();
        this.gameDefinition = session.getAssociatedGameDefinition();
        this.betLimits = session.getBetLimits();
        this.userConnectionId = session.getAssociatedUserConnection().id();
        this.coins = session.getCoins();
    }

    @Override
    public String getPrimaryKey() {
        return sessionId;
    }
}

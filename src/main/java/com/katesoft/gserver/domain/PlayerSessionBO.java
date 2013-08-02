package com.katesoft.gserver.domain;

import org.springframework.security.crypto.codec.Hex;

import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coins;

public class PlayerSessionBO implements BO {
    public final String sessionId;
    public final String userId;
    public final String userConnectionId;
    public final BetLimits betLimits;
    public final Coins coins;
    public final GameBO game;
    public String clientPlatform;

    public PlayerSessionBO(PlayerSession session, String platform) {
        this.sessionId = session.id();
        this.userId = session.getPlayer().userId();
        this.game = session.getGameMeta();
        this.betLimits = session.getBetLimits();
        this.userConnectionId = session.getUserConnection().id();
        this.coins = session.getCoins();
        this.clientPlatform = platform;
    }
    public PlayerSessionBO(String sessionId, String userId, String connectionId, BetLimits betLimits, Coins coins, GameBO game, String platform) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.userConnectionId = connectionId;
        this.betLimits = betLimits;
        this.coins = coins;
        this.game = game;
        this.clientPlatform = platform;
    }
    @Override
    public String getPrimaryKey() {
        return sessionId;
    }
    public static String toSessionId(Player player, GameBO game) {
        return String.valueOf( Hex.encode( ( player.userId() + game.getPrimaryKey() ).getBytes() ) );
    }
}

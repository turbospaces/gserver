package com.katesoft.gserver.domain;

import org.springframework.security.crypto.encrypt.TextEncryptor;

import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.core.Encryptors;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coins;

public class PlayerSessionBO implements BO {
    public final String sessionId;
    public final String playerId;
    public final String userConnectionId;
    public final BetLimits betLimits;
    public final Coins coins;
    public final GameBO game;

    public PlayerSessionBO(PlayerSession session) {
        this.sessionId = session.id();
        this.playerId = session.getPlayer().getPrimaryKey();
        this.game = session.getAssociatedGameDefinition();
        this.betLimits = session.getBetLimits();
        this.userConnectionId = session.getAssociatedUserConnection().id();
        this.coins = session.getCoins();
    }
    public PlayerSessionBO(String sessionId, String playerId, String connectionId, BetLimits betLimits, Coins coins, GameBO game) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.userConnectionId = connectionId;
        this.betLimits = betLimits;
        this.coins = coins;
        this.game = game;
    }
    @Override
    public String getPrimaryKey() {
        return sessionId + ":" + game.getPrimaryKey();
    }
    public static String toSessionId(Player player, GameBO game, TextEncryptor encryptor) {
        return Encryptors.encode( encryptor, player.getPrimaryKey(), game.getPrimaryKey() );
    }
}

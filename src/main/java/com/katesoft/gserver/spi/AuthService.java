package com.katesoft.gserver.spi;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.core.AbstractPlayer;

public interface AuthService {
    Player login(String playerId, String credentials);
    void logout(Player player);

    public static class MockAuthService implements AuthService {
        private final ConcurrentMap<String, Player> players = Maps.newConcurrentMap();

        @Override
        public Player login(String playerId, String credentials) {
            Player player = players.get( playerId );
            if ( player == null ) {
                player = new AbstractPlayer( playerId, playerId + "@my.com" ) {};
                Player prev = players.putIfAbsent( playerId, player );
                if ( prev != null ) {
                    player = prev;
                }
            }
            return player;
        }
        @Override
        public void logout(Player player) {}
    }
}

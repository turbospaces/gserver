package com.katesoft.gserver.spi;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.core.AbstractPlayerSession;

public interface GamesAdminService {
    ImmutableSet<Class<? extends Game>> supportedGames();
    void unloadGame(Class<? extends Game> game);
    PlayerSession openOrRestorePlayerSession(String gameId, Player player, UserConnection uc);

    public static class MockGameControlService implements GamesAdminService {
        private final List<Class<? extends Game>> games;

        public MockGameControlService(Class<? extends Game>... games) {
            this.games = Lists.newCopyOnWriteArrayList( Arrays.asList( games ) );
        }
        @Override
        public ImmutableSet<Class<? extends Game>> supportedGames() {
            return ImmutableSet.copyOf( games );
        }
        @Override
        public void unloadGame(Class<? extends Game> game) {
            games.remove( game );
        }
        @Override
        public PlayerSession openOrRestorePlayerSession(String gameId, Player player, UserConnection uc) {
            return new AbstractPlayerSession( uc, null, player ) {};
        }
    }
}

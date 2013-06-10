package com.katesoft.gserver.spi;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.core.AbstractPlayer;
import com.katesoft.gserver.core.AbstractPlayerSession;
import com.katesoft.gserver.core.CommandsQualifierCodec;

public interface PlatformInterface {
    Player login(String playerId, String credentials);
    void logout(Player player, String sessionId);
    PlayerSession openPlayerSession(String gameId, Player player, UserConnection uc);
    CommandsQualifierCodec commandsCodec();
    GamePlayContext gamePlayContext();

    //
    //
    //
    public static class MockPlatformInterface implements PlatformInterface {
        private final ConcurrentMap<String, Player> players = Maps.newConcurrentMap();
        private final ConcurrentMap<String, Class<? extends Game>> games = Maps.newConcurrentMap();
        private final GamePlayContext ctx;
        private final CommandsQualifierCodec codec;

        public MockPlatformInterface(GamePlayContext ctx, CommandsQualifierCodec codec, Class<? extends Game>... games) {
            this.ctx = ctx;
            this.codec = codec;
            for ( Class<? extends Game> cl : games ) {
                this.games.put( cl.getSimpleName(), cl );
            }
        }
        @Override
        public GamePlayContext gamePlayContext() {
            return ctx;
        }
        @Override
        public CommandsQualifierCodec commandsCodec() {
            return codec;
        }
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
        public void logout(Player player, String sessionId) {
            player.closePlayerSession( sessionId );
        }
        @Override
        public PlayerSession openPlayerSession(final String gameId, final Player player, final UserConnection uc) {
            return new AbstractPlayerSession( uc, newGameInstance( gameId ), player ) {
                @Override
                public String id() {
                    return uc.id();
                }
            };
        }
        protected Game newGameInstance(final String gameId) {
            Class<? extends Game> c = checkNotNull( games.get( gameId ), "Game=%s not supported", gameId );
            for ( ;; ) {
                try {
                    Game game = c.newInstance();
                    game.init( gamePlayContext() );
                    return game;
                }
                catch ( Exception e ) {
                    Throwables.propagate( e );
                }
            }
        }
    }
}

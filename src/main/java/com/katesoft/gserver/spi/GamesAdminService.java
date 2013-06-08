package com.katesoft.gserver.spi;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Iterables.removeIf;
import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.core.AbstractPlayerSession;

public interface GamesAdminService {
    Collection<Class<? extends Game>> supportedGames();
    void unloadGame(Class<? extends Game> game);
    PlayerSession openOrRestorePlayerSession(String gameId,
                                             Player player,
                                             UserConnection uc);

    public static abstract class AbstractGameControlService implements GamesAdminService {
        protected Logger logger = LoggerFactory.getLogger(GamesAdminService.class);

        protected final List<Game> games;
        protected final GamePlayContext ctx;

        public <T extends Game> AbstractGameControlService(final GamePlayContext ctx, Class<T>... games) {
            this.ctx = ctx;
            this.games = newCopyOnWriteArrayList(transform(asList(games), new Function<Class<T>, Game>() {
                @Override
                public Game apply(Class<T> input) {
                    for ( ;; ) {
                        try {
                            T game = input.newInstance();
                            game.init(ctx);
                            logger.debug("registered {} with id=({})", game.getClass().getSimpleName(), game.id());
                            return game;
                        }
                        catch ( Exception e ) {
                            Throwables.propagate(e);
                        }
                    }
                }
            }));
        }
        @Override
        public Collection<Class<? extends Game>> supportedGames() {
            return transform(games, new Function<Game, Class<? extends Game>>() {
                @Override
                public Class<? extends Game> apply(Game input) {
                    return input.getClass();
                }
            });
        }
        @Override
        public void unloadGame(final Class<? extends Game> cl) {
            removeIf(games, new Predicate<Game>() {
                @Override
                public boolean apply(Game input) {
                    return input.getClass().equals(cl);
                }
            });
        }
        @Override
        public PlayerSession openOrRestorePlayerSession(final String gameId,
                                                        final Player player,
                                                        final UserConnection uc) {
            return new AbstractPlayerSession(uc, findGameById(gameId), player) {
                @Override
                public String id() {
                    return uc.id();
                }
                @Override
                public long inactivityTimeoutSeconds() {
                    return -1;
                }
            };
        }
        protected Game findGameById(final String gameId) {
            return find(games, new Predicate<Game>() {
                @Override
                public boolean apply(Game input) {
                    return input.id().equals(gameId);
                }
            });
        }
    }
}

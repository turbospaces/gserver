package com.katesoft.gserver.domain;

import static java.lang.Thread.currentThread;

import javax.annotation.concurrent.Immutable;

import org.springframework.util.ClassUtils;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GamePlayContext;

@Immutable
public class GameBO implements BO {
    private final String shortcut;
    private final String displayName;
    private final String gameClassName;

    public GameBO(String shortcut, String displayName, String gameClassName) {
        this.shortcut = shortcut;
        this.displayName = displayName;
        this.gameClassName = gameClassName;
    }
    @Override
    public String getPrimaryKey() {
        return shortcut;
    }
    public String getDisplayName() {
        return displayName;
    }
    public String getGameClassName() {
        return gameClassName;
    }
    @SuppressWarnings("unchecked")
    public Game newInstance(final GamePlayContext ctx) {
        for ( ;; ) {
            try {
                Class<? extends Game> c = (Class<? extends Game>) ClassUtils.forName( getGameClassName(), currentThread().getContextClassLoader() );
                Game game = c.newInstance();
                game.init( ctx );
                return game;
            }
            catch ( Exception e ) {
                Throwables.propagate( e );
            }
        }
    }
    @Override
    public int hashCode() {
        return Objects.hashCode( getPrimaryKey() );
    }
    @Override
    public boolean equals(Object obj) {
        return Objects.equal( getPrimaryKey(), ( (GameBO) obj ).getPrimaryKey() );
    }
    @Override
    public String toString() {
        return Objects
                .toStringHelper( this )
                .add( "shortcut", getPrimaryKey() )
                .add( "displayName", getDisplayName() )
                .add( "gameClassName", getGameClassName() )
                .toString();
    }
}

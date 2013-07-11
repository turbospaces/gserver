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
    private String shortcut;
    private String displayName;
    private String gameClassName;

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
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setShortcut(String shortcut) {
        this.shortcut = shortcut;
    }
    public String getGameClassName() {
        return gameClassName;
    }
    public void setGameClassName(String gameClassName) {
        this.gameClassName = gameClassName;
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
    public String toString() {
        return Objects
                .toStringHelper( this )
                .add( "shortcut", getPrimaryKey() )
                .add( "displayName", getDisplayName() )
                .add( "gameClassName", getGameClassName() )
                .toString();
    }
}

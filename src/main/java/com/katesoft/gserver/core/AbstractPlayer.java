package com.katesoft.gserver.core;

import static java.lang.String.format;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.katesoft.gserver.api.CommandWrapperEvent;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;

public abstract class AbstractPlayer implements Player {
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    private final Set<PlayerSession> sessions = Sets.newHashSet();
    protected String id, displayName, email;

    public AbstractPlayer(String id, String email) {
        this.id = id;
        this.email = email;
    }
    protected AbstractPlayer() {}

    @Override
    public String id() {
        return id;
    }
    @Override
    public String displayName() {
        return displayName;
    }
    @Override
    public String email() {
        return email;
    }
    @Override
    public boolean addPlayerSession(PlayerSession s) {
        return sessions.add( Preconditions.checkNotNull( s ) );
    }
    @Override
    public void close() {
        for ( PlayerSession s : sessions ) {
            try {
                s.close();
            }
            catch ( Throwable t ) {
                logger.error( t.getMessage(), t );
            }
        }
    }
    @Override
    public boolean dispatchCommand(CommandWrapperEvent cmd) {
        for ( PlayerSession s : sessions ) {
            Game g = s.getAssociatedGame();
            try {
                g.getGameCommandInterpreter().interpretCommand( cmd );
            }
            catch ( Throwable t ) {
                logger.error( format( "Unable to interpet game specific command %s:%s", g.id(), cmd.cmdClass().getSimpleName() ), t );
            }
        }
        return cmd.isInterpreted();
    }
}

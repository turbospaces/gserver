package com.katesoft.gserver.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.find;
import static java.lang.String.format;

import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.chain.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GameCommand;
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
    public synchronized boolean addPlayerSession(PlayerSession s) {
        return sessions.add( Preconditions.checkNotNull( s ) );
    }
    @Override
    public synchronized void closePlayerSession(final String sessionId) {
        PlayerSession session = find( sessions, new Predicate<PlayerSession>() {
            @Override
            public boolean apply(@Nullable PlayerSession input) {
                return input.id().equals( sessionId );
            }
        } );
        session.close();
        sessions.remove( session );
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
    public boolean execute(Context context) {
        final NetworkCommandContext ctx = (NetworkCommandContext) context;
        final String sessionId = checkNotNull( ctx.getCmd().getSessionId(), "got command=%s detached from session", ctx.getCmd() );
        final PlayerSession session = find( sessions, new Predicate<PlayerSession>() {
            @Override
            public boolean apply(PlayerSession input) {
                return input.id().equals( sessionId );
            }
        } );
        final Game g = session.getAssociatedGame();

        for ( ;; ) {
            try {
                GameCommand e = new GameCommand( ctx, session );

                g.commandInterpreter().interpretCommand( e );
                return e.isAcknowledged();
            }
            catch ( Throwable t ) {
                logger.error(
                        format( "Unable to interpet game specific command %s:%s", g.getClass().getSimpleName(), ctx
                                .getCmd()
                                .getClass()
                                .getSimpleName() ),
                        t );
                Throwables.propagate( t );
            }
        }
    }
}

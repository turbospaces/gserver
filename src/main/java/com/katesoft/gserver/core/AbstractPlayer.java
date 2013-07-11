package com.katesoft.gserver.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.find;
import static java.lang.String.format;

import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.chain.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GameCommand;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.domain.UserAccountBO;

public abstract class AbstractPlayer implements Player {
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    private final Map<String, PlayerSession> sessions = Maps.newHashMap();
    protected String id, displayName;

    public AbstractPlayer(UserAccountBO userAccount) {
        this.id = userAccount.getPrimaryKey();
        this.displayName = userAccount.toFullName();
    }
    @Override
    public String getPrimaryKey() {
        return id;
    }
    @Override
    public String displayName() {
        return displayName;
    }
    @Override
    public synchronized boolean addPlayerSession(PlayerSession playerSession) {
        return sessions.put( playerSession.id(), playerSession ) == null;
    }
    @Override
    public synchronized void closePlayerSession(final String sessionId) {
        PlayerSession session = find( sessions.values(), new Predicate<PlayerSession>() {
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
        for ( PlayerSession s : sessions.values() ) {
            try {
                s.close();
            }
            catch ( Throwable t ) {
                logger.error( t.getMessage(), t );
            }
        }
    }
    @Override
    public boolean execute(final Context context) {
        final NetworkCommandContext ctx = (NetworkCommandContext) context;
        final String sessionId = checkNotNull( ctx.getCmd().getSessionId(), "got command=%s detached from session", ctx.getCmd() );
        final PlayerSession session = find( sessions.values(), new Predicate<PlayerSession>() {
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

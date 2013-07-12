package com.katesoft.gserver.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.find;
import static java.lang.String.format;

import java.util.Map;

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
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coins;
import com.katesoft.gserver.domain.GameBO;
import com.katesoft.gserver.domain.UserAccountBO;

public abstract class AbstractPlayer implements Player {
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected final Map<String, PlayerSession> sessions = Maps.newHashMap();
    protected final String userId, displayName;

    public AbstractPlayer(UserAccountBO userAccount) {
        this.userId = userAccount.getPrimaryKey();
        this.displayName = userAccount.toFullName();
    }
    @Override
    public final String userId() {
        return userId;
    }
    @Override
    public String displayName() {
        return displayName;
    }
    // @formatter:off
    @Override
    public PlayerSession openPlayerSession(String sessionId,
                                           UserConnection uc,
                                           Game game,
                                           GameBO gameBO,
                                           BetLimits blimits,
                                           Coins coins) {
        PlayerSession session = new AbstractPlayerSession( sessionId, uc, game, gameBO, this, blimits, coins ) {};
        sessions.put( session.id(), session );
        return session;
    }
    // @formatter:on
    @Override
    public void closePlayerSession(final String sessionId) {
        PlayerSession session = sessions.remove( sessionId );
        if ( session != null ) {
            session.close();
        }
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
        sessions.clear();
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
        final Game g = session.getGame();

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

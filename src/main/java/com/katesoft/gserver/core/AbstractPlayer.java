package com.katesoft.gserver.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.tryFind;
import static java.lang.String.format;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.commons.chain.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.katesoft.gserver.api.AbstractProtocolException;
import com.katesoft.gserver.api.AbstractProtocolException.InvalidSessionUsageException;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GameCommand;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BalanceUpdateNotify;
import com.katesoft.gserver.commands.Commands.ShowMessageNotify;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coins;
import com.katesoft.gserver.domain.Entities.NotificationType;
import com.katesoft.gserver.domain.GameBO;
import com.katesoft.gserver.domain.UserAccountBO;

public abstract class AbstractPlayer implements Player {
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected final Map<String, PlayerSession> sessions = Maps.newHashMap();
    protected final String userId, displayName;
    protected BigDecimal balance;

    public AbstractPlayer(UserAccountBO userAccount) {
        this.userId = userAccount.getPrimaryKey();
        this.displayName = userAccount.toFullName();
        this.balance = userAccount.getBalance();
    }
    @Override
    public final String userId() {
        return userId;
    }
    @Override
    public String displayName() {
        return displayName;
    }
    @Override
    public BigDecimal balance() {
        return balance;
    }
    @Override
    public void updateBalance(BigDecimal amount) {
        this.balance = amount;
        BalanceUpdateNotify notify = BalanceUpdateNotify.newBuilder().setBalance( amount.doubleValue() ).build();
        for ( PlayerSession ps : sessions.values() ) {
            ps.getUserConnection().writeAsync( notify );
        }
    }
    @Override
    public void showUserMessage(String msg, NotificationType type) {
        ShowMessageNotify notify = ShowMessageNotify.newBuilder().setMsg( msg ).setType( type ).build();
        for ( PlayerSession ps : sessions.values() ) {
            ps.getUserConnection().writeAsync( notify );
        }
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
    public boolean execute(final Context context) throws InvalidSessionUsageException {
        final NetworkCommandContext ctx = (NetworkCommandContext) context;
        final String sessionId = checkNotNull( ctx.getCmd().getSessionId(), "got command=%s detached from session", ctx.getCmd() );
        final Optional<PlayerSession> opt = tryFind( sessions.values(), new Predicate<PlayerSession>() {
            @Override
            public boolean apply(PlayerSession input) {
                return input.id().equals( sessionId );
            }
        } );
        if ( !opt.isPresent() ) {
            throw new InvalidSessionUsageException( sessionId );
        }

        PlayerSession session = opt.get();
        Game g = session.getGame();
        boolean ack = false;

        try {
            GameCommand e = new GameCommand( ctx, session );
            g.commandInterpreter().interpretCommand( e );
            ack = e.isAcknowledged();
            if ( !ack ) {
                throw new AbstractProtocolException.UnknownCommadException( g.getClass().getSimpleName() );
            }
        }
        catch ( Throwable t ) {
            logger
                    .error(
                            format( "Unable to interpet game specific command %s:%s", g.getClass().getSimpleName(), ctx
                                    .getCmd()
                                    .getClass()
                                    .getSimpleName() ),
                            t );
            Throwables.propagate( t );
        }
        return ack;
    }
}

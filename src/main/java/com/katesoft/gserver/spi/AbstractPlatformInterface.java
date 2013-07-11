package com.katesoft.gserver.spi;

import static com.google.common.base.Preconditions.checkState;
import static com.katesoft.gserver.core.Commands.toReply;

import java.util.concurrent.ConcurrentMap;

import org.apache.commons.chain.Chain;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ChainBase;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.api.DeadConnectionException;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.CloseGamePlayAndLogoutCommand;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.LoginCommnadException;
import com.katesoft.gserver.commands.Commands.LoginCommnadException.Builder;
import com.katesoft.gserver.commands.Commands.OpenGamePlayCommand;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.commands.Commands.UpdatePlayerSettingsCommand;
import com.katesoft.gserver.core.AbstractPlayer;
import com.katesoft.gserver.core.AbstractPlayerSession;
import com.katesoft.gserver.core.CommandsQualifierCodec;
import com.katesoft.gserver.core.NetworkCommandContext;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coin;
import com.katesoft.gserver.domain.GameBO;
import com.katesoft.gserver.domain.RedisDomainRepository;
import com.katesoft.gserver.domain.UserAccountBO;
import com.katesoft.gserver.domain.support.RedisPersistentTokenBasedRememberMeServices;
import com.katesoft.gserver.domain.support.RedisPersistentTokenRepository;
import com.katesoft.gserver.domain.support.RedisUserDetailsService;

public abstract class AbstractPlatformInterface implements PlatformInterface {
    protected final Logger logger = LoggerFactory.getLogger( getClass() );
    private final GamePlayContext ctx;
    private final CommandsQualifierCodec codec;
    private final Chain chain;

    private final ConcurrentMap<String, AbstractPlayer> players = Maps.newConcurrentMap();

    private final RedisUserDetailsService userDetailsService;
    private final RedisDomainRepository repository;
    private final RedisPersistentTokenBasedRememberMeServices rememberMeServices;

    public AbstractPlatformInterface(GamePlayContext ctx,
                                     CommandsQualifierCodec codec,
                                     RedisDomainRepository repository,
                                     RedisPersistentTokenBasedRememberMeServices rememberMeServices) {
        this.ctx = ctx;
        this.codec = codec;
        this.repository = repository;
        this.rememberMeServices = rememberMeServices;
        this.userDetailsService = new RedisUserDetailsService( repository );
        this.chain = initChain();
    }
    @Override
    public Chain platformCommandsInterpreter() {
        return chain;
    }
    @Override
    public GamePlayContext gamePlayContext() {
        return ctx;
    }
    @Override
    public CommandsQualifierCodec commandsCodec() {
        return codec;
    }
    protected ChainBase initChain() {
        return new ChainBase( new Command() {
            @Override
            public boolean execute(Context context) throws Exception {
                NetworkCommandContext ncc = (NetworkCommandContext) context;
                BaseCommand cmd = ncc.getCmd();
                UserConnection uc = ncc.getUserConnection();
                Class<? extends GeneratedMessage> type = codec.decoder().apply( cmd );

                /**
                 * by default handles those actions:
                 * 1. login()
                 * 2. openGamePlay()
                 */
                if ( LoginCommand.class == type ) {
                    LoginCommand login = cmd.getExtension( LoginCommand.cmd );
                    try {
                        Player player = login( login.getToken() );
                        uc.asociatePlayer( player );
                    }
                    catch ( AuthenticationException e ) {
                        Builder excb = LoginCommnadException.newBuilder().setMsg( e.getMessage() );
                        if ( cmd.getDebug() ) {
                            excb.setStacktrace( ExceptionUtils.getStackTrace( e ) );
                        }
                        uc.writeSync( toReply( cmd, codec, LoginCommnadException.cmd, excb.build() ) );
                        throw new DeadConnectionException( uc, e.getMessage() );
                    }
                    return PROCESSING_COMPLETE;
                }
                else if ( OpenGamePlayCommand.class == type ) {
                    OpenGamePlayCommand openGamePlay = cmd.getExtension( OpenGamePlayCommand.cmd );
                    Player player = uc.getAssociatedPlayer();
                    Pair<PlayerSession, GameBO> playerSession = openPlayerSession( openGamePlay.getGameId(), player, uc );
                    player.addPlayerSession( playerSession.getLeft() );
                    GameBO game = playerSession.getRight();

                    OpenGamePlayReply reply = OpenGamePlayReply
                            .newBuilder()
                            .setSessionId( playerSession.getLeft().id() )
                            .setDisplayName( game.getDisplayName() )
                            .setBetLimits( playerSession.getLeft().getBetLimits() )
                            .addAllCoins( playerSession.getLeft().getCoins() )
                            .build();
                    uc.writeAsync( toReply( cmd, commandsCodec(), OpenGamePlayReply.cmd, reply ) );
                    return PROCESSING_COMPLETE;
                }
                else if ( CloseGamePlayAndLogoutCommand.class == type ) {
                    Player player = uc.getAssociatedPlayer();
                    if ( player != null ) {
                        logout( player, cmd.getSessionId() );
                        uc.close();
                    }
                    return PROCESSING_COMPLETE;
                }
                else if ( UpdatePlayerSettingsCommand.class == type ) {
                    UpdatePlayerSettingsCommand updatePlayerSettings = cmd.getExtension( UpdatePlayerSettingsCommand.cmd );
                    Player player = uc.getAssociatedPlayer();
                }

                return CONTINUE_PROCESSING;
            }
        } );
    }
    /**
     * by default treat token as persistent remember me token value which is generated by spring-security and stored via
     * {@link RedisPersistentTokenRepository}.
     * 
     * @param token - cookie value
     * @return player.
     */
    protected Player login(String token) {
        String[] parts = rememberMeServices.decodeCookie( token );

        String series = parts[0];
        PersistentRememberMeToken rememberMeToken = rememberMeServices.getTokenRepository().getTokenForSeries( series );
        checkState( rememberMeToken.getTokenValue().equals( parts[1] ) );
        String username = rememberMeToken.getUsername();

        UserAccountBO userAccount = (UserAccountBO) userDetailsService.loadUserByUsername( username );
        AbstractPlayer player = players.get( username );
        if ( player == null ) {
            player = new AbstractPlayer( username ) {};
            player.displayName( userAccount.toFullName() );
            AbstractPlayer prev = players.putIfAbsent( username, player );
            if ( prev != null ) {
                player = prev;
            }
        }
        return player;
    }
    protected Pair<PlayerSession, GameBO> openPlayerSession(String game, Player pl, final UserConnection uc) throws EmptyResultDataAccessException {
        Optional<GameBO> opt = repository.findGame( game );
        if ( opt.isPresent() ) {
            GameBO gameBO = opt.get();
            BetLimits blimits = BetLimits.newBuilder().setMaxBet( Short.MAX_VALUE ).setMinBet( 1 ).build();
            ImmutableSet<Coin> coins = ImmutableSet.copyOf( Coin.values() );
            return ImmutablePair.of( (PlayerSession) new AbstractPlayerSession( uc, gameBO.newInstance( ctx ), gameBO, pl, blimits, coins ) {
                @Override
                public String id() {
                    return uc.id();
                }
            }, gameBO );
        }
        throw new EmptyResultDataAccessException( "unable to find Game definition by shortcut=" + game, 1 );
    }
    protected void logout(Player player, String sessionId) {
        player.closePlayerSession( sessionId );
    }
}

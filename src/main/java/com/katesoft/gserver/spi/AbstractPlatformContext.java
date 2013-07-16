package com.katesoft.gserver.spi;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.katesoft.gserver.core.Commands.toReply;
import static com.katesoft.gserver.domain.RedisDomainRepository.required;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.chain.Chain;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ChainBase;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.util.StringUtils;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.api.DeadConnectionException;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.CloseGamePlayAndLogoutCommand;
import com.katesoft.gserver.commands.Commands.CloseGamePlayAndLogoutReply;
import com.katesoft.gserver.commands.Commands.Geti18nMessagesCommand;
import com.katesoft.gserver.commands.Commands.Geti18nMessagesReply;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.LoginCommnadException;
import com.katesoft.gserver.commands.Commands.LoginCommnadException.Builder;
import com.katesoft.gserver.commands.Commands.OpenGamePlayCommand;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.commands.Commands.ReloginCommand;
import com.katesoft.gserver.commands.Commands.UpdatePlayerSettingsCommand;
import com.katesoft.gserver.core.AbstractPlayer;
import com.katesoft.gserver.core.CommandsQualifierCodec;
import com.katesoft.gserver.core.NetworkCommandContext;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coin;
import com.katesoft.gserver.domain.Entities.Coins;
import com.katesoft.gserver.domain.Entities.i18n;
import com.katesoft.gserver.domain.GameBO;
import com.katesoft.gserver.domain.PlayerSessionBO;
import com.katesoft.gserver.domain.RedisDomainRepository;
import com.katesoft.gserver.domain.UserAccountBO;
import com.katesoft.gserver.domain.support.RedisPersistentTokenBasedRememberMeServices;
import com.katesoft.gserver.domain.support.RedisPersistentTokenRepository;
import com.katesoft.gserver.domain.support.RedisUserDetailsService;

public abstract class AbstractPlatformContext implements PlatformContext {
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    private final GamePlayContext ctx;
    private final CommandsQualifierCodec codec;
    private final Chain chain;

    private final ConcurrentMap<String, AbstractPlayer> players = Maps.newConcurrentMap();

    private final RedisUserDetailsService userDetailsService;
    private final RedisDomainRepository repository;
    private final RedisPersistentTokenBasedRememberMeServices rememberMeServices;

    public AbstractPlatformContext(GamePlayContext ctx,
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
                boolean processed = CONTINUE_PROCESSING;

                /**
                 * by default handles those actions:
                 * 1. login()
                 * 2. openGamePlay()
                 */
                if ( LoginCommand.class == type ) {
                    LoginCommand login = cmd.getExtension( LoginCommand.cmd );
                    Player player = login( login.getToken() );
                    tryLogin( cmd, player, uc );
                    processed = PROCESSING_COMPLETE;
                }
                else if ( ReloginCommand.class == type ) {
                    checkNotNull( cmd.getExtension( ReloginCommand.cmd ) );
                    String sessionId = checkNotNull( cmd.getSessionId() );
                    Player player = relogin( sessionId );
                    tryLogin( cmd, player, uc );
                    processed = PROCESSING_COMPLETE;
                }
                else if ( OpenGamePlayCommand.class == type ) {
                    OpenGamePlayCommand openGamePlay = cmd.getExtension( OpenGamePlayCommand.cmd );
                    Player player = uc.associatedPlayer();
                    OpenGamePlayReply.Builder b = OpenGamePlayReply.newBuilder();
                    PlayerSessionBO playerSession = openPlayerSession( openGamePlay.getGameId(), player, uc, b );

                    OpenGamePlayReply reply = b
                            .setSessionId( playerSession.sessionId )
                            .setBetLimits( playerSession.betLimits )
                            .setCoins( playerSession.coins )
                            .build();

                    uc.writeAsync( toReply( cmd, commandsCodec(), OpenGamePlayReply.cmd, reply ) );
                    processed = PROCESSING_COMPLETE;
                }
                else if ( CloseGamePlayAndLogoutCommand.class == type ) {
                    CloseGamePlayAndLogoutCommand closeCommand = cmd.getExtension( CloseGamePlayAndLogoutCommand.cmd );
                    Player player = uc.associatedPlayer();
                    if ( player != null ) {
                        logout( player, cmd.getSessionId() );
                        if ( closeCommand.getForceCloseConnection() ) {
                            throw new DeadConnectionException( uc, "closed as per user request" );
                        }
                        else {
                            CloseGamePlayAndLogoutReply reply = CloseGamePlayAndLogoutReply.newBuilder().build();
                            uc.writeAsync( toReply( cmd, commandsCodec(), CloseGamePlayAndLogoutReply.cmd, reply ) );
                        }
                    }
                    processed = PROCESSING_COMPLETE;
                }
                else if ( Geti18nMessagesCommand.class == type ) {
                    Geti18nMessagesCommand i18nCommand = cmd.getExtension( Geti18nMessagesCommand.cmd );
                    List<String> keysList = i18nCommand.getKeysList();
                    String locale = i18nCommand.getLocale();
                    ImmutableSet<i18n> messages = geti18nMessages( keysList, locale );

                    Geti18nMessagesReply reply = Geti18nMessagesReply.newBuilder().addAllValues( messages ).build();
                    uc.writeAsync( toReply( cmd, commandsCodec(), Geti18nMessagesReply.cmd, reply ) );
                    processed = PROCESSING_COMPLETE;
                }
                else if ( UpdatePlayerSettingsCommand.class == type ) {
                    cmd.getExtension( UpdatePlayerSettingsCommand.cmd );
                    uc.associatedPlayer();
                }
                return processed;
            }
        } );
    }
    private void tryLogin(BaseCommand cmd, Player player, UserConnection uc) {
        try {
            uc.asociatePlayer( player );
        }
        catch ( Exception e ) {
            Builder excb = LoginCommnadException.newBuilder().setMsg( e.getMessage() );
            if ( cmd.getDebug() ) {
                excb.setStacktrace( ExceptionUtils.getStackTrace( e ) );
            }
            uc.writeSync( toReply( cmd, codec, LoginCommnadException.cmd, excb.build() ) );
            throw new DeadConnectionException( uc, e.getMessage() );
        }
    }
    /**
     * by default treat token as persistent remember me token value which is generated by spring-security and stored via
     * {@link RedisPersistentTokenRepository}.
     * 
     * @param token - cookie value
     * @return player.
     */
    protected Player login(String token) {
        String[] parts = rememberMeServices.decodeWebsocketLoginToken( token );
        String series = parts[0];
        String tokenValue = parts[1];

        PersistentRememberMeToken rememberMeToken = rememberMeServices.getTokenRepository().getTokenForSeries( series );
        checkState( rememberMeToken.getTokenValue().equals( tokenValue ) );
        String username = rememberMeToken.getUsername();
        UserAccountBO userAccount = (UserAccountBO) userDetailsService.loadUserByUsername( username );

        return initPlayer( userAccount );
    }
    /**
     * attempt to re-login (re-attach) user to existing session if possible.
     * 
     * @param sessionId - player session id.
     * @return player.
     */
    protected Player relogin(String sessionId) {
        Optional<PlayerSessionBO> ongoing = repository.findPlayerSession( sessionId );
        PlayerSessionBO playerSession = required( ongoing, PlayerSessionBO.class, sessionId );
        UserAccountBO userAccount = (UserAccountBO) userDetailsService.loadUserByUsername( playerSession.userId );
        return initPlayer( userAccount );
    }
    protected Player initPlayer(UserAccountBO userAccount) {
        AbstractPlayer player = players.get( userAccount.getPrimaryKey() );
        if ( player == null ) {
            player = new AbstractPlayer( userAccount ) {};
            AbstractPlayer prev = players.putIfAbsent( userAccount.getPrimaryKey(), player );
            if ( prev != null ) {
                player = prev;
            }
        }
        return player;
    }
    /**
     * open new game player session and store relevant information to persistent data store.
     * 
     * @param gameCode - game shortcut.
     * @param player - player instance.
     * @param uc - user connection.
     * @param b - response builder.
     * 
     * @return player session data.
     */
    protected PlayerSessionBO openPlayerSession(String gameCode, Player player, UserConnection uc, OpenGamePlayReply.Builder b) {
        GameBO gameBO = required( repository.findGame( gameCode ), GameBO.class, gameCode );
        String sessionId = PlayerSessionBO.toSessionId( player, gameBO );
        Optional<PlayerSessionBO> ongoing = repository.findPlayerSession( sessionId );
        Game game = gameBO.newInstance( ctx );

        if ( ongoing.isPresent() ) {
            logger.info( "Attaching game play to existing Player Session = {}", ongoing.get().sessionId );
            b.setReattached( true );
            return ongoing.get();
        }
        else {
            BetLimits blimits = BetLimits.newBuilder().setMaxBet( Short.MAX_VALUE ).setMinBet( 1 ).build();
            Coins coins = Coins.newBuilder().addAllCoins( ImmutableSet.copyOf( Coin.values() ) ).build();

            PlayerSession playerSession = player.openPlayerSession( sessionId, uc, game, gameBO, blimits, coins );
            logger.info( "PlayerSesion has been created = {}", playerSession );
            PlayerSessionBO playerSessionBO = new PlayerSessionBO( playerSession );
            repository.savePlayerSession( playerSessionBO );
            b.setReattached( false );

            return playerSessionBO;
        }
    }
    protected void logout(Player player, String sessionId) {
        Optional<PlayerSessionBO> ongoing = repository.findPlayerSession( sessionId );
        if ( ongoing.isPresent() ) {
            PlayerSessionBO playerSessionBO = ongoing.get();
            repository.deletePlayerSession( playerSessionBO );
            player.closePlayerSession( sessionId );
            logger.info( "PlayerSesion={} has been closed", playerSessionBO.getPrimaryKey() );
        }
    }
    protected ImmutableSet<i18n> geti18nMessages(List<String> keysList, String locale) {
        MessageSource messageSource = ctx.messageSource();
        Locale l = StringUtils.parseLocaleString( locale );
        ImmutableSet.Builder<i18n> b = ImmutableSet.builder();
        for ( String key : keysList ) {
            b.add( i18n.newBuilder().setKey( key ).setValue( messageSource.getMessage( key, null, l ) ).build() );
        }
        return b.build();
    }
}

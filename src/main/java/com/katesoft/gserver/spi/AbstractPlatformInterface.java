package com.katesoft.gserver.spi;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.katesoft.gserver.core.Commands.toReply;

import java.util.concurrent.ConcurrentMap;

import org.apache.commons.chain.Chain;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ChainBase;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.CloseGamePlayAndLogoutCommand;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.LoginCommandReply;
import com.katesoft.gserver.commands.Commands.OpenGamePlayCommand;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.commands.Commands.UpdatePlayerSettingsCommand;
import com.katesoft.gserver.core.AbstractPlayer;
import com.katesoft.gserver.core.AbstractPlayerSession;
import com.katesoft.gserver.core.CommandsQualifierCodec;
import com.katesoft.gserver.core.NetworkCommandContext;

public abstract class AbstractPlatformInterface implements PlatformInterface {
    private final GamePlayContext ctx;
    private final CommandsQualifierCodec codec;
    private final Chain chain;

    private final ConcurrentMap<String, Player> players = Maps.newConcurrentMap();
    private final ConcurrentMap<String, Class<? extends Game>> games = Maps.newConcurrentMap();

    public AbstractPlatformInterface(GamePlayContext ctx, CommandsQualifierCodec codec) {
        this.ctx = ctx;
        this.codec = codec;
        this.chain = initChain();
    }
    public void setSupportedGames(Class<? extends Game>... games) {
        for ( Class<? extends Game> cl : games ) {
            this.games.put( cl.getSimpleName(), cl );
        }
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

                if ( LoginCommand.class == type ) {
                    LoginCommand login = cmd.getExtension( LoginCommand.cmd );
                    Player player = login( login.getPlayerId(), login.getCredentials() );
                    uc.asociatePlayer( player );
                    LoginCommandReply reply = LoginCommandReply.newBuilder().setReq( login ).build();
                    uc.writeAsync( toReply( cmd, codec, LoginCommandReply.cmd, reply ) );
                    return PROCESSING_COMPLETE;
                }
                else if ( OpenGamePlayCommand.class == type ) {
                    OpenGamePlayCommand openGamePlay = cmd.getExtension( OpenGamePlayCommand.cmd );
                    Player player = uc.getAssociatedPlayer();
                    PlayerSession playerSession = openPlayerSession( openGamePlay.getGameId(), player, uc );
                    player.addPlayerSession( playerSession );
                    OpenGamePlayReply reply = OpenGamePlayReply.newBuilder().setReq( openGamePlay ).setSessionId( playerSession.id() ).build();
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
    protected Player login(String playerId, String credentials) {
        Player player = players.get( playerId );
        if ( player == null ) {
            player = new AbstractPlayer( playerId, playerId + "@my.com" ) {};
            Player prev = players.putIfAbsent( playerId, player );
            if ( prev != null ) {
                player = prev;
            }
        }
        return player;
    }
    protected void logout(Player player, String sessionId) {
        player.closePlayerSession( sessionId );
    }
    protected PlayerSession openPlayerSession(final String gameId, final Player player, final UserConnection uc) {
        return new AbstractPlayerSession( uc, newGameInstance( gameId ), player ) {
            @Override
            public String id() {
                return uc.id();
            }
        };
    }
    protected Game newGameInstance(final String gameId) {
        Class<? extends Game> c = checkNotNull( games.get( gameId ), "Game=%s not supported", gameId );
        for ( ;; ) {
            try {
                Game game = c.newInstance();
                game.init( gamePlayContext() );
                return game;
            }
            catch ( Exception e ) {
                Throwables.propagate( e );
            }
        }
    }
}

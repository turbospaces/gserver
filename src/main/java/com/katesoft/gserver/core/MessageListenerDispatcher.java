package com.katesoft.gserver.core;

import static com.katesoft.gserver.core.Commands.toReply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.CloseGamePlayAndLogoutCommand;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.LoginCommandReply;
import com.katesoft.gserver.commands.Commands.OpenGamePlayCommand;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.spi.PlatformInterface;
import com.katesoft.gserver.transport.TransportMessageListener;

public class MessageListenerDispatcher implements TransportMessageListener {
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    private final PlatformInterface platformInterface;

    @Override
    public void onMessage(BaseCommand cmd, UserConnection uc) throws Exception {
        String qualifier = cmd.getQualifier();
        Class<? extends GeneratedMessage> type = platformInterface.commandsCodec().qualifierToType().apply( qualifier );

        if ( cmd.getDebug() ) {
            logger.debug( "onMessage(connection={})={}", uc.id(), cmd );
        }

        if ( LoginCommand.class == type ) {
            LoginCommand login = cmd.getExtension( LoginCommand.cmd );
            Player player = platformInterface.login( login.getPlayerId(), login.getCredentials() );
            uc.asociatePlayer( player );
            LoginCommandReply reply = LoginCommandReply.newBuilder().setReq( login ).build();
            uc.writeAsync( toReply( cmd, platformInterface.commandsCodec(), LoginCommandReply.cmd, reply ) );
        }
        else if ( OpenGamePlayCommand.class == type ) {
            OpenGamePlayCommand openGamePlay = cmd.getExtension( OpenGamePlayCommand.cmd );
            Player player = uc.getAssociatedPlayer();
            PlayerSession playerSession = platformInterface.openPlayerSession( openGamePlay.getGameId(), player, uc );
            player.addPlayerSession( playerSession );
            OpenGamePlayReply reply = OpenGamePlayReply.newBuilder().setReq( openGamePlay ).setSessionId( playerSession.id() ).build();
            uc.writeAsync( toReply( cmd, platformInterface.commandsCodec(), OpenGamePlayReply.cmd, reply ) );
        }
        else if ( CloseGamePlayAndLogoutCommand.class == type ) {
            Player player = uc.getAssociatedPlayer();
            if ( player != null ) {
                platformInterface.logout( player, cmd.getSessionId() );
                uc.close();
            }
        }
        else {
            Player player = uc.getAssociatedPlayer();
            player.dispatchCommand( cmd, platformInterface.commandsCodec(), platformInterface.gamePlayContext() );
        }
    }
    public MessageListenerDispatcher(PlatformInterface platformInterface) {
        this.platformInterface = platformInterface;
    }
}

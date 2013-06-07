package com.katesoft.gserver.core;

import com.katesoft.gserver.api.CommandWrapperEvent;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.LogoutCommand;
import com.katesoft.gserver.commands.Commands.OpenGamePlayCommand;
import com.katesoft.gserver.spi.AuthService;
import com.katesoft.gserver.spi.GameControlService;
import com.katesoft.gserver.transport.TransportMessageListener;

public class MessageListenerDispatcher implements TransportMessageListener {
	private CommandsQualifierCodec codec = new CommandsQualifierCodec.DefaultCommandsCodec();
    private AuthService authService;
    private GameControlService gameCtlService;

    @Override
    public void onMessage(BaseCommand cmd, UserConnection userConnection) throws Exception {
        String qualifier = cmd.getQualifier();

        if ( LoginCommand.class.getSimpleName().equals( qualifier ) ) {
            LoginCommand login = cmd.getExtension( LoginCommand.cmd );
            Player player = authService.login( login.getPlayerId(), login.getCredentials() );
            userConnection.asociatePlayer( player );
        }
        else if ( LogoutCommand.class.getSimpleName().equals( qualifier ) ) {
            Player player = userConnection.getAssociatedPlayer();
            if ( player != null ) {
                authService.logout( player );
                userConnection.close();
            }
        }
        else if ( OpenGamePlayCommand.class.getSimpleName().equals( qualifier ) ) {
            OpenGamePlayCommand openGamePlay = cmd.getExtension( OpenGamePlayCommand.cmd );
            Player player = userConnection.getAssociatedPlayer();
            PlayerSession playerSession = gameCtlService.openOrRestorePlayerSession( openGamePlay.getGameId(), player );
            player.addPlayerSession( playerSession );
        }
        else {
            Player player = userConnection.getAssociatedPlayer();
            player.dispatchCommand( new CommandWrapperEvent(cmd, codec) );
        }
    }
}

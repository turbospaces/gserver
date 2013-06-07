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
import com.katesoft.gserver.spi.GamesAdminService;
import com.katesoft.gserver.transport.TransportMessageListener;

public class MessageListenerDispatcher implements TransportMessageListener {
    private final CommandsQualifierCodec codec = new CommandsQualifierCodec.DefaultCommandsCodec();
    private AuthService authService;
    private GamesAdminService gameCtlService;

    @Override
    public void onMessage(BaseCommand cmd, UserConnection uc) throws Exception {
        String qualifier = cmd.getQualifier();

        if ( LoginCommand.class.getSimpleName().equals( qualifier ) ) {
            LoginCommand login = cmd.getExtension( LoginCommand.cmd );
            Player player = authService.login( login.getPlayerId(), login.getCredentials() );
            uc.asociatePlayer( player );
            uc.writeAsync( cmd );
        }
        else if ( LogoutCommand.class.getSimpleName().equals( qualifier ) ) {
            Player player = uc.getAssociatedPlayer();
            if ( player != null ) {
                authService.logout( player );
                uc.close();
            }
        }
        else if ( OpenGamePlayCommand.class.getSimpleName().equals( qualifier ) ) {
            OpenGamePlayCommand openGamePlay = cmd.getExtension( OpenGamePlayCommand.cmd );
            Player player = uc.getAssociatedPlayer();
            PlayerSession playerSession = gameCtlService.openOrRestorePlayerSession( openGamePlay.getGameId(), player, uc );
            player.addPlayerSession( playerSession );
        }
        else {
            Player player = uc.getAssociatedPlayer();
            player.dispatchCommand( new CommandWrapperEvent( cmd, codec ) );
        }
    }
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }
    public void setGameCtlService(GamesAdminService gameCtlService) {
        this.gameCtlService = gameCtlService;
    }
}

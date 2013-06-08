package com.katesoft.gserver.core;

import static com.katesoft.gserver.core.Commands.toReply;

import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.LoginCommandReply;
import com.katesoft.gserver.commands.Commands.LogoutCommand;
import com.katesoft.gserver.commands.Commands.OpenGamePlayCommand;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.spi.AuthService;
import com.katesoft.gserver.spi.GamesAdminService;
import com.katesoft.gserver.transport.TransportMessageListener;

public class MessageListenerDispatcher implements TransportMessageListener {
    private CommandsQualifierCodec codec;
    private AuthService authService;
    private GamesAdminService gameCtlService;

    @Override
    public void onMessage(BaseCommand cmd,
                          UserConnection uc) throws Exception {
        String qualifier = cmd.getQualifier();

        if ( LoginCommand.class.getSimpleName().equals(qualifier) ) {
            LoginCommand login = cmd.getExtension(LoginCommand.cmd);
            Player player = authService.login(login.getPlayerId(), login.getCredentials());
            uc.asociatePlayer(player);
            LoginCommandReply reply = LoginCommandReply.newBuilder().setReq(login).build();
            uc.writeAsync(toReply(cmd, codec, LoginCommandReply.cmd, reply));
        }
        else if ( OpenGamePlayCommand.class.getSimpleName().equals(qualifier) ) {
            OpenGamePlayCommand openGamePlay = cmd.getExtension(OpenGamePlayCommand.cmd);
            Player player = uc.getAssociatedPlayer();
            PlayerSession playerSession = gameCtlService.openOrRestorePlayerSession(openGamePlay.getGameId(), player, uc);
            player.addPlayerSession(playerSession);
            OpenGamePlayReply reply =
            		OpenGamePlayReply.newBuilder().setReq(openGamePlay).setSessionId(playerSession.id())
                            .setInactiveTimeout(playerSession.inactivityTimeoutSeconds()).build();
            uc.writeAsync(toReply(cmd, codec, OpenGamePlayReply.cmd, reply));
        }
        else if ( LogoutCommand.class.getSimpleName().equals(qualifier) ) {
            Player player = uc.getAssociatedPlayer();
            if ( player != null ) {
                authService.logout(player);
                uc.close();
            }
        }
        else {
            Player player = uc.getAssociatedPlayer();
            player.dispatchCommand(cmd, codec);
        }
    }
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }
    public void setGameCtlService(GamesAdminService gameCtlService) {
        this.gameCtlService = gameCtlService;
    }
    public void setCommandsQualifierCodec(CommandsQualifierCodec codec) {
        this.codec = codec;
    }
}

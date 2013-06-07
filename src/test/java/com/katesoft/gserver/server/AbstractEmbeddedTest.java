package com.katesoft.gserver.server;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.core.MessageListenerDispatcher;
import com.katesoft.gserver.games.RouletteGame;
import com.katesoft.gserver.misc.Misc;
import com.katesoft.gserver.spi.AuthService;
import com.katesoft.gserver.spi.GamesAdminService;
import com.katesoft.gserver.transport.NettyServer;
import com.katesoft.gserver.transport.NettyTcpClient;

public abstract class AbstractEmbeddedTest {
    public static final Random RND = new SecureRandom();

    protected static NettyServer s;
    protected static NettyTcpClient c;
    protected static UserConnection uc;

    Logger logger = LoggerFactory.getLogger( getClass() );

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() {
        MessageListenerDispatcher mld = new MessageListenerDispatcher();
        mld.setAuthService( new AuthService.MockAuthService() );
        mld.setGameCtlService( new GamesAdminService.MockGameControlService( RouletteGame.class ) );

        HostAndPort hostAndPort = HostAndPort.fromParts( Misc.shortHostname(), Misc.nextAvailablePort() );
        s = new NettyServer();
        s.startServer( hostAndPort, mld );

        c = new NettyTcpClient( hostAndPort );
        c.run();
        uc = s.awaitForHandshake( c );
    }
    @AfterClass
    public static void afterClass() {
        try {
            c.close();
        }
        finally {
            s.close();
        }
    }
    protected Optional<LoginCommand> login() throws InterruptedException, ExecutionException {
        LoginCommand logCmd = LoginCommand.newBuilder().setPlayerId( "playerX" ).setCredentials( "tokenX" ).setClientPlatform( "flash" ).build();
        ListenableFuture<BaseCommand> f = c.callAsync( LoginCommand.cmd, logCmd, Optional.<LoginCommand> absent() );
        BaseCommand bcmd = f.get();
        return Optional.fromNullable( bcmd.getExtension( LoginCommand.cmd ) );
    }
}

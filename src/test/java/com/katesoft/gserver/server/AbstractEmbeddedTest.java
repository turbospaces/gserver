package com.katesoft.gserver.server;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HostAndPort.fromParts;
import static com.katesoft.gserver.misc.Misc.nextAvailablePort;
import static com.katesoft.gserver.misc.Misc.shortHostname;
import static com.katesoft.gserver.misc.Misc.shutdownExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HostAndPort;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.api.GamePlayContext.AbstractGamePlayContext;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.LoginCommandReply;
import com.katesoft.gserver.commands.Commands.OpenGamePlayCommand;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.core.CommandsQualifierCodec;
import com.katesoft.gserver.core.MessageListenerDispatcher;
import com.katesoft.gserver.games.RouletteGame;
import com.katesoft.gserver.misc.Misc;
import com.katesoft.gserver.spi.PlatformInterface;
import com.katesoft.gserver.transport.NettyServer;
import com.katesoft.gserver.transport.NettyTcpClient;

public abstract class AbstractEmbeddedTest {
    public static final CommandsQualifierCodec CODEC = new CommandsQualifierCodec.DefaultCommandsCodec();
    public static final ScheduledExecutorService SCHEDULED_EXEC = newSingleThreadScheduledExecutor();

    protected static NettyServer s;
    protected static NettyTcpClient c;
    protected static UserConnection uc;

    protected Logger logger = LoggerFactory.getLogger( getClass() );

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() {
        AbstractGamePlayContext ctx = new GamePlayContext.AbstractGamePlayContext( SCHEDULED_EXEC, Misc.RANDOM ) {};
        PlatformInterface platform = new PlatformInterface.MockPlatformInterface( ctx, CODEC, RouletteGame.class );
        MessageListenerDispatcher mld = new MessageListenerDispatcher( platform );

        HostAndPort hostAndPort = fromParts( shortHostname(), nextAvailablePort() );
        s = new NettyServer();
        s.startServer( hostAndPort, mld );

        c = new NettyTcpClient( hostAndPort, CODEC );
        c.setCommandsQualifierCodec( CODEC );
        c.run();
        uc = s.awaitForHandshake( c );
    }
    @AfterClass
    public static void afterClass() {
        shutdownExecutor( SCHEDULED_EXEC );
        try {
            c.close();
        }
        finally {
            s.close();
        }
    }
    protected void login() throws InterruptedException, ExecutionException {
        LoginCommand cmd = LoginCommand.newBuilder().setPlayerId( "playerX" ).setCredentials( "tokenX" ).setClientPlatform( "flash" ).build();
        BaseCommand bcmd = c.callAsync( LoginCommand.cmd, cmd, null ).get();
        checkNotNull( bcmd.getExtension( LoginCommandReply.cmd ) );
    }
    protected OpenGamePlayReply openGamePlay(Class<? extends Game> game) throws InterruptedException, ExecutionException {
        OpenGamePlayCommand cmd = OpenGamePlayCommand.newBuilder().setGameId( game.getSimpleName() ).build();
        BaseCommand bcmd = c.callAsync( OpenGamePlayCommand.cmd, cmd, null ).get();
        OpenGamePlayReply reply = bcmd.getExtension( OpenGamePlayReply.cmd );
        checkNotNull( reply.getSessionId() );
        return reply;
    }
}

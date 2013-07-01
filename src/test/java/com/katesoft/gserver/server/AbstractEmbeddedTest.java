package com.katesoft.gserver.server;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HostAndPort.fromParts;
import static com.katesoft.gserver.misc.Misc.shutdownExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.api.CommandWrapperEvent;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.api.GamePlayContext.AbstractGamePlayContext;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.TransportServer;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.LoginCommandReply;
import com.katesoft.gserver.commands.Commands.MessageHeaders;
import com.katesoft.gserver.commands.Commands.OpenGamePlayCommand;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.core.CommandsQualifierCodec;
import com.katesoft.gserver.core.CommandsQualifierCodec.DefaultCommandsCodec;
import com.katesoft.gserver.core.MessageListenerDispatcher;
import com.katesoft.gserver.games.RouletteGame;
import com.katesoft.gserver.games.roullete.RoulleteCommands;
import com.katesoft.gserver.misc.Misc;
import com.katesoft.gserver.spi.PlatformInterface;
import com.katesoft.gserver.transport.ConnectionType;
import com.katesoft.gserver.transport.NettyServer;
import com.katesoft.gserver.transport.NettyTcpClient;
import com.katesoft.gserver.transport.ProxyServer;

public abstract class AbstractEmbeddedTest {
    public static final ScheduledExecutorService SCHEDULED_EXEC = newSingleThreadScheduledExecutor();
    public static final ExtensionRegistry EXTENSION_REGISTRY = com.katesoft.gserver.core.Commands.newMessageRegistry();

    protected static NettyServer s;
    protected static NettyTcpClient c;
    protected static UserConnection uc;
    protected ConnectionType connectionType = ConnectionType.TCP;

    protected Logger logger = LoggerFactory.getLogger( getClass() );

    static {
        RoulleteCommands.registerAllExtensions( EXTENSION_REGISTRY );
    }

    @Before
    public void setup() throws Exception {
        if ( s == null ) {
            MessageListenerDispatcher mld = mockMessageListener();
            TransportServer.TransportServerSettings settings = TransportServer.TransportServerSettings.avail();

            s = new NettyServer();
            s.startServer( settings, mld );

            HostAndPort x = ( connectionType == ConnectionType.TCP ? settings.tcp : settings.websockets.get() );

            ProxyServer proxy = setupProxy( s, x );

            if ( proxy != null ) {
                x = proxy.getProxyAddress();
            }

            c = new NettyTcpClient( x, mld.getPlatformInterface().commandsCodec(), connectionType );
            c.run();
            if ( proxy == null ) {
                uc = s.awaitForClientHandshake( c.get() );
            }
        }
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
    @SuppressWarnings({ "unchecked" })
    public static <T> CommandWrapperEvent mockCommandEvent(GeneratedMessage.GeneratedExtension<BaseCommand, T> extension, T t,
                                                           PlayerSession playerSession, DefaultCommandsCodec codec) {
        long tmstmp = System.currentTimeMillis();
        MessageHeaders headers = MessageHeaders
                .newBuilder()
                .setCorrelationID( UUID.randomUUID().toString() )
                .setMessageTimestamp( tmstmp )
                .setSequenceNumber( (short) tmstmp )
                .build();

        Builder b = BaseCommand.newBuilder().setProtocolVersion( "1.0" ).setExtension( extension, t ).setHeaders( headers );
        b = codec.encoder().apply( (Pair<Builder, GeneratedMessage>) ImmutablePair.of( b, t ) );

        return new CommandWrapperEvent( b.build(), codec, playerSession );
    }
    @SuppressWarnings("unused")
    protected ProxyServer setupProxy(NettyServer ns, HostAndPort actualPort) throws Exception {
        return null;
    }
    protected void login() throws InterruptedException, ExecutionException {
        LoginCommand cmd = LoginCommand.newBuilder().setPlayerId( "playerX" ).setCredentials( "tokenX" ).setClientPlatform( "flash" ).build();
        BaseCommand bcmd = c.callAsync( LoginCommand.cmd, cmd, null, true ).get();
        checkNotNull( bcmd.getExtension( LoginCommandReply.cmd ) );
    }
    protected OpenGamePlayReply openGamePlay(Class<? extends Game> game) throws InterruptedException, ExecutionException {
        OpenGamePlayCommand cmd = OpenGamePlayCommand.newBuilder().setGameId( game.getSimpleName() ).build();
        BaseCommand bcmd = c.callAsync( OpenGamePlayCommand.cmd, cmd, null, true ).get();
        OpenGamePlayReply reply = bcmd.getExtension( OpenGamePlayReply.cmd );
        checkNotNull( reply.getSessionId() );
        return reply;
    }

    @SuppressWarnings("unchecked")
    public static MessageListenerDispatcher mockMessageListener() {
        DefaultCommandsCodec codec = new CommandsQualifierCodec.DefaultCommandsCodec( EXTENSION_REGISTRY );
        AbstractGamePlayContext ctx = new GamePlayContext.AbstractGamePlayContext( SCHEDULED_EXEC, Misc.RANDOM ) {};
        PlatformInterface platform = new PlatformInterface.MockPlatformInterface( ctx, codec, RouletteGame.class );
        MessageListenerDispatcher mld = new MessageListenerDispatcher( platform, EXTENSION_REGISTRY );
        return mld;
    }

    public static void main(String... args) throws InterruptedException {
        TransportServer.TransportServerSettings settings = new TransportServer.TransportServerSettings();
        settings.tcp = fromParts( "localhost", 8189 );
        settings.websockets = Optional.of( fromParts( "localhost", 8190 ) );

        MessageListenerDispatcher mld = mockMessageListener();

        NettyServer ms = new NettyServer();
        ms.startServer( settings, mld );
        synchronized ( ms ) {
            ms.wait();
        }
        ms.close();
    }
}

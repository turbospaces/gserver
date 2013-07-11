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
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.net.HostAndPort;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GameCommand;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.api.GamePlayContext.AbstractGamePlayContext;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.api.TransportServer;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.MessageHeaders;
import com.katesoft.gserver.commands.Commands.OpenGamePlayCommand;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.core.CommandsQualifierCodec;
import com.katesoft.gserver.core.CommandsQualifierCodec.ProtoCommandsCodec;
import com.katesoft.gserver.core.MessageDispatcher;
import com.katesoft.gserver.core.NetworkCommandContext;
import com.katesoft.gserver.domain.AbstractDomainTest;
import com.katesoft.gserver.domain.GameBO;
import com.katesoft.gserver.games.RouletteGame;
import com.katesoft.gserver.games.roullete.RoulleteCommands;
import com.katesoft.gserver.misc.Misc;
import com.katesoft.gserver.spi.AbstractPlatformInterface;
import com.katesoft.gserver.transport.ConnectionType;
import com.katesoft.gserver.transport.NettyServer;
import com.katesoft.gserver.transport.NettyTcpClient;
import com.katesoft.gserver.transport.ProxyServer;

public abstract class AbstractEmbeddedTest extends AbstractDomainTest {
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
            MessageDispatcher mld = messageListener();
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
            try {
                c.close();
            }
            finally {
                s.close();
            }
        }
        finally {
            AbstractDomainTest.afterClass();
        }
    }
    @SuppressWarnings("unused")
    protected ProxyServer setupProxy(NettyServer ns, HostAndPort actualPort) throws Exception {
        return null;
    }
    @SuppressWarnings({ "unchecked" })
    public static <T> GameCommand mockCommandEvent(GeneratedExtension<BaseCommand, T> ext, T t, PlayerSession ps, ProtoCommandsCodec codec) {
        long tmstmp = System.currentTimeMillis();
        MessageHeaders headers = MessageHeaders
                .newBuilder()
                .setCorrelationID( UUID.randomUUID().toString() )
                .setMessageTimestamp( tmstmp )
                .setSequenceNumber( (short) tmstmp )
                .build();

        Builder b = BaseCommand.newBuilder().setProtocolVersion( "1.0" ).setExtension( ext, t ).setHeaders( headers );
        b = codec.encoder().apply( (Pair<Builder, GeneratedMessage>) ImmutablePair.of( b, t ) );

        NetworkCommandContext ctx = new NetworkCommandContext( b.build(), codec, ps.getAssociatedUserConnection() );
        return new GameCommand( ctx, ps );
    }
    protected void login() {
        LoginCommand cmd = LoginCommand.newBuilder().setToken( loginToken ).setClientPlatform( "flash" ).build();
        c.callAsync( LoginCommand.cmd, cmd, null, true );
    }
    protected OpenGamePlayReply openGamePlay(final Class<? extends Game> game) throws InterruptedException, ExecutionException {
        ImmutableSet<GameBO> allGames = repo.findAllGames();
        GameBO bo = Iterables.find( allGames, new Predicate<GameBO>() {
            @Override
            public boolean apply(GameBO input) {
                return input.getGameClassName().equals( game.getName() );
            }
        } );

        OpenGamePlayCommand cmd = OpenGamePlayCommand.newBuilder().setGameId( bo.getPrimaryKey() ).build();
        BaseCommand bcmd = c.callAsync( OpenGamePlayCommand.cmd, cmd, null, true ).get();
        OpenGamePlayReply reply = bcmd.getExtension( OpenGamePlayReply.cmd );
        checkNotNull( reply.getSessionId() );
        return reply;
    }
    public static MessageDispatcher messageListener() {
        ProtoCommandsCodec codec = new CommandsQualifierCodec.ProtoCommandsCodec( EXTENSION_REGISTRY );
        AbstractGamePlayContext ctx = new GamePlayContext.AbstractGamePlayContext( SCHEDULED_EXEC, Misc.RANDOM ) {};

        repo.saveGame( new GameBO( "amrl", "American Roulette", RouletteGame.class.getName() ) );

        AbstractPlatformInterface platform = new AbstractPlatformInterface( ctx, codec, repo, rememberMeServices ) {};
        MessageDispatcher mld = new MessageDispatcher( platform, EXTENSION_REGISTRY );
        return mld;
    }

    public static void main(String... args) throws InterruptedException {
        AbstractDomainTest.beforeClass();
        new AbstractDomainTest(){}.before();
        TransportServer.TransportServerSettings settings = new TransportServer.TransportServerSettings();
        settings.tcp = fromParts( "localhost", 8189 );
        settings.websockets = Optional.of( fromParts( "localhost", 8190 ) );

        MessageDispatcher mld = messageListener();

        NettyServer ms = new NettyServer();
        ms.startServer( settings, mld );
        synchronized ( ms ) {
            ms.wait();
        }
        ms.close();
    }
}

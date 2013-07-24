package com.katesoft.gserver.server;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HostAndPort.fromParts;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static com.katesoft.gserver.misc.Misc.shutdownExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
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
import com.katesoft.gserver.commands.Commands.CloseGamePlayAndLogoutCommand;
import com.katesoft.gserver.commands.Commands.CloseGamePlayAndLogoutReply;
import com.katesoft.gserver.commands.Commands.Geti18nMessagesCommand;
import com.katesoft.gserver.commands.Commands.Geti18nMessagesReply;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.MessageHeaders;
import com.katesoft.gserver.commands.Commands.OpenGamePlayCommand;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.core.CommandsQualifierCodec;
import com.katesoft.gserver.core.CommandsQualifierCodec.ProtoCommandsCodec;
import com.katesoft.gserver.core.NetworkCommandContext;
import com.katesoft.gserver.domain.AbstractDomainTest;
import com.katesoft.gserver.domain.GameBO;
import com.katesoft.gserver.games.RouletteGame;
import com.katesoft.gserver.games.roulette.RoulleteCommands;
import com.katesoft.gserver.misc.Misc;
import com.katesoft.gserver.spi.AbstractPlatformContext;
import com.katesoft.gserver.spi.PlatformContext;
import com.katesoft.gserver.transport.ConnectionType;
import com.katesoft.gserver.transport.NettyServer;
import com.katesoft.gserver.transport.NettyTcpClient;

public abstract class AbstractEmbeddedTest extends AbstractDomainTest {
    public static final ScheduledExecutorService SCHEDULED_EXEC = newSingleThreadScheduledExecutor();
    public static final ExtensionRegistry EXTENSION_REGISTRY = com.katesoft.gserver.core.Commands.newMessageRegistry();

    protected static NettyServer s;
    protected static NettyTcpClient c;
    protected ConnectionType connectionType = ConnectionType.TCP;
    protected TransportServer.TransportServerSettings settings = TransportServer.TransportServerSettings.avail();
    protected PlatformContext ctx;
    protected Logger logger = LoggerFactory.getLogger( getClass() );

    static {
        RoulleteCommands.registerAllExtensions( EXTENSION_REGISTRY );
    }

    @Before
    public void setup() {
        if ( s == null ) {
            ctx = platform();
            s = new NettyServer();
            s.startServer( settings, ctx );
            c = newClient();
        }
    }
    @AfterClass
    public static void afterClass() {
        shutdownExecutor( SCHEDULED_EXEC );
        try {
            try {
                if ( c != null ) {
                    c.close();
                }
            }
            finally {
                if ( s != null ) {
                    s.close();
                }
            }
        }
        finally {
            AbstractDomainTest.afterClass();
        }
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

        NetworkCommandContext ctx = new NetworkCommandContext( b.build(), codec, ps.getUserConnection() );
        return new GameCommand( ctx, ps );
    }
    protected void login() {
        login( c, loginToken );
    }
    protected static void login(NettyTcpClient client, String token) {
        LoginCommand cmd = LoginCommand.newBuilder().setToken( token ).setClientPlatform( "flash" ).build();
        client.callAsync( LoginCommand.cmd, cmd, null );
    }
    protected CloseGamePlayAndLogoutReply logout(String sessionId) throws InterruptedException, ExecutionException {
        CloseGamePlayAndLogoutCommand cmd = CloseGamePlayAndLogoutCommand.newBuilder().setForceCloseConnection( false ).build();
        BaseCommand bcmd = c.callAsync( CloseGamePlayAndLogoutCommand.cmd, cmd, sessionId ).get();
        return bcmd.getExtension( CloseGamePlayAndLogoutReply.cmd );
    }
    protected Geti18nMessagesReply geti18nMessages(Collection<String> keys) throws InterruptedException, ExecutionException {
        Geti18nMessagesCommand cmd = Geti18nMessagesCommand.newBuilder().setLocale( "ru" ).addAllKeys( keys ).build();
        BaseCommand bcmd = c.callAsync( Geti18nMessagesCommand.cmd, cmd, null ).get();
        return bcmd.getExtension( Geti18nMessagesReply.cmd );
    }
    protected OpenGamePlayReply openGamePlay(final Class<? extends Game> game) {
        return openGamePlay( game, c );
    }
    protected OpenGamePlayReply openGamePlay(final Class<? extends Game> game, NettyTcpClient client) {
        ImmutableSet<GameBO> allGames = repo.findAllGames();
        GameBO bo = Iterables.find( allGames, new Predicate<GameBO>() {
            @Override
            public boolean apply(GameBO input) {
                return input.getGameClassName().equals( game.getName() );
            }
        } );

        OpenGamePlayCommand cmd = OpenGamePlayCommand.newBuilder().setGameId( bo.getPrimaryKey() ).build();
        for ( ;; )
            try {
                BaseCommand bcmd = getUninterruptibly( client.callAsync( OpenGamePlayCommand.cmd, cmd, null ) );
                OpenGamePlayReply reply = bcmd.getExtension( OpenGamePlayReply.cmd );
                checkNotNull( reply.getSessionId() );
                return reply;
            }
            catch ( ExecutionException e ) {
                Throwables.propagate( e );
            }
    }
    protected NettyTcpClient newClient() {
        HostAndPort x = ( connectionType == ConnectionType.TCP ? settings.tcp : settings.websockets.get() );
        NettyTcpClient client = new NettyTcpClient( x, ctx.commandsCodec(), connectionType );
        client.run();
        UserConnection userConnection = s.awaitForClientHandshake( client.get() );
        client.associateUserConnection( userConnection );
        return client;
    }
    public static PlatformContext platform() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename( "classpath:messages" );
        ms.setDefaultEncoding( "UTF-8" );

        ProtoCommandsCodec codec = new CommandsQualifierCodec.ProtoCommandsCodec( EXTENSION_REGISTRY );
        AbstractGamePlayContext ctx = new GamePlayContext.AbstractGamePlayContext( SCHEDULED_EXEC, Misc.RANDOM, ms ) {};

        repo.saveGame( new GameBO( "amrl", "American Roulette", RouletteGame.class.getName() ) );

        return new AbstractPlatformContext( ctx, codec, repo, rememberMeServices ) {};
    }

    public static void main(String... args) throws InterruptedException {
        AbstractDomainTest.beforeClass();
        new AbstractDomainTest() {}.before();
        TransportServer.TransportServerSettings settings = new TransportServer.TransportServerSettings();
        settings.tcp = fromParts( "localhost", 8189 );
        settings.websockets = Optional.of( fromParts( "localhost", 8190 ) );

        PlatformContext ctx = platform();

        NettyServer ms = new NettyServer();
        ms.startServer( settings, ctx );
        synchronized ( ms ) {
            ms.wait();
        }
        ms.close();
    }
}

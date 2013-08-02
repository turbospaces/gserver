package com.katesoft.gserver.transport;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HostAndPort;
import com.google.protobuf.ExtensionRegistry;
import com.katesoft.gserver.api.TransportServer;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands;
import com.katesoft.gserver.domain.Entities.ServerSettings;
import com.katesoft.gserver.misc.Misc;
import com.katesoft.gserver.spi.PlatformContext;

public class NettyServer implements TransportServer<SocketChannel> {
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final EventLoopGroup tcpEventGroup = new NioEventLoopGroup( Runtime.getRuntime().availableProcessors() ) {
        @Override
        protected ThreadFactory newDefaultThreadFactory() {
            return new DefaultThreadFactory( "tcp-pool", Thread.MAX_PRIORITY );
        }
    };
    private final EventLoopGroup wsEventGroup = new NioEventLoopGroup( Runtime.getRuntime().availableProcessors() ) {
        @Override
        protected ThreadFactory newDefaultThreadFactory() {
            return new DefaultThreadFactory( "websockets-pool", Thread.MAX_PRIORITY );
        }
    };

    private ChannelDispatchHandler root;
    private ServerSettings settings;
    private PlatformContext platform;

    @Override
    public void startServer(final ServerSettings s, final PlatformContext ctx) {
        this.settings = s;
        this.platform = ctx;
        root = new ChannelDispatchHandler( ctx, s );

        final ServerBootstrap tcpBootstrap = new ServerBootstrap();
        tcpBootstrap
                .group( tcpEventGroup )
                .channel( NioServerSocketChannel.class )
                .handler( new LoggingHandler( LogLevel.DEBUG ) )
                .childHandler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        registerProtobufCodecs( p, platform.commandsCodec().extensionRegistry() ).addLast( root );
                    }
                } );
        applyChannelOptions( tcpBootstrap, settings );
        HostAndPort hostAndPort = HostAndPort.fromString( settings.getTcpBindAddress() );
        tcpBootstrap.bind( hostAndPort.getHostText(), hostAndPort.getPort() ).syncUninterruptibly();

        if ( settings.hasWebsocketsBindAddress() ) {
            final ServerBootstrap webSocksBootstap = new ServerBootstrap();
            webSocksBootstap
                    .group( wsEventGroup )
                    .channel( NioServerSocketChannel.class )
                    .handler( new LoggingHandler( LogLevel.DEBUG ) )
                    .childHandler( new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast( "codec-http", new HttpServerCodec() );
                            p.addLast( "aggregator", new HttpObjectAggregator( 65536 ) );
                            p.addLast( root );
                        }
                    } );
            applyChannelOptions( webSocksBootstap, settings );
            hostAndPort = HostAndPort.fromString( settings.getWebsocketsBindAddress() );
            webSocksBootstap.bind( hostAndPort.getHostText(), hostAndPort.getPort() ).syncUninterruptibly();
        }
    }
    protected void applyChannelOptions(ServerBootstrap bootstrap, ServerSettings serverSettings) {
        bootstrap.option( ChannelOption.SO_KEEPALIVE, true );
        bootstrap.option( ChannelOption.TCP_NODELAY, true );
        bootstrap.option( ChannelOption.SO_REUSEADDR, false );

        if ( serverSettings.hasTcpSendBufferSize() ) {
            bootstrap.option( ChannelOption.SO_SNDBUF, serverSettings.getTcpSendBufferSize() );
        }
        if ( serverSettings.hasTcpReceiveBufferSize() ) {
            bootstrap.option( ChannelOption.SO_RCVBUF, serverSettings.getTcpReceiveBufferSize() );
        }
        if ( serverSettings.hasTcpSocketLinger() ) {
            bootstrap.option( ChannelOption.SO_LINGER, serverSettings.getTcpSocketLinger() );
        }

        bootstrap.childOption( ChannelOption.SO_KEEPALIVE, true );
        bootstrap.childOption( ChannelOption.TCP_NODELAY, true );
    }
    @Override
    public void close() {
        long took = Misc.benchmark( new Runnable() {
            @Override
            public void run() {
                try {
                    root.close();
                }
                finally {
                    tcpEventGroup.shutdownGracefully();
                    wsEventGroup.shutdownGracefully();
                }
            }
        } );
        logger.info( "Closed Netty Acceptor in = {} ms", took );
    }
    @Override
    public UserConnection getUserConnection(String id) {
        return root.get().get( id );
    }
    @Override
    public int connectionsCount() {
        return root.get().size();
    }
    public ServerSettings transportSettings() {
        return settings;
    }
    @Override
    public UserConnection awaitForClientHandshake(SocketChannel clientChannel) {
        return root.awaitForClientHandshake( clientChannel );
    }
    public static ChannelPipeline registerProtobufCodecs(ChannelPipeline p, ExtensionRegistry registry) {
        p.addLast( "frameDecoder", new LengthFieldBasedFrameDecoder( 1048576, 0, 4, 0, 4 ) );
        p.addLast( "protobufDecoder", new ProtobufDecoder( Commands.BaseCommand.getDefaultInstance(), registry ) );

        p.addLast( "frameEncoder", new LengthFieldPrepender( 4 ) );
        p.addLast( "protobufEncoder", new ProtobufEncoder() );
        return p;
    }
}

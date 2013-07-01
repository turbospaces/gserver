package com.katesoft.gserver.transport;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
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

import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ExtensionRegistry;
import com.katesoft.gserver.api.TransportServer;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands;
import com.katesoft.gserver.misc.Misc;

public class NettyServer implements TransportServer<SocketChannel> {
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final EventLoopGroup eventGroup = new NioEventLoopGroup();
    private TransportMessageListener listener;
    private RootDispatchHandler root;
    private TransportServerSettings settings;

    @Override
    public void startServer(final TransportServer.TransportServerSettings s, final TransportMessageListener l) {
        this.settings = s;
        this.listener = l;

        root = new RootDispatchHandler( l );

        final ServerBootstrap tcpBootstrap = new ServerBootstrap();
        tcpBootstrap
                .group( eventGroup )
                .channel( NioServerSocketChannel.class )
                .handler( new LoggingHandler( LogLevel.DEBUG ) )
                .childHandler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        registerProtobufCodecs( p, l.extentionRegistry() ).addLast( root );
                    }
                } );
        tcpBootstrap.bind( settings.tcp.getHostText(), settings.tcp.getPort() ).syncUninterruptibly();

        if ( settings.websockets.isPresent() ) {
            final ServerBootstrap webSocksBootstap = new ServerBootstrap();
            webSocksBootstap
                    .group( eventGroup )
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
            webSocksBootstap.bind( settings.websockets.get().getHostText(), settings.websockets.get().getPort() ).syncUninterruptibly();
        }
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
                    eventGroup.shutdownGracefully();
                }
            }
        } );
        logger.info( "Closed Netty Acceptor in = {} ms", took );
    }
    @Override
    public UserConnection getUserConnection(String id) {
        return root.find( id );
    }
    @Override
    public int connectionsCount() {
        return root.get().size();
    }
    @Override
    public UserConnection awaitForClientHandshake(SocketChannel clientChannel) {
        for ( ;; ) {
            for ( Channel c : root.get() ) {
                SocketAddress remoteAddress = c.remoteAddress();
                if ( clientChannel.localAddress().equals( remoteAddress ) ) {
                    return root.find( (SocketChannel) c );
                }
            }
            sleepUninterruptibly( 1, MILLISECONDS );
        }
    }
    public TransportServerSettings transportSettings() {
        return settings;
    }
    public TransportMessageListener transportMessageListener() {
        return listener;
    }
    public static ChannelPipeline registerProtobufCodecs(ChannelPipeline p, ExtensionRegistry registry) {
        p.addLast( "frameDecoder", new LengthFieldBasedFrameDecoder( 1048576, 0, 4, 0, 4 ) );
        p.addLast( "protobufDecoder", new ProtobufDecoder( Commands.BaseCommand.getDefaultInstance(), registry ) );

        p.addLast( "frameEncoder", new LengthFieldPrepender( 4 ) );
        p.addLast( "protobufEncoder", new ProtobufEncoder() );
        return p;
    }
}

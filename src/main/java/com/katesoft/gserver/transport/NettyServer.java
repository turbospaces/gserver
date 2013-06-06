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
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HostAndPort;
import com.google.protobuf.ExtensionRegistry;
import com.katesoft.gserver.api.TransportServer;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands;
import com.katesoft.gserver.misc.Misc;

public class NettyServer implements TransportServer {
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final EventLoopGroup eventGroup = new NioEventLoopGroup();
    private RootDispatchHandler root;

    @Override
    public void startServer(final HostAndPort binding, TransportMessageListener rootMessageListener) {
        root = new RootDispatchHandler( rootMessageListener );

        final ExtensionRegistry registry = ExtensionRegistry.newInstance();
        Commands.registerAllExtensions( registry );

        final ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap
                .group( eventGroup )
                .channel( NioServerSocketChannel.class )
                .handler( new LoggingHandler( LogLevel.DEBUG ) )
                .childHandler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        registerProtobufCodecs( p, registry ).addLast( root );
                    }
                } );

        long took = Misc.benchmark( new Runnable() {
            @Override
            public void run() {
                bootstrap.bind( binding.getHostText(), binding.getPort() ).syncUninterruptibly();
            }
        } );

        logger.info( "{} :=> {} started in {} ms", binding, bootstrap, took );
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
    public UserConnection awaitForHandshake(NettyTcpClient client) {
        SocketChannel sch = client.get();
        for ( ;; ) {
            for ( Channel c : root.get() ) {
                SocketAddress remoteAddress = c.remoteAddress();
                if ( sch.localAddress().equals( remoteAddress ) ) {
                    return root.find( (SocketChannel) c );
                }
            }
            sleepUninterruptibly( 1, MILLISECONDS );
        }
    }
    public static ChannelPipeline registerProtobufCodecs(ChannelPipeline p, ExtensionRegistry registry) {
        p.addLast( "frameDecoder", new ProtobufVarint32FrameDecoder() );
        p.addLast( "protobufDecoder", new ProtobufDecoder( Commands.BaseCommand.getDefaultInstance(), registry ) );

        p.addLast( "frameEncoder", new ProtobufVarint32LengthFieldPrepender() );
        p.addLast( "protobufEncoder", new ProtobufEncoder() );
        return p;
    }
}

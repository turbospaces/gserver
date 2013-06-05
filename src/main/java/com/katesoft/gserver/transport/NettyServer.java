package com.katesoft.gserver.transport;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;
import com.katesoft.gserver.core.TransportServer;
import com.katesoft.gserver.core.UserConnection;
import com.katesoft.gserver.misc.Misc;

public class NettyServer implements TransportServer {
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final EventLoopGroup eventGroup = new NioEventLoopGroup();
    private final RootHandler root = new RootHandler();

    @Override
    public void startServer(final HostAndPort binding) {
        final ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap
                .group( eventGroup )
                .channel( NioServerSocketChannel.class )
                .childHandler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast( new LoggingHandler( LogLevel.DEBUG ), root );
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
    public Optional<UserConnection> find(String id) {
        return Optional.of( root.find( id ) );
    }
    @Override
    public int connectionsCount() {
        return root.connections.size();
    }
    public void awaitForHandshake(NettyTcpClient client) {}
}

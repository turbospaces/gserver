package com.katesoft.gserver.transport;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                .handler( new LoggingHandler( LogLevel.DEBUG ) )
                .childHandler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast( root );
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
}

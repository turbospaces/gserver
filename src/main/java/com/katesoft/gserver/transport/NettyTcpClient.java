package com.katesoft.gserver.transport;

import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.Closeable;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.net.HostAndPort;

public class NettyTcpClient implements Runnable, Closeable, Supplier<SocketChannel> {
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    private final EventLoopGroup eventGroup = new NioEventLoopGroup();
    private final HostAndPort hostAndPort;
    private SocketChannel sch;

    public NettyTcpClient(HostAndPort hostAndPort) {
        this.hostAndPort = hostAndPort;
    }
    @Override
    public void run() {
        final CountDownLatch l = new CountDownLatch( 1 );
        Bootstrap b = new Bootstrap();
        b
                .group( eventGroup )
                .channel( NioSocketChannel.class )
                .option( ChannelOption.TCP_NODELAY, true )
                .option( ChannelOption.SO_KEEPALIVE, true )
                .handler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast( new ChannelInboundByteHandlerAdapter() {
                            @Override
                            protected void inboundBufferUpdated(ChannelHandlerContext ctx, ByteBuf in) {}
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) {
                                sch = (SocketChannel) ctx.pipeline().channel();
                                logger.debug( "NettyTcpClient connected via={}", sch );
                                l.countDown();
                            }
                        } );
                    }
                } );

        b.connect( hostAndPort.getHostText(), hostAndPort.getPort() ).syncUninterruptibly();
        awaitUninterruptibly( l );
    }
    @Override
    public SocketChannel get() {
        return sch;
    }
    @Override
    public void close() {
        if ( eventGroup != null ) {
            eventGroup.shutdownGracefully();
        }
    }
}

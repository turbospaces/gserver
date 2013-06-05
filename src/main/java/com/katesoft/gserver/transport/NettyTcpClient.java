package com.katesoft.gserver.transport;

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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.Closeable;

import com.google.common.net.HostAndPort;

public class NettyTcpClient implements Runnable, Closeable {
    private final EventLoopGroup eventGroup = new NioEventLoopGroup();
    private final HostAndPort hostAndPort;

    public NettyTcpClient(HostAndPort hostAndPort) {
        this.hostAndPort = hostAndPort;
    }
    @Override
    public void run() {
        Bootstrap b = new Bootstrap();
        b
                .group( eventGroup )
                .channel( NioSocketChannel.class )
                .option( ChannelOption.TCP_NODELAY, true )
                .option( ChannelOption.SO_KEEPALIVE, true )
                .handler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ChannelInboundByteHandlerAdapter() {
                            @Override
                            protected void inboundBufferUpdated(ChannelHandlerContext ctx, ByteBuf in) {}
                        } );
                    }
                } );

        b.connect( hostAndPort.getHostText(), hostAndPort.getPort() ).syncUninterruptibly();
    }
    @Override
    public void close() {
        if ( eventGroup != null ) {
            eventGroup.shutdownGracefully();
        }
    }
}

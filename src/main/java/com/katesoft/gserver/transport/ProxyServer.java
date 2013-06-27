package com.katesoft.gserver.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.ByteLoggingHandler;
import io.netty.handler.logging.LogLevel;

import java.io.Closeable;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.net.HostAndPort;

/**
 * simple utility proxy server that is designed to proxy simple back-end Master-Slave pair processes. currently supports
 * only 1-to-(0,1) relation between master and slaves.
 */
public class ProxyServer implements Closeable, Runnable {
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final EventLoopGroup eventLoop = new NioEventLoopGroup();
    private final HostAndPort localBinding;
    private final Pair<HostAndPort, Optional<HostAndPort>> masterSlave;

    /**
     * create new proxy server instance for given local binding and master-slave pair.
     * 
     * @param localBinding - proxy server bind address.
     * @param masterSlave - master (left) and optionally slave.
     */
    public ProxyServer(HostAndPort localBinding, Pair<HostAndPort, Optional<HostAndPort>> masterSlave) {
        this.localBinding = localBinding;
        this.masterSlave = masterSlave;
    }

    @Override
    public void run() {
        ServerBootstrap lb = new ServerBootstrap();
        try {
            lb.group( eventLoop, eventLoop ).channel( NioServerSocketChannel.class ).childHandler( new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast( new ByteLoggingHandler( LogLevel.INFO ), new ChannelInboundByteHandlerAdapter() {
                        private Channel outboundChannel;

                        @Override
                        public void channelActive(ChannelHandlerContext ctx) {
                            final Channel inboundChannel = ctx.channel();
                            connect( inboundChannel, masterSlave.getLeft(), new Runnable() {
                                @Override
                                public void run() {
                                    if ( masterSlave.getRight().isPresent() ) {
                                        connect( inboundChannel, masterSlave.getRight().get(), new Runnable() {
                                            @Override
                                            public void run() {
                                                closeOnFlush( inboundChannel );
                                            }
                                        } );
                                    }
                                    else {
                                        closeOnFlush( inboundChannel );
                                    }
                                }
                            } );
                        }
                        @Override
                        protected void inboundBufferUpdated(final ChannelHandlerContext ctx, ByteBuf in) {
                            ByteBuf out = outboundChannel.outboundByteBuffer();
                            out.writeBytes( in );
                            if ( outboundChannel.isActive() ) {
                                outboundChannel.flush().addListener( new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture future) {
                                        if ( future.isSuccess() ) {
                                            ctx.channel().read();
                                        }
                                        else {
                                            future.channel().close();
                                        }
                                    }
                                } );
                            }
                        }
                        @Override
                        public void channelInactive(ChannelHandlerContext ctx) {
                            if ( outboundChannel != null ) {
                                closeOnFlush( outboundChannel );
                            }
                        }
                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                            logger.error( cause.getMessage(), cause );
                            closeOnFlush( ctx.channel() );
                        }
                        private void closeOnFlush(Channel channel) {
                            if ( channel.isActive() ) {
                                channel.flush().addListener( ChannelFutureListener.CLOSE );
                            }
                        }
                        private void connect(final Channel inboundChannel, final HostAndPort hp, final Runnable r) {
                            Bootstrap rb = new Bootstrap();
                            rb.group( inboundChannel.eventLoop() ).channel( NioSocketChannel.class ).handler( new ChannelInboundByteHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext c) {
                                    c.read();
                                    c.flush();
                                }
                                @Override
                                public void channelInactive(ChannelHandlerContext c) {
                                    logger.error( "Proxied channel closed={}, trying to failover...", c.channel() );
                                    r.run();
                                }
                                @Override
                                public void exceptionCaught(ChannelHandlerContext c, Throwable cause) {
                                    logger.error( cause.getMessage(), cause );
                                    closeOnFlush( c.channel() );
                                }
                                @Override
                                protected void inboundBufferUpdated(final ChannelHandlerContext c, ByteBuf in) {
                                    ByteBuf out = inboundChannel.outboundByteBuffer();
                                    out.writeBytes( in );
                                    inboundChannel.flush().addListener( new ChannelFutureListener() {
                                        @Override
                                        public void operationComplete(ChannelFuture future) {
                                            if ( future.isSuccess() ) {
                                                c.channel().read();
                                            }
                                            else {
                                                future.channel().close();
                                            }
                                        }
                                    } );
                                }
                            } ).option( ChannelOption.AUTO_READ, false );
                            ChannelFuture f = rb.connect( hp.getHostText(), hp.getPort() );
                            outboundChannel = f.channel();
                            f.addListener( new ChannelFutureListener() {
                                @Override
                                public void operationComplete(ChannelFuture future) {
                                    if ( future.isSuccess() ) {
                                        inboundChannel.read();
                                        logger.info( "Added node={} to load balancing cycle", hp );
                                    }
                                    else {
                                        inboundChannel.close();
                                    }
                                }
                            } );
                        }
                    } );
                }
            } ).childOption( ChannelOption.AUTO_READ, false ).bind( localBinding.getHostText(), localBinding.getPort() ).sync();
        }
        catch ( InterruptedException e ) {
            logger.error( e.getMessage(), e );
            Throwables.propagate( e );
        }
        logger.info( "started proxy server on={}", localBinding );
    }
    public HostAndPort getProxyAddress() {
        return localBinding;
    }
    @Override
    public void close() {
        eventLoop.shutdownGracefully();
    }
}

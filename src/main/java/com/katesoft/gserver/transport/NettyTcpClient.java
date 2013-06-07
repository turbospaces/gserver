package com.katesoft.gserver.transport;

import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.Closeable;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.MessageHeaders;
import com.katesoft.gserver.core.Commands;
import com.katesoft.gserver.core.CommandsQualifierCodec;

public class NettyTcpClient implements Runnable, Closeable, Supplier<SocketChannel> {
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    private final AtomicLong seq = new AtomicLong();
    private final ConcurrentMap<String, SettableFuture<BaseCommand>> corr = Maps.newConcurrentMap();
    private final CommandsQualifierCodec commandsCodec = CommandsQualifierCodec.DEFAULT.get();
    private final EventLoopGroup eventGroup = new NioEventLoopGroup();
    private final HostAndPort hostAndPort;
    private SocketChannel sch;
    private ChannelHandlerContext channelCtx;

    public NettyTcpClient(HostAndPort hostAndPort) {
        this.hostAndPort = hostAndPort;
    }
    @Override
    public void run() {
        final ExtensionRegistry registry = Commands.newMessageRegistry();
        final CountDownLatch l = new CountDownLatch( 1 );
        final Bootstrap b = new Bootstrap();
        
        b
                .group( eventGroup )
                .channel( NioSocketChannel.class )
                .option( ChannelOption.TCP_NODELAY, true )
                .option( ChannelOption.SO_KEEPALIVE, true )
                .handler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        NettyServer.registerProtobufCodecs( p, registry );
                        p.addLast( new ChannelInboundMessageHandlerAdapter<BaseCommand>() {

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) {
                                NettyTcpClient.this.channelCtx = ctx;
                                sch = (SocketChannel) ctx.pipeline().channel();
                                logger.debug( "NettyTcpClient connected via={}", sch );
                                l.countDown();
                            }
                            @Override
                            public void messageReceived(ChannelHandlerContext ctx, BaseCommand cmd) {
                                SettableFuture<BaseCommand> f = corr.remove( cmd.getHeaders().getCorrelationID() );
                                if ( f != null ) {
                                    f.set( cmd );
                                }
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <Type> ListenableFuture<BaseCommand> callAsync(GeneratedMessage.GeneratedExtension<BaseCommand, Type> extension, Type t,
                                                          Optional<LoginCommand> loginCommand) {
        BaseCommand.Builder cmd = BaseCommand.newBuilder().setExtension( extension, t );
        commandsCodec.qualifierWriter().apply(new AbstractMap.SimpleEntry(cmd, t));

        long seqN = seq.incrementAndGet();
        MessageHeaders headers = MessageHeaders
                .newBuilder()
                .setCorrelationID( getClass().getSimpleName() + ":" + seqN )
                .setMessageTimestamp( System.currentTimeMillis() )
                .setSequenceNumber( seqN )
                .build();
        cmd.setHeaders( headers ).setProtocolVersion( "1.0" );
        if ( loginCommand.isPresent() ) {
            cmd.setSessionId( loginCommand.get().getSessionId() );
        }

        SettableFuture<BaseCommand> f = SettableFuture.create();
        corr.put( headers.getCorrelationID(), f );

        try {
            channelCtx.write( cmd.build() );
            channelCtx.flush();
        }
        catch ( Throwable e ) {
            logger.error( e.getMessage(), e );
            Throwables.propagate( e );

            corr.remove( headers.getCorrelationID() );
        }

        return f;
    }
    @Override
    public void close() {
        if ( eventGroup != null ) {
            eventGroup.shutdownGracefully();
        }
    }
}

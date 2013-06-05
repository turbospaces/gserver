package com.katesoft.gserver.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;

import java.io.Closeable;

import org.jasypt.util.text.BasicTextEncryptor;

import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.katesoft.gserver.core.UserConnection;
import com.katesoft.gserver.misc.Misc;

class RootHandler extends ChannelInboundByteHandlerAdapter implements Closeable, Supplier<ChannelGroup> {
    private static AttributeKey<SocketUserConnection> USER_CONNECTION_ATTR = new AttributeKey<SocketUserConnection>( "x-user-connection" );

    private final ChannelGroup connections = new DefaultChannelGroup();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        SocketChannel ch = (SocketChannel) ctx.pipeline().channel();
        SocketUserConnection uc = new SocketUserConnection( ch );
        ch.attr( USER_CONNECTION_ATTR ).set( uc );
        connections.add( ch );
    }
    @Override
    protected void inboundBufferUpdated(ChannelHandlerContext ctx, ByteBuf in) {
        ByteBuf out = ctx.nextOutboundByteBuffer();
        out.writeBytes( in );
        ctx.flush();
    }
    @Override
    public void close() {
        connections.close();
    }
    @Override
    public ChannelGroup get() {
        return connections;
    }
    UserConnection find(String id) {
        Integer x = SocketUserConnection.decodeId( id );
        Channel ch = connections.find( x );
        return ch.attr( USER_CONNECTION_ATTR ).get();
    }
    UserConnection find(SocketChannel sch) {
        Channel ch = connections.find( sch.id() );
        return ch.attr( USER_CONNECTION_ATTR ).get();
    }

    static final class SocketUserConnection implements UserConnection {
        private static final BasicTextEncryptor ENCRYPTOR = new BasicTextEncryptor();
        static {
            ENCRYPTOR.setPassword( SocketUserConnection.class.getSimpleName() );
        }
        private final String id;

        public SocketUserConnection(SocketChannel ch) {
            this.id = encodeId( ch );
        }
        @Override
        public String id() {
            return id;
        }
        @Override
        public String toString() {
            return Objects.toStringHelper( this ).add( "id", id() ).toString();
        }
        private static String encodeId(SocketChannel ch) {
            /**
             * 1. timestamp
             * 2. netty socket connection id
             * 3. process id
             */
            String id = System.currentTimeMillis() + ":" + ch.id() + ":" + Misc.getPid();
            return ENCRYPTOR.encrypt( id );
        }
        private static Integer decodeId(String id) {
            String[] items = ENCRYPTOR.decrypt( id ).split( ":" );
            return Integer.parseInt( items[1] );
        }
    }
}

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

import com.katesoft.gserver.core.UserConnection;
import com.katesoft.gserver.misc.Misc;

class RootHandler extends ChannelInboundByteHandlerAdapter implements Closeable {
	private static AttributeKey<SocketUserConnection> USER_CONNECTION_ATTR = new AttributeKey<SocketUserConnection>("x-user-connection");

	final ChannelGroup connections = new DefaultChannelGroup();

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		SocketChannel ch = (SocketChannel) ctx.pipeline().channel();
		connections.add(ch);
		SocketUserConnection uc = new SocketUserConnection(ch);
		ch.attr(USER_CONNECTION_ATTR).set(uc);
	}

	@Override
	protected void inboundBufferUpdated(ChannelHandlerContext ctx, ByteBuf in) {
		ByteBuf out = ctx.nextOutboundByteBuffer();
		out.writeBytes(in);
		ctx.flush();
	}
	
    UserConnection find(String id) {
    	Integer x = Integer.valueOf(id.substring(0, id.indexOf(":") ));
    	Channel ch = connections.find(x);
    	return ch.attr(USER_CONNECTION_ATTR).get();
    }

	@Override
	public void close() {
		connections.close();
	}

	static final class SocketUserConnection implements UserConnection {
		private final String id;

		public SocketUserConnection(SocketChannel ch) {
			this.id = ch.id() + ":" + Misc.UNIQUE_PREFIX;
		}

		@Override
		public String id() {
			return id;
		}
	}
}

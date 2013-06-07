package com.katesoft.gserver.api;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractMap;
import java.util.UUID;

import javax.annotation.concurrent.Immutable;

import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;
import com.katesoft.gserver.commands.Commands.MessageHeaders;
import com.katesoft.gserver.core.CommandsQualifierCodec;

@Immutable
public class CommandWrapperEvent {
	private BaseCommand cmd;
	private CommandsQualifierCodec codec;

	public CommandWrapperEvent(BaseCommand cmd, CommandsQualifierCodec codec) {
		this.cmd = checkNotNull(cmd);
		this.codec = checkNotNull(codec);
	}

	public Class<? extends GeneratedMessage> cmdClass() {
		return codec.qualifierToType().apply(cmd.getQualifier());
	}

	public BaseCommand getCmd() {
		return cmd;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> CommandWrapperEvent mock(GeneratedMessage.GeneratedExtension<BaseCommand, T> extension, T t) {
		long tmstmp = System.currentTimeMillis();
		CommandsQualifierCodec qualifierCodec = CommandsQualifierCodec.DEFAULT.get();
		
		MessageHeaders headers = MessageHeaders.newBuilder()
				.setCorrelationID(UUID.randomUUID().toString())
				.setMessageTimestamp(tmstmp).setSequenceNumber((short) tmstmp)
				.build();
		
		Builder b = BaseCommand.newBuilder().setProtocolVersion("1.0").setExtension(extension, t).setHeaders(headers);
		return new CommandWrapperEvent(qualifierCodec.qualifierWriter().apply(new AbstractMap.SimpleEntry(b, t)).build(), qualifierCodec);
	}
}

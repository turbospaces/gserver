package com.katesoft.gserver.core;

import static java.lang.System.currentTimeMillis;

import java.util.AbstractMap;
import java.util.UUID;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.api.CommandWrapperEvent;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;
import com.katesoft.gserver.commands.Commands.MessageHeaders;
import com.katesoft.gserver.games.roullete.RoulleteCommands;

public abstract class Commands {
    public static ExtensionRegistry newMessageRegistry() {
        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        com.katesoft.gserver.commands.Commands.registerAllExtensions(registry);
        RoulleteCommands.registerAllExtensions(registry);
        return registry;
    }
	public static <T> BaseCommand toReply(CommandWrapperEvent e,
			GeneratedMessage.GeneratedExtension<BaseCommand, T> extension,
			T reply) {
		return toReply(e.getCmd(), e.getCodec(), extension, reply);
	}
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> BaseCommand toReply(BaseCommand cmd,
                                          CommandsQualifierCodec codec,
                                          GeneratedMessage.GeneratedExtension<BaseCommand, T> extension,
                                          T reply) {
        Builder builder =
                BaseCommand.newBuilder().setProtocolVersion(cmd.getProtocolVersion())
                        .setHeaders(cmd.getHeaders().toBuilder().setMessageTimestamp(currentTimeMillis()).build());
        builder.setExtension(extension, reply);
        return codec.qualifierWriter().apply(new AbstractMap.SimpleEntry(builder, reply)).build();
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> CommandWrapperEvent mockCommandEvent(GeneratedMessage.GeneratedExtension<BaseCommand, T> extension, T t, PlayerSession playerSession) {
        long tmstmp = System.currentTimeMillis();
        CommandsQualifierCodec qualifierCodec = CommandsQualifierCodec.DEFAULT.get();

        MessageHeaders headers =
                MessageHeaders.newBuilder().setCorrelationID(UUID.randomUUID().toString()).setMessageTimestamp(tmstmp)
                        .setSequenceNumber((short) tmstmp).build();

        Builder b = BaseCommand.newBuilder().setProtocolVersion("1.0").setExtension(extension, t).setHeaders(headers);
        b = qualifierCodec.qualifierWriter().apply(new AbstractMap.SimpleEntry(b, t));

        return new CommandWrapperEvent(b.build(), qualifierCodec, playerSession);
    }
    private Commands() {}
}

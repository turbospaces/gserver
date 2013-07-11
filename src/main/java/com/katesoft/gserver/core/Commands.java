package com.katesoft.gserver.core;

import static java.lang.System.currentTimeMillis;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.katesoft.gserver.api.GameCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;

public abstract class Commands {
    /**
     * create new protobuf extensions registry and fill platform commands inside.
     * 
     * @return protobuf extensions registry.
     */
    public static ExtensionRegistry newMessageRegistry() {
        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        com.katesoft.gserver.commands.Commands.registerAllExtensions( registry );
        return registry;
    }
    public static <T> BaseCommand toReply(GameCommand e, GeneratedExtension<BaseCommand, T> extension, T reply) {
        return toReply( e.getCmd(), e.getCodec(), extension, reply );
    }
    @SuppressWarnings({ "unchecked" })
    public static <T> BaseCommand toReply(BaseCommand cmd, CommandsQualifierCodec codec, GeneratedExtension<BaseCommand, T> extension, T reply) {
        Builder builder = BaseCommand
                .newBuilder()
                .setProtocolVersion( cmd.getProtocolVersion() )
                .setHeaders( cmd.getHeaders().toBuilder().setMessageTimestamp( currentTimeMillis() ).build() );
        builder.setExtension( extension, reply );
        builder.setDebug( cmd.getDebug() );
        return codec.encoder().apply( (Pair<Builder, GeneratedMessage>) ImmutablePair.of( builder, reply ) ).build();
    }
    private Commands() {}
}

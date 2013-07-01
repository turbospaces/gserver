package com.katesoft.gserver.core;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.ExtensionRegistry.ExtensionInfo;
import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;

/**
 * Each command that can be handled by game server contains fully (or shortly) qualified command code and this interface
 * is abstraction around.
 */
public interface CommandsQualifierCodec {
    /**
     * Decode corresponding java class from fully(or shortly) qualified command name.
     * 
     * @return java class
     */
    Function<BaseCommand, Class<? extends GeneratedMessage>> decoder() throws Exception;
    /**
     * Get function which will be applied in order to derive fully(or shortly) qualified name for command. The
     * parameter of function will be pair where <code>left</code> is base command builder and the <code>right</code> is
     * actual command object(protobuf extension).
     * 
     * @return derivation function
     */
    Function<Pair<BaseCommand.Builder, GeneratedMessage>, BaseCommand.Builder> encoder();
    /**
     * @return associated protobuf extension registry.
     */
    ExtensionRegistry extensionRegistry();

    public static final class DefaultCommandsCodec implements CommandsQualifierCodec {
        private final ExtensionRegistry extensionRegistry;
        private final Function<BaseCommand, Class<? extends GeneratedMessage>> from = new Function<BaseCommand, Class<? extends GeneratedMessage>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Class<? extends GeneratedMessage> apply(BaseCommand input) {
                String qualifier = input.getQualifier();
                ExtensionInfo xinfo = extensionRegistry.findExtensionByName( qualifier + ".cmd" );
                if ( xinfo != null ) {
                    return (Class<? extends GeneratedMessage>) xinfo.defaultInstance.getClass();
                }
                return null;
            }
        };
        private final Function<Pair<BaseCommand.Builder, GeneratedMessage>, BaseCommand.Builder> to = new Function<Pair<BaseCommand.Builder, GeneratedMessage>, BaseCommand.Builder>() {
            @Override
            public Builder apply(@Nullable Pair<BaseCommand.Builder, GeneratedMessage> input) {
                String qualifier = input.getRight().getDescriptorForType().getFullName();
                return input.getLeft().setQualifier( qualifier );
            }
        };

        public DefaultCommandsCodec(ExtensionRegistry extensionRegistry) {
            this.extensionRegistry = extensionRegistry;
        }
        @Override
        public Function<BaseCommand, Class<? extends GeneratedMessage>> decoder() {
            return from;
        }
        @Override
        public Function<Pair<Builder, GeneratedMessage>, Builder> encoder() {
            return to;
        }
        @Override
        public ExtensionRegistry extensionRegistry() {
            return extensionRegistry;
        }
    }
}

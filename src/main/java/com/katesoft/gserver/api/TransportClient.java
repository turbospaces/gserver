package com.katesoft.gserver.api;

import java.io.Closeable;

import com.google.common.base.Supplier;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.core.CommandsQualifierCodec;

public interface TransportClient<ChannelType> extends Closeable, Supplier<ChannelType> {
    <T> ListenableFuture<BaseCommand> callAsync(GeneratedExtension<BaseCommand, T> ext, T t, String sessionId, boolean debug);
    void setCommandsQualifierCodec(CommandsQualifierCodec commandsCodec);
}

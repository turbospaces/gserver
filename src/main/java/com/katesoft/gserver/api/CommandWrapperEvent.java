package com.katesoft.gserver.api;

import static com.google.common.base.Preconditions.checkNotNull;
import io.netty.util.concurrent.Future;

import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.core.CommandsQualifierCodec;

public class CommandWrapperEvent {
    private final BaseCommand cmd;
    private final CommandsQualifierCodec codec;
    private final PlayerSession playerSession;
    private boolean interpreted;

    public CommandWrapperEvent(BaseCommand cmd, CommandsQualifierCodec codec, PlayerSession playerSession) {
        this.playerSession = playerSession;
        this.cmd = checkNotNull(cmd);
        this.codec = checkNotNull(codec);
    }
    public Class<? extends GeneratedMessage> cmdClass() {
        return codec.qualifierToType().apply(cmd.getQualifier());
    }
    public BaseCommand getCmd() {
        return cmd;
    }
    public BaseCommand getCmdForInterpretation() {
        interpreted = true;
        return getCmd();
    }
    public boolean isInterpreted() {
        return interpreted;
    }
    public Future<Void> replyAsync(GeneratedMessage reply) {
        return playerSession.getAssociatedUserConnection().writeAsync(reply);
    }
    public void reply(GeneratedMessage reply) {
        playerSession.getAssociatedUserConnection().writeSync(reply);
    }
}

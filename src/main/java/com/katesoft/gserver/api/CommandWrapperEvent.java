package com.katesoft.gserver.api;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Future;

import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.UnknownCommadException;
import com.katesoft.gserver.core.CommandsQualifierCodec;

public class CommandWrapperEvent {
    private final BaseCommand cmd;
    private final CommandsQualifierCodec codec;
    private final PlayerSession playerSession;
    private boolean acknowledged;

    public CommandWrapperEvent(BaseCommand cmd, CommandsQualifierCodec codec, PlayerSession playerSession) {
        this.playerSession = playerSession;
        this.cmd = checkNotNull( cmd );
        this.codec = checkNotNull( codec );
    }
    /**
     * @return class of command(translate from the qualifier via codec).
     */
    public Class<? extends GeneratedMessage> cmdClass() {
        return codec.qualifierToType().apply( cmd.getQualifier() );
    }
    /**
     * @return command itself wrapped into base command, you would need to get
     *         actual request from the extension.
     */
    public BaseCommand getCmd() {
        return cmd;
    }
    /**
     * @return codec used for qualifier to class translation.
     */
    public CommandsQualifierCodec getCodec() {
        return codec;
    }
    /**
     * manually acknowledge the reception of the message. In most cases you
     * would not need to call this method at all since it will be called as part
     * of {@link #replyAsyncAndAcknowledge(BaseCommand)} and {@link #replySyncAndAcknowledge(BaseCommand)}, but it would
     * be
     * required to manually acknowledge the reception of message in case when no
     * reply expected/required (this is needed for system in order to properly
     * react for unhandled messages and reply with {@link UnknownCommadException}).
     */
    public void acknowledge() {
        this.acknowledged = true;
    }
    public boolean isAcknowledged() {
        return acknowledged;
    }
    public Future<Void> replyAsyncAndAcknowledge(BaseCommand reply) {
        try {
            return playerSession.getAssociatedUserConnection().writeAsync( reply );
        }
        finally {
            acknowledge();
        }
    }
    public void replySyncAndAcknowledge(BaseCommand reply) {
        try {
            playerSession.getAssociatedUserConnection().writeSync( reply );
        }
        finally {
            acknowledge();
        }
    }
}

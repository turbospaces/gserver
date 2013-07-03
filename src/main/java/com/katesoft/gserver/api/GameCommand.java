package com.katesoft.gserver.api;

import java.util.concurrent.Future;

import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.UnknownCommadException;
import com.katesoft.gserver.core.CommandsQualifierCodec;
import com.katesoft.gserver.core.NetworkCommandContext;

public final class GameCommand {
    private final PlayerSession playerSession;
    private final NetworkCommandContext ctx;
    private boolean acknowledged;

    public GameCommand(NetworkCommandContext ctx, PlayerSession playerSession) {
        this.ctx = ctx;
        this.playerSession = playerSession;
    }
    public void interpretIfPossible(Class<? extends GeneratedMessage> clazz, Runnable r) throws Exception {
        if ( cmdClass() == clazz ) {
            try {
                r.run();
            }
            finally {
                acknowledge();
            }
        }
    }
    /**
     * @return class of command(translate from the qualifier via codec).
     * @throws Exception if the target class can't be decoded from message by qualifier.
     */
    public Class<? extends GeneratedMessage> cmdClass() throws Exception {
        return ctx.getCmdCodec().decoder().apply( getCmd() );
    }
    /**
     * @return command itself wrapped into base command, you would need to get
     *         actual request from the extension.
     */
    public BaseCommand getCmd() {
        return ctx.getCmd();
    }
    /**
     * @return codec used for qualifier to class translation.
     */
    public CommandsQualifierCodec getCodec() {
        return ctx.getCmdCodec();
    }
    /**
     * manually acknowledge the reception of the message. In most cases you
     * would not need to call this method at all since it will be called as part
     * of {@link #replyAsync(BaseCommand)} and {@link #replySync(BaseCommand)}, but it would
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
    public Future<Void> replyAsync(BaseCommand reply) {
        try {
            return playerSession.getAssociatedUserConnection().writeAsync( reply );
        }
        finally {
            acknowledge();
        }
    }
    public void replySync(BaseCommand reply) {
        try {
            playerSession.getAssociatedUserConnection().writeSync( reply );
        }
        finally {
            acknowledge();
        }
    }
}

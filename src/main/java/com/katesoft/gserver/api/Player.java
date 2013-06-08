package com.katesoft.gserver.api;

import java.io.Closeable;

import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.core.CommandsQualifierCodec;

public interface Player extends Closeable {
    String id();
    String displayName();
    String email();
    boolean addPlayerSession(PlayerSession s);
    /**
     * close all player session gracefully(method called upon user logout command).
     */
    @Override
    void close();
    /**
     * send the command to the player session(s) for interpretation.
     * 
     * @param cmd - network command event.
     * @param codec - command qualifier codec.
     * @return whether command has been interpreted by at least one game(player session).
     */
    boolean dispatchCommand(final BaseCommand cmd, CommandsQualifierCodec codec);
}

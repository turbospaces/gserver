package com.katesoft.gserver.api;

import java.io.Closeable;

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
     * @return whether command has been interpreted by at least one game(player session).
     */
    boolean dispatchCommand(CommandWrapperEvent cmd);
}

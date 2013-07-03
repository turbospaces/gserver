package com.katesoft.gserver.api;

import java.io.Closeable;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

public interface Player extends Closeable, Command {
    String id();
    String displayName();
    String email();
    boolean addPlayerSession(PlayerSession s);
    /**
     * close all player sessions gracefully(method called upon user logout command).
     */
    @Override
    void close();
    /**
     * close specific user session by ID.
     * 
     * @param sessionId - player session ID.
     */
    void closePlayerSession(String sessionId);
    @Override
    boolean execute(Context context);
}

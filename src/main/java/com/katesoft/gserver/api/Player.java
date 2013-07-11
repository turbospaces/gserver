package com.katesoft.gserver.api;

import java.io.Closeable;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import com.katesoft.gserver.domain.BO;

public interface Player extends Closeable, Command, BO {
    String displayName();
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

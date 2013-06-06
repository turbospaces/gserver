package com.katesoft.gserver.api;

import java.io.Closeable;

import com.katesoft.gserver.commands.Commands.BaseCommand;

public interface Player extends Closeable {
    String id();
    String displayName();
    String email();
    void addPlayerSession(PlayerSession s);
    /**
     * close all player session gracefully(method called upon user logout command).
     */
    @Override
    void close();
    void dispatchCommand(BaseCommand cmd);
}

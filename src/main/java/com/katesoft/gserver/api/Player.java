package com.katesoft.gserver.api;

import java.io.Closeable;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coins;
import com.katesoft.gserver.domain.GameBO;

public interface Player extends Closeable, Command {
    String userId();
    String displayName();
    PlayerSession openPlayerSession(String sessionId,
                                    UserConnection uc,
                                    Game game,
                                    GameBO gameBO,
                                    BetLimits blimits,
                                    Coins coins);
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

package com.katesoft.gserver.api;

import java.io.Closeable;

public interface Game extends Closeable {
    void init(GamePlayContext ctx);
    String displayName();
    GameCommandsInterpreter commandsInterpreter();
    /**
     * close game on logout command. no further commands can't be accepted by game instance.
     */
    @Override
    void close();
}

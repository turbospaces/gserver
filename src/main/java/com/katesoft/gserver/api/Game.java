package com.katesoft.gserver.api;

import java.io.Closeable;

public interface Game extends Closeable {
    void init(GamePlayContext ctx);
    GameCommandInterpreter commandInterpreter();
    /**
     * close game on logout command (typically means that user closed game play for particular game).
     */
    @Override
    void close();
}

package com.katesoft.gserver.api;

public interface GameCommandInterpreter {
    void interpretCommand(GameCommand cmd) throws Exception;
}

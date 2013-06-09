package com.katesoft.gserver.api;

public interface GameCommandsInterpreter {
    void interpretCommand(CommandWrapperEvent cmd) throws Exception;
}

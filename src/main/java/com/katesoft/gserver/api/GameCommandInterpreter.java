package com.katesoft.gserver.api;

public interface GameCommandInterpreter {
    void interpretCommand(CommandWrapperEvent cmd) throws Exception;
}

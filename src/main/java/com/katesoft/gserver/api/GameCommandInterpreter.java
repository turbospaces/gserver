package com.katesoft.gserver.api;

import com.katesoft.gserver.commands.Commands.BaseCommand;

public interface GameCommandInterpreter {
    void interpretCommand(BaseCommand command, PlayerSession session) throws Exception;
}

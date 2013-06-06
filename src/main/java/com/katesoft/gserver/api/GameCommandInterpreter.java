package com.katesoft.gserver.api;

public interface GameCommandInterpreter {
	void interpretCommand(Object command) throws Exception;
}

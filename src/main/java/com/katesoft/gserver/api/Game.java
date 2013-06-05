package com.katesoft.gserver.api;

public interface Game {
    String id();
    String displayName();
    GameCommandInterpreter getGameCommandInterpreter();
}

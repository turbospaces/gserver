package com.katesoft.gserver.api;

public interface Game {
    void init(GamePlayContext ctx);
    String id();
    String displayName();
    GameCommandInterpreter getGameCommandInterpreter();

    public static abstract class AbstractBlankGame implements Game {
        protected GamePlayContext gamePlayContext;
        protected GameCommandInterpreter interpreter;
        private final String id;

        public AbstractBlankGame(String id) {
            this.id = id;
        }
        @Override
        public void init(GamePlayContext ctx) {
            this.gamePlayContext = ctx;
        }
        @Override
        public String id() {
            return id;
        }
        @Override
        public String displayName() {
            return getClass().getSimpleName();
        }
        @Override
        public GameCommandInterpreter getGameCommandInterpreter() {
            return interpreter;
        }
    }
}

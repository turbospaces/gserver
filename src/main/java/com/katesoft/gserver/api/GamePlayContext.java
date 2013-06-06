package com.katesoft.gserver.api;

import java.util.Random;


public interface GamePlayContext {
    Random rng();
    void creditWin(BetWrapper bet);
}

package com.katesoft.gserver.api;

import java.util.Random;

import com.katesoft.gserver.core.BetWrapper;

public interface GamePlayContext {
    Random rng();
    void creditWin(BetWrapper bet);
}

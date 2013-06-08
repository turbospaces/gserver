package com.katesoft.gserver.api;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.katesoft.gserver.misc.Misc;

public interface GamePlayContext {
    Random rng();
    void creditWin(BetWrapper bet);

    public static abstract class AbstractGamePlayContext implements GamePlayContext {
        private final SecureRandom rng = (SecureRandom) Misc.RANDOM;
        @Override
        public Random rng() {
            return rng;
        }        
        @Override
        public void creditWin(BetWrapper bet) {}
    }

    public static class RTP extends AbstractGamePlayContext {
        private final AtomicInteger wins = new AtomicInteger();
        private final AtomicInteger loses = new AtomicInteger();
        private final AtomicLong winsAmount = new AtomicLong();
        private final AtomicLong loseAmount = new AtomicLong();

        @Override
        public void creditWin(BetWrapper bet) {
            if ( bet.isWin() ) {
                wins.incrementAndGet();
                winsAmount.addAndGet(bet.betAmountUnsigned());
            }
            else {
                loses.incrementAndGet();
                loseAmount.addAndGet(bet.betAmountUnsigned());
            }
        }
        public long totalWin() {
            long l1 = winsAmount.get(), l2 = loseAmount.get();
            for ( ;; ) {
                long r = winsAmount.get() - loseAmount.get();
                if ( l1 == winsAmount.get() && l2 == loseAmount.get() ) {
                    return r;
                }
            }
        }
        public int winsCount() {
            return wins.get();
        }
        public int losesCount() {
            return loses.get();
        }
        public double payout() {
            int i1 = winsCount(), i2 = losesCount();
            for ( ;; ) {
                double r = losesCount() / winsCount();
                if ( i1 == winsCount() && i2 == losesCount() ) {
                    return r;
                }
            }
        }
    };
}

package com.katesoft.gserver.api;

import java.security.SecureRandom;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface GamePlayContext {
    Random rng();
    void creditWin(BetWrapper bet);

    GamePlayContext MOCK = new GamePlayContext() {
        private final Logger logger = LoggerFactory.getLogger( GamePlayContext.class );
        private final SecureRandom rng = new SecureRandom();

        @Override
        public Random rng() {
            return rng;
        }
        @Override
        public void creditWin(BetWrapper bet) {
            logger.trace( "creditWin(:)={}", bet );
        }
    };

    public static class RTP implements GamePlayContext {
        private final SecureRandom rng = new SecureRandom();

        private int wins = 0;
        private int loses = 0;
        private long winsAmount = 0;
        private long loseAmount = 0;

        @Override
        public Random rng() {
            return rng;
        }
        @Override
        public void creditWin(BetWrapper bet) {
            synchronized ( this ) {
                if ( bet.isWin() ) {
                    wins++;
                    winsAmount += bet.betAmountUnsigned();
                }
                else {
                    loses++;
                    loseAmount += bet.betAmountUnsigned();
                }
            }
        }
        public long totalAbsoluteAmount(boolean winsOtherwiseLose) {
            synchronized ( this ) {
                return winsOtherwiseLose ? winsAmount : loseAmount;
            }
        }
        public long totalWin() {
            synchronized ( this ) {
                return winsAmount - loseAmount;
            }
        }
        public int totalCount(boolean winsOtherwiseLose) {
            synchronized ( this ) {
                return winsOtherwiseLose ? wins : loses;
            }
        }
    };
}

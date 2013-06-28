package com.katesoft.gserver.api;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.katesoft.gserver.misc.Misc;

public interface GamePlayContext {
    Random rng();
    void creditWin(BetWrapper bet);
    ScheduledFuture<?> schedule(Runnable r, long period, TimeUnit timeUnit);

    //
    //
    // abstract implementation in place.
    //
    //
    public static abstract class AbstractGamePlayContext implements GamePlayContext {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        private final Random rng;
        private final ScheduledExecutorService scheduledExec;

        public AbstractGamePlayContext(ScheduledExecutorService exec, Random rng) {
            this.scheduledExec = exec;
            this.rng = rng;
        }
        @Override
        public Random rng() {
            return rng;
        }
        @Override
        public ScheduledFuture<?> schedule(final Runnable r, final long period, final TimeUnit timeUnit) {
            return scheduledExec.scheduleAtFixedRate( new Runnable() {
                @Override
                public void run() {
                    try {
                        r.run();
                    }
                    catch ( Throwable t ) {
                        logger.error( t.getMessage(), t );
                    }
                }
            }, 0, period, timeUnit );
        }
        @Override
        public void creditWin(BetWrapper bet) {}
    }

    //
    //
    // calculate return to player statistics.
    //
    //
    public static class RTP extends AbstractGamePlayContext {
        private final AtomicInteger wins = new AtomicInteger();
        private final AtomicInteger loses = new AtomicInteger();
        private final AtomicLong winAmount = new AtomicLong();
        private final AtomicLong loseAmount = new AtomicLong();

        public RTP(ScheduledExecutorService scheduledExec) {
            super( scheduledExec, Misc.RANDOM );
        }
        @Override
        public void creditWin(BetWrapper bet) {
            if ( bet.isWin() ) {
                wins.incrementAndGet();
                winAmount.addAndGet( bet.betAmountUnsigned() );
            }
            else {
                loses.incrementAndGet();
                loseAmount.addAndGet( bet.betAmountUnsigned() );
            }
        }
        public long totalWin() {
            long l1 = winAmount.get(), l2 = loseAmount.get();
            for ( ;; ) {
                long r = winAmount.get() - loseAmount.get();
                if ( l1 == winAmount.get() && l2 == loseAmount.get() ) {
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
    }
}

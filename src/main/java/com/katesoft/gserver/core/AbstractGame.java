package com.katesoft.gserver.core;

import static com.google.common.base.Suppliers.memoize;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.katesoft.gserver.api.BetWrapper;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GameCommandInterpreter;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.domain.GameBO;

public abstract class AbstractGame implements Game {
    protected transient GamePlayContext gamePlayContext;
    protected transient GameCommandInterpreter interpreter;
    protected transient GameBO definition;
    private transient final Supplier<List<ScheduledFuture<?>>> scheduledTasks = memoize( new Supplier<List<ScheduledFuture<?>>>() {
        @Override
        public List<ScheduledFuture<?>> get() {
            return Lists.newLinkedList();
        }
    } );

    @Override
    public void init(final GamePlayContext ctx) {
        this.gamePlayContext = new GamePlayContext() {
            @Override
            public Random rng() {
                return ctx.rng();
            }
            @Override
            public void creditWin(BetWrapper bet) {
                ctx.creditWin( bet );
            }
            @Override
            public ScheduledFuture<?> schedule(Runnable r, long period, TimeUnit timeUnit) {
                synchronized ( this ) {
                    ScheduledFuture<?> task = ctx.schedule( r, period, timeUnit );
                    scheduledTasks.get().add( task );
                    return task;
                }
            }
        };
    }
    @Override
    public GameCommandInterpreter commandInterpreter() {
        return interpreter;
    }
    @Override
    public synchronized void close() {
        for ( ScheduledFuture<?> task : scheduledTasks.get() ) {
            if ( !task.isCancelled() ) {
                task.cancel( false );
            }
        }
        scheduledTasks.get().clear();
    }
}

package com.katesoft.gserver.core;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.katesoft.gserver.api.BetWrapper;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GameCommandsInterpreter;
import com.katesoft.gserver.api.GamePlayContext;

public abstract class AbstractGame implements Game {
    protected transient GamePlayContext gamePlayContext;
    protected transient GameCommandsInterpreter interpreter;
    private transient final Supplier<List<ScheduledFuture<?>>> scheduledTasks = Suppliers.memoize(new Supplier<List<ScheduledFuture<?>>>() {
        @Override
        public List<ScheduledFuture<?>> get() {
            return Lists.newLinkedList();
        }
    });

    @Override
    public void init(final GamePlayContext ctx) {
        this.gamePlayContext = new GamePlayContext() {
            @Override
            public Random rng() {
                return ctx.rng();
            }
            @Override
            public void creditWin(BetWrapper bet) {
                ctx.creditWin(bet);
            }
            @Override
            public ScheduledFuture<?> schedule(Runnable r,
                                               long period,
                                               TimeUnit timeUnit) {
                synchronized ( this ) {
                    ScheduledFuture<?> task = ctx.schedule(r, period, timeUnit);
                    scheduledTasks.get().add(task);
                    return task;
                }
            }
        };
    }
    @Override
    public String displayName() {
        return getClass().getSimpleName();
    }
    @Override
    public GameCommandsInterpreter commandsInterpreter() {
        return interpreter;
    }
    @Override
    public synchronized void close() {
        for ( ScheduledFuture<?> task : scheduledTasks.get() ) {
            if ( !task.isCancelled() ) {
                task.cancel(false);
            }
        }
        scheduledTasks.get().clear();
    }
}
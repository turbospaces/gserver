package com.katesoft.gserver.core;

import java.io.Closeable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;

public interface CommandsQualifierCodec extends Closeable {
    Function<String, Class<? extends GeneratedMessage>> qualifierToType();
    Function<Map.Entry<BaseCommand.Builder, Object>, BaseCommand.Builder> qualifierWriter();

    public static final class DefaultCommansResolver implements CommandsQualifierCodec {
        private final ConcurrentMap<String, Class<?>> cache = Maps.newConcurrentMap();
        private final Function<String, Class<? extends GeneratedMessage>> from = new Function<String, Class<? extends GeneratedMessage>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Class<? extends GeneratedMessage> apply(@Nullable String input) {
                Class<?> clazz = cache.get(input);
                Preconditions.checkNotNull(clazz);
                return (Class<? extends GeneratedMessage>) clazz;
            }
        };
        private final Function<Map.Entry<BaseCommand.Builder, Object>, BaseCommand.Builder> to =
                new Function<Map.Entry<BaseCommand.Builder, Object>, BaseCommand.Builder>() {
                    @Override
                    public Builder apply(@Nullable Map.Entry<BaseCommand.Builder, Object> input) {
                        String sname = input.getValue().getClass().getSimpleName();
                        cache.putIfAbsent(sname, input.getValue().getClass());
                        return input.getKey().setQualifier(sname);
                    }
                };

        public DefaultCommansResolver() {
            try {
                ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
                ImmutableSet<ClassInfo> topLevelClasses = classPath.getTopLevelClasses();
                for ( ClassInfo c : topLevelClasses ) {
                    try {
                        Class<?> load = c.load();
                        if ( GeneratedMessage.class.isAssignableFrom(load) ) {
                            cache.put(load.getSimpleName(), load);
                        }
                    }
                    catch ( NoClassDefFoundError er ) {}
                }
            }
            catch ( Throwable t ) {
                Throwables.propagate(t);
            }
        }
        @Override
        public Function<String, Class<? extends GeneratedMessage>> qualifierToType() {
            return from;
        }
        @Override
        public Function<Entry<Builder, Object>, Builder> qualifierWriter() {
            return to;
        }
        @Override
        public void close() {
            cache.clear();
        }
    }
}

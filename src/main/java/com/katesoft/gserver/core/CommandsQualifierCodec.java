package com.katesoft.gserver.core;

import java.io.Closeable;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;

/**
 * Each command that can be handled by game server contains fully (or shortly) qualified command code and this interface
 * is abstraction around.
 */
public interface CommandsQualifierCodec extends Closeable {
    /**
     * Decode corresponding java class from fully(or shortly) qualified command name.
     * 
     * @return java class
     */
    Function<String, Class<? extends GeneratedMessage>> decodec() throws Exception;
    /**
     * Get function which will be applied in order to derive fully(or shortly) qualified name for command. The
     * parameter of function will be pair where <code>left</code> is base command builder and the <code>right</code> is
     * actual command object(protobuf extension).
     * 
     * @return derivation function
     */
    Function<Pair<BaseCommand.Builder, Object>, BaseCommand.Builder> codec();

    Supplier<CommandsQualifierCodec> DEFAULT = Suppliers.memoize( new Supplier<CommandsQualifierCodec>() {
        @Override
        public CommandsQualifierCodec get() {
            return new DefaultCommandsCodec();
        }
    } );

    public static final class DefaultCommandsCodec implements CommandsQualifierCodec {
        private final ConcurrentMap<String, Class<?>> cache = Maps.newConcurrentMap();
        private final Function<String, Class<? extends GeneratedMessage>> from = new Function<String, Class<? extends GeneratedMessage>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Class<? extends GeneratedMessage> apply(@Nullable String input) {
                Class<?> clazz = cache.get( input );
                Preconditions.checkNotNull( clazz, "Command=%s not registered", input );
                return (Class<? extends GeneratedMessage>) clazz;
            }
        };
        private final Function<Pair<BaseCommand.Builder, Object>, BaseCommand.Builder> to = new Function<Pair<BaseCommand.Builder, Object>, BaseCommand.Builder>() {
            @Override
            public Builder apply(@Nullable Pair<BaseCommand.Builder, Object> input) {
                String sname = input.getValue().getClass().getSimpleName();
                cache.putIfAbsent( sname, input.getValue().getClass() );
                return input.getKey().setQualifier( sname );
            }
        };

        public DefaultCommandsCodec() {
            try {
                ClassPath classPath = ClassPath.from( Thread.currentThread().getContextClassLoader() );
                ImmutableSet<ClassInfo> topLevelClasses = classPath.getTopLevelClasses();
                for ( ClassInfo c : topLevelClasses ) {
                    try {
                        Class<?> load = c.load();
                        if ( GeneratedMessage.class.isAssignableFrom( load ) ) {
                            cache.put( load.getSimpleName(), load );
                        }
                    }
                    catch ( NoClassDefFoundError er ) {}
                }
            }
            catch ( Throwable t ) {
                Throwables.propagate( t );
            }
        }
        @Override
        public Function<String, Class<? extends GeneratedMessage>> decodec() {
            return from;
        }
        @Override
        public Function<Pair<Builder, Object>, Builder> codec() {
            return to;
        }
        @Override
        public void close() {
            cache.clear();
        }
    }
}

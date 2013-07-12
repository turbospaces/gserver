package com.katesoft.gserver.misc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.Uninterruptibles;
import com.sun.jna.LastErrorException;
import com.sun.jna.Platform;

/**
 * Sugar stuff + different kind of high-level adoptions.
 * 
 * @author andrey borisov
 */
public abstract class Misc {
    private static final Logger LOGGER = LoggerFactory.getLogger( Misc.class );
    private static final sun.misc.Unsafe UNSAFE;
    private static final Range<Integer> FREE_PORT_SCAN_RANGE = Range.closed( 1 << 10, 1 << 14 );

    public static final String OS_USER = System.getProperty( "user.name" );
    public static final Random RANDOM = new SecureRandom();

    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField( "theUnsafe" );
            f.setAccessible( true );
            UNSAFE = (sun.misc.Unsafe) f.get( null );
        }
        catch ( Exception e ) {
            throw new Error( e );
        }
    }

    public static boolean isLinux() {
        return Platform.isLinux();
    }
    /**
     * try to disable usage of OS swap if the target platform is linux.
     * back-ported from apache cassandra module.</p>
     * 
     * NOTE: you need to allocate Xmx=Xms during initialization of virtual machine. This method will Lock all pages
     * which are currently mapped into the address space of the process.
     */
    public static void tryDisableLinuxSwap() {
        try {
            CLibrary.INSTANCE.mlockall( CLibrary.MCL_CURRENT );
            LOGGER.info( "linux swap disabled..." );
        }
        catch ( UnsatisfiedLinkError e ) {
            if ( !Platform.isWindows() ) {
                LOGGER.error( e.getMessage(), e );
            }
        }
        catch ( RuntimeException e ) {
            if ( !( e instanceof LastErrorException ) ) {
                throw e;
            }
            if ( ( (LastErrorException) e ).getErrorCode() == CLibrary.ENOMEM && Platform.isLinux() ) {
                LOGGER.error( "Unable to lock JVM memory (ENOMEM)."
                        + " This can result in part of the JVM being swapped out, especially with mmapped I/O enabled."
                        + " Increase RLIMIT_MEMLOCK or run process as root." );
            }
        }
    }
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(final Object source, final Class<T> valueType, final long fieldOffset) {
        Object value = null;

        if ( valueType.isPrimitive() ) {
            if ( valueType == int.class )
                value = UNSAFE.getInt( source, fieldOffset );
            if ( valueType == short.class )
                value = UNSAFE.getShort( source, fieldOffset );
            else if ( valueType == long.class )
                value = UNSAFE.getLong( source, fieldOffset );
            else if ( valueType == float.class )
                value = UNSAFE.getFloat( source, fieldOffset );
            else if ( valueType == double.class )
                value = UNSAFE.getDouble( source, fieldOffset );
            else if ( valueType == char.class )
                value = UNSAFE.getChar( source, fieldOffset );
            else if ( valueType == byte.class )
                value = UNSAFE.getByte( source, fieldOffset );
            else if ( valueType == boolean.class )
                value = UNSAFE.getBoolean( source, fieldOffset );
        }
        else
            value = UNSAFE.getObject( source, fieldOffset );
        return (T) value;
    }
    /**
     * get the field offset for sub-sequence dirty access.
     * 
     * @param f - the field.
     * @return long value offset
     */
    public static long getFieldOffset(Field f) {
        return UNSAFE.objectFieldOffset( f );
    }
    public static <T> void setFieldValueUnsafe(final Object source, final T value, final Class<T> valueType, final long fieldOffset) {
        if ( valueType.isPrimitive() ) {
            if ( valueType == int.class )
                UNSAFE.putInt( source, fieldOffset, ( (Integer) value ).intValue() );
            if ( valueType == short.class )
                UNSAFE.putShort( source, fieldOffset, ( (Short) value ).shortValue() );
            else if ( valueType == long.class )
                UNSAFE.putLong( source, fieldOffset, ( (Long) value ).longValue() );
            else if ( valueType == float.class )
                UNSAFE.putFloat( source, fieldOffset, ( (Float) value ).floatValue() );
            else if ( valueType == double.class )
                UNSAFE.putDouble( source, fieldOffset, ( (Double) value ).doubleValue() );
            else if ( valueType == char.class )
                UNSAFE.putChar( source, fieldOffset, ( (Character) value ).charValue() );
            else if ( valueType == byte.class )
                UNSAFE.putByte( source, fieldOffset, ( (Byte) value ).byteValue() );
            else if ( valueType == boolean.class )
                UNSAFE.putBoolean( source, fieldOffset, ( (Boolean) value ).booleanValue() );
        }
        else
            UNSAFE.putObject( source, fieldOffset, value );
    }
    public static int nextAvailablePort() {
        int port = FREE_PORT_SCAN_RANGE.lowerEndpoint()
                + RANDOM.nextInt( FREE_PORT_SCAN_RANGE.upperEndpoint() - FREE_PORT_SCAN_RANGE.lowerEndpoint() );
        while ( port <= FREE_PORT_SCAN_RANGE.upperEndpoint() ) {
            if ( isPortAvailable( port ) ) {
                LOGGER.debug( "next avail port = {}", port );
                return port;
            }
            port++;
        }
        throw new IllegalStateException( String.format(
                "unable to find free port between %s and %s",
                FREE_PORT_SCAN_RANGE.lowerEndpoint(),
                FREE_PORT_SCAN_RANGE.upperEndpoint() ) );
    }
    /**
     * check whether the application can bind on given port.
     * 
     * @param port bind port
     * @return true if the binding can be done on port.
     */
    public static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket( port );
            ss.setReuseAddress( true );
            ds = new DatagramSocket( port );
            ds.setReuseAddress( true );
            return true;
        }
        catch ( IOException e ) {}
        finally {
            if ( ds != null ) {
                ds.close();
            }
            try {
                if ( ss != null ) {
                    ss.close();
                }
            }
            catch ( IOException e ) {
                Throwables.propagate( e );
            }
        }
        return false;
    }
    /**
     * start new thread and execute client's runnable task. Wait for thread completion. Catch execution exception and
     * return it. If there is not such execution exception, raise {@link AssertionError}.</p>
     * 
     * @param runnable
     *            client's task
     * @return execution exception if any
     */
    public static Exception runAndGetExecutionException(final Runnable runnable) {
        final AtomicReference<Exception> ex = new AtomicReference<Exception>();
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                }
                catch ( Exception e ) {
                    LOGGER.error( e.getMessage(), e );
                    ex.set( e );
                }
            }
        };
        thread.start();
        Uninterruptibles.joinUninterruptibly( thread );
        if ( ex.get() == null )
            throw new AssertionError( "there is no exception!" );
        return ex.get();
    }
    /**
     * repeats the task action totalIterationsCount times concurrently(you provide how many threads and callback
     * function) - this is general purpose utility.</p>
     * 
     * <strong>NOTE :</strong> this method returns all caught exceptions and you should at least use
     * <code>JUnit.Asser.assertTrue(repeateConcurrenlty.size(), 0)</code> or something similar to check that there are
     * no execution errors.
     * 
     * @param threads
     *            number of concurrent threads
     * @param totalIterationsCount
     *            how many times to repeat task execution concurrently
     * @param task
     *            the action which needs to be performed
     * @return all errors from thread's execution
     */
    public static <T> List<Throwable> repeatConcurrently(final int threads, final int totalIterationsCount, final Function<Integer, Object> task) {
        final AtomicInteger atomicLong = new AtomicInteger( totalIterationsCount );
        final CountDownLatch countDownLatch = new CountDownLatch( threads );
        final LinkedList<Throwable> errors = Lists.newLinkedList();
        for ( int j = 0; j < threads; j++ ) {
            Thread thread = new Thread( new Runnable() {
                @Override
                public void run() {
                    try {
                        int l;
                        while ( ( l = atomicLong.decrementAndGet() ) >= 0 ) {
                            try {
                                task.apply( l );
                            }
                            catch ( Throwable e ) {
                                LOGGER.error( e.getMessage(), e );
                                errors.add( e );
                                Throwables.propagate( e );
                            }
                        }
                    }
                    finally {
                        countDownLatch.countDown();
                    }
                }
            } );
            thread.setName( String.format( "RepeateConcurrentlyThread-%s:{%s}", j, task.toString() ) );
            thread.start();
        }
        Uninterruptibles.awaitUninterruptibly( countDownLatch );
        return errors;
    }
    /**
     * repeats the task action totalIterationsCount times concurrently - similar to
     * {@link #repeatConcurrently(int, int, Function)} but you can pass {@link Runnable} instead of {@link Function}.
     * 
     * @param threads
     *            number of concurrent threads
     * @param totalIterationsCount
     *            how many times to repeat task execution concurrently
     * @param task
     *            the action which needs to be performed (runnable task)
     * @return all errors from thread's execution
     * @see #repeatConcurrently(int, int, Function)
     */
    public static <T> List<Throwable> repeatConcurrently(final int threads, final int totalIterationsCount, final Runnable task) {
        return repeatConcurrently( threads, totalIterationsCount, new Function<Integer, Object>() {
            @Override
            public Object apply(final Integer iteration) {
                task.run();
                return this;
            }
        } );
    }
    public static <T> List<Throwable> repeatConcurrently(final int totalIterationsCount, final Runnable task) {
        return repeatConcurrently( Runtime.getRuntime().availableProcessors(), totalIterationsCount, task );
    }
    public static <T> List<Throwable> repeatConcurrently(final int totalIterationsCount, final Function<Integer, Object> task) {
        return repeatConcurrently( Runtime.getRuntime().availableProcessors(), totalIterationsCount, task );
    }
    public static <T> List<Throwable> repeatConcurrently(final Runnable task) {
        return repeatConcurrently( Runtime.getRuntime().availableProcessors(), ( 1 << 16 ), task );
    }
    /**
     * parse the URL in form of ip1:port1,ip2:port2,.... ipN:portN.
     * 
     * @param url the static list of urls.
     * @return more strict parsed and validated host and port configuration.
     */
    public static Collection<HostAndPort> splitURL(String url) {
        List<String> hostAndPorts = Lists.newArrayList( Splitter.on( "," ).omitEmptyStrings().trimResults().split( url ) );
        return Lists.transform( hostAndPorts, new Function<String, HostAndPort>() {
            @Override
            public HostAndPort apply(String hostAndPort) {
                return HostAndPort.fromString( hostAndPort );
            }
        } );
    }
    /**
     * @return process id - works only with SUN JDK.
     */
    public static Long getPid() {
        String procID = ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0];
        return Long.parseLong( procID );
    }
    public static String getJavaOptsAsString() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = runtimeMXBean.getInputArguments();
        return Joiner.on( " " ).join( jvmArgs );
    }
    public static String shortHostname() {
        for ( ;; )
            try {
                String host = java.net.InetAddress.getLocalHost().getHostName();
                int startIndex = host.indexOf( "." );
                if ( startIndex > 0 ) {
                    return host.substring( 0, startIndex );
                }
                else {
                    return host;
                }
            }
            catch ( UnknownHostException e ) {
                Throwables.propagate( e );
            }
    }
    public static ByteArrayOutputStream newLoggingOutputStream(final Class<?> clazz, final boolean info) {
        final Logger x = LoggerFactory.getLogger( clazz );
        return new ByteArrayOutputStream() {
            /**
             * upon flush() write the existing contents of the OutputStream
             * to the logger as a log record.
             * 
             * @throws java.io.IOException in case of error
             */
            @Override
            public void flush() throws IOException {
                synchronized ( this ) {
                    super.flush();
                    String record = this.toString();
                    super.reset();

                    if ( record.length() == 0 ) {
                        return;
                    }

                    if ( info ) {
                        x.info( record );
                    }
                    else {
                        x.debug( record );
                    }
                }
            }
        };
    }
    public static String localhost() {
        String address = "localhost";
        try {
            for ( InetAddress a : InetAddress.getAllByName( "localhost" ) ) {
                if ( !a.isLinkLocalAddress() ) {
                    address = a.getHostAddress();
                    break;
                }
            }
        }
        catch ( UnknownHostException e ) {
            LOGGER.debug( e.getMessage(), e );
        }
        return address;
    }
    public static long benchmark(final Runnable action) {
        long currentTime = System.currentTimeMillis();
        action.run();
        return System.currentTimeMillis() - currentTime;
    }
    public static void shutdownExecutor(ExecutorService executor) {
        shutdownExecutor( 1 << 4, executor );
    }
    public static void shutdownExecutor(int gracePeriodSecs, ExecutorService executor) {
        executor.shutdown(); // Disable new tasks from being submitted.
        try {
            // Wait a while for existing tasks to terminate.
            if ( !executor.awaitTermination( gracePeriodSecs, TimeUnit.SECONDS ) ) {
                executor.shutdownNow(); // Cancel currently executing tasks.
                // Wait a while for tasks to respond to being cancelled.
                if ( !executor.awaitTermination( gracePeriodSecs, TimeUnit.SECONDS ) ) {
                    LOGGER.warn( "Pool {} did not terminate", executor );
                }
            }
        }
        catch ( InterruptedException ie ) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    private Misc() {}
}

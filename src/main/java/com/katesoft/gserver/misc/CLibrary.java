package com.katesoft.gserver.misc;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.PointerType;

/**
 * class for native optimization over jna library.
 * 
 * @author andrey borisov
 */
public interface CLibrary extends Library {
    CLibrary INSTANCE = (CLibrary) Native.loadLibrary( ( Platform.isWindows() ? "msvcrt" : "c" ), CLibrary.class );

    int MCL_CURRENT = 1;
    int MCL_FUTURE = 2;

    int ENOMEM = 12;

    int mlockall(int flags);
    int sched_setaffinity(int pid, int cpusetsize, PointerType cpuset);
    int sched_getaffinity(int pid, int cpusetsize, PointerType cpuset);
    int sched_getcpu();
}

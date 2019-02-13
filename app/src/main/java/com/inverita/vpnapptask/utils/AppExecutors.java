package com.inverita.vpnapptask.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Global executor pools for the whole application.
 * Grouping tasks like this avoids the effects of task starvation.
 */
public class AppExecutors {

    private static final int THREAD_COUNT = 3;

    private final Executor networkIO;

    private final Executor mainThread;

    private AppExecutors(final Executor networkIO, final Executor mainThread) {
        this.networkIO = networkIO;
        this.mainThread = mainThread;
    }

    public AppExecutors() {
        this(Executors.newFixedThreadPool(THREAD_COUNT), new MainThreadExecutor());
    }

    /**
     * Method returns network thread for executing.
     * @return Network thread.
     */
    public Executor networkIO() {
        return networkIO;
    }

    /**
     * Method returns main thread for executing.
     * @return Main thread.
     */
    public Executor mainThread() {
        return mainThread;
    }

    /**
     * Custom Executor that needed for launching functions on main thread.
     */
    private static class MainThreadExecutor implements Executor {
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(final Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}

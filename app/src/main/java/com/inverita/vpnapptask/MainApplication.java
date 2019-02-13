package com.inverita.vpnapptask;

import android.app.Application;

import com.inverita.vpnapptask.utils.AppExecutors;

/**
 * Custom Class that extends from Application to provide access to some static functions and
 * variables throughout whole app.
 */
public class MainApplication extends Application {

    private static MainApplication instance;
    private AppExecutors appExecutors;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initAppExecutors();
    }

    private void initAppExecutors() {
        appExecutors = new AppExecutors();
    }

    /**
     * Method returns object of AppExecutors for launching stuff on different threads.
     * @return Object of AppExecutors
     */
    public AppExecutors getAppExecutors() {
        return appExecutors;
    }

    /**
     * Method returns static instance of Application.
     * @return Instance of Application
     */
    public static MainApplication getInstance() {
        return instance;
    }
}

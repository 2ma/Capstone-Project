package hu.am2.myway;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AppExecutors {

    private final Executor diskIO;
    private final Executor serviceExecutor;

    @Inject
    public AppExecutors() {
        this(Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor());
    }

    public AppExecutors(Executor diskIO, Executor serviceExecutor) {
        this.diskIO = diskIO;
        this.serviceExecutor = serviceExecutor;
    }

    public Executor getDiskIO() {
        return diskIO;
    }

    public Executor getServiceExecutor() {
        return serviceExecutor;
    }
}

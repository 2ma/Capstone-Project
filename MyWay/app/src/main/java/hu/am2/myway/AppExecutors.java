package hu.am2.myway;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AppExecutors {

    private final Executor diskIO;

    @Inject
    public AppExecutors() {
        this(Executors.newSingleThreadExecutor());
    }

    public AppExecutors(Executor diskIO) {
        this.diskIO = diskIO;
    }

    public Executor getDiskIO() {
        return diskIO;
    }
}

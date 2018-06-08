package hu.am2.myway.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import hu.am2.myway.location.LocationService;

@Module
public abstract class ServiceModule {

    @ContributesAndroidInjector
    abstract LocationService providesLocationService();
}

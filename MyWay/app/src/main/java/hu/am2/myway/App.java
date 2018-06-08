package hu.am2.myway;

import android.app.Activity;
import android.app.Application;
import android.app.Service;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasServiceInjector;
import hu.am2.myway.di.DaggerAppComponent;
import timber.log.Timber;


public class App extends Application implements HasActivityInjector, HasServiceInjector {

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjectorActivity;

    @Inject
    DispatchingAndroidInjector<Service> dispatchingAndroidInjectorService;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
        DaggerAppComponent.builder()
            .context(this)
            .build()
            .inject(this);
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjectorActivity;
    }

    @Override
    public AndroidInjector<Service> serviceInjector() {
        return dispatchingAndroidInjectorService;
    }
}

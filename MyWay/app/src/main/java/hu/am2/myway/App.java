package hu.am2.myway;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasBroadcastReceiverInjector;
import dagger.android.HasServiceInjector;
import hu.am2.myway.di.DaggerAppComponent;
import timber.log.Timber;


public class App extends Application implements HasActivityInjector, HasServiceInjector, HasBroadcastReceiverInjector {

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjectorActivity;

    @Inject
    DispatchingAndroidInjector<Service> dispatchingAndroidInjectorService;

    @Inject
    DispatchingAndroidInjector<BroadcastReceiver> dispatchingAndroidInjectorBroadcast;

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

    @Override
    public AndroidInjector<BroadcastReceiver> broadcastReceiverInjector() {
        return dispatchingAndroidInjectorBroadcast;
    }
}

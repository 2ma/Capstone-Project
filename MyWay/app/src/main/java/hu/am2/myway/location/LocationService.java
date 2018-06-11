package hu.am2.myway.location;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.arch.lifecycle.LiveData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.location.model.WayStatus;

//based on: https://github.com/googlesamples/android-play-location/tree/master/LocationUpdatesForegroundService

public class LocationService extends Service {

    private IBinder binder = new ServiceBinder();

    private boolean configurationChange = false;

    @Inject
    LocationProvider locationProvider;

    private static final String TAG = "LocationService";

    static final String CHANNEL_ID = "location_channel";
    static final int NOTIFICATION_ID = 1337;


    @Override
    public void onCreate() {
        AndroidInjection.inject(this);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (Constants.ACTION_START_PAUSE_RECORDING.equals(action)) {
            startPauseRecording();
        } else if (Constants.ACTION_STOP_RECORDING.equals(action)) {
            stopRecording();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configurationChange = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        configurationChange = false;
        locationProvider.onBindService();
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        configurationChange = false;
        locationProvider.onBindService();
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        locationProvider.unBindService();
        if (!configurationChange && locationProvider.getState() == WayStatus.STATE_RECORDING) {
            //TODO start foreground
            startForeground(NOTIFICATION_ID, locationProvider.getNotification(this));
        }
        return true;
    }

    public void startPauseRecording() {
        startService(new Intent(getApplicationContext(), LocationService.class));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationProvider.startPauseRecording();
        if (locationProvider.getState() != WayStatus.STATE_RECORDING) {
            stopSelf();
        }
    }

    public LiveData<WayStatus> getWayStatusLiveData() {
        return locationProvider.getWayStatusLiveData();
    }

    public LiveData<Long> getElapsedTimeLiveData() {
        return locationProvider.getElapsedTimeLiveData();
    }

    @Override
    public void onDestroy() {
        locationProvider.cleanUpOnDestroy();
    }

    public void stopRecording() {
        locationProvider.stopRecording();
        stopSelf();
    }

    public int getRecordingState() {
        return locationProvider.getState();
    }

    public WayStatus getWayStatus() {
        return locationProvider.getWayStatus();
    }

    public class ServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    //TODO WAKE LOCK
}

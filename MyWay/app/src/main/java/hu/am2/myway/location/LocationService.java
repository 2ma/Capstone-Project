package hu.am2.myway.location;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.arch.lifecycle.LiveData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;

import hu.am2.myway.R;
import hu.am2.myway.location.model.Track;
import timber.log.Timber;

//based on: https://github.com/googlesamples/android-play-location/tree/master/LocationUpdatesForegroundService

public class LocationService extends Service {

    private IBinder binder = new ServiceBinder();

    private Handler handler;

    private Looper looper;

    private NotificationManager notificationManager;

    private boolean configurationChange = false;

    LocationProvider locationProvider;

    private boolean bound = false;
    private boolean isRecroding = false;

    private long lastStartTime = -1;

    private static final String TAG = "LocationService";

    private static final String CHANNEL_ID = "location_channel";
    private static final int NOTIFICATION_ID = 1337;

    @Override
    public void onCreate() {


        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        looper = handlerThread.getLooper();

        locationProvider = new LocationProvider(this);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            notificationManager.createNotificationChannel(channel);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        bound = true;
        updateTime();
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        configurationChange = false;
        bound = true;
        updateTime();
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {

        if (!configurationChange) {
            //TODO start foreground
            startForeground(NOTIFICATION_ID, getNotification());
        }
        bound = false;

        return true;
    }

    private Notification getNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setContentText("Running in foreground")
            .setContentTitle("Foreground")
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_place_black_24dp)
            .setTicker("Foreground")
            .setWhen(System.currentTimeMillis());

        return builder.build();
    }

    public void recording() {
        if (isRecroding) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        startService(new Intent(getApplicationContext(), LocationService.class));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Timber.d("Looper main %s", Looper.getMainLooper());
        isRecroding = true;
        locationProvider.startRecording(looper);
        updateTime();
    }

    public void stopRecording() {
        locationProvider.stopRecording();
        handler.removeCallbacksAndMessages(null);
        isRecroding = false;
        stopSelf();
    }

    public LiveData<Track> getTrackLiveData() {
        return locationProvider.getTrackLiveData();
    }

    public LiveData<Long> getElapsedTimeLiveData() {
        return locationProvider.getElapsedTimeLiveData();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        looper.quit();
    }

    public void saveTrack() {

    }

    public class ServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    private void updateTime() {
        handler.postDelayed(timeUpdater, 500);
    }

    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            locationProvider.updateTime();
            if (bound && isRecroding) {
                handler.postDelayed(this, 500);
            }
        }
    };

}

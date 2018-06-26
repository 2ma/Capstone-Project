package hu.am2.myway.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.ui.permission.PermissionActivity;
import timber.log.Timber;

import static hu.am2.myway.location.WayRecorder.STATE_RECORDING;

//based on: https://github.com/googlesamples/android-play-location/tree/master/LocationUpdatesForegroundService

public class LocationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final IBinder binder = new ServiceBinder();

    private static final int UPDATE_INTERVAL_MILLISECONDS = 5000;


    @Inject
    WayRecorder wayRecorder;

    static final String CHANNEL_ID = "location_channel";
    static final int NOTIFICATION_ID = 1337;

    private Handler handler;

    private Looper looper;

    private LocationCallback locationCallback;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private LocationRequest locationRequest;

    private SharedPreferences sharedPreferences;

    private final MutableLiveData<Location> lastLocation = new MutableLiveData<>();

    private PowerManager.WakeLock wakeLock;

    private static final String WAKE_LOCK_TAG = "hum.am2.myway:locationservice";

    @Override
    public void onCreate() {

        AndroidInjection.inject(this);

        Timber.d("onCreate: Service");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        loadPreferencesAndRegisterListener();

        createLocationRequest();

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                handleLocationResult(locationResult);
            }
        };

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        HandlerThread handlerThread = new HandlerThread("locationService");
        handlerThread.start();
        handler = new Handler();

        looper = handlerThread.getLooper();

        requestLocationUpdates(looper);
    }

    private void handleLocationResult(LocationResult locationResult) {
        if (locationResult != null && locationResult.getLastLocation() != null) {
            Location location = locationResult.getLastLocation();
            lastLocation.postValue(location);
            if (wayRecorder.getState() == WayRecorder.STATE_RECORDING) {
                wayRecorder.handleLastLocation(location);
            }
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_MILLISECONDS);
        locationRequest.setFastestInterval(UPDATE_INTERVAL_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void loadPreferencesAndRegisterListener() {
        wayRecorder.loadPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates(Looper looper) {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, looper)
            .addOnFailureListener(e -> {
                //TODO handle failure
            });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        String action = intent.getAction();
        if (Constants.ACTION_START_PAUSE_RECORDING.equals(action)) {
            startPauseRecording();
        } else if (Constants.ACTION_STOP_RECORDING.equals(action)) {
            stopRecording();
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return binder;
    }

    public void startPauseRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, PermissionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setAction(PermissionActivity.ACTION_PERMISSION);
            startActivity(intent);
            Timber.d("Missing permission in service");
            stopSelf();
            return;
        }
        if (wayRecorder.getState() != STATE_RECORDING) {
            Timber.d("Starting recording from service");
            startService(new Intent(getApplicationContext(), LocationService.class));
            startForeground(NOTIFICATION_ID, wayRecorder.getNotification(this));
            wayRecorder.startWayRecording();
            startUiTimer();
            if (wakeLock == null) {
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
            }
            wakeLock.acquire();

        } else {
            Timber.d("Pausing recording from service");
            handler.removeCallbacksAndMessages(null);
            wayRecorder.pauseWayRecording();
            stopForeground(true);
            stopSelf();
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        }
    }

    public LiveData<Integer> getStateLiveData() {
        return wayRecorder.getStateLiveData();
    }

    public LiveData<Location> getLocationLiveData() {
        return lastLocation;
    }

    public LiveData<Long> getTotalTimeLiveData() {
        return wayRecorder.getTotalTimeLiveData();
    }

    @Override
    public void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        handler.removeCallbacksAndMessages(null);
        looper.quit();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
        Timber.d("<--DESTROY-->");
    }

    public void stopRecording() {
        handler.removeCallbacksAndMessages(null);
        wayRecorder.stopWayRecording();
        stopForeground(true);
        stopSelf();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }

    }

    public int getRecordingState() {
        return wayRecorder.getState();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        wayRecorder.preferenceChanged(key);
    }

    public class ServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }

    }

    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Time update");
            wayRecorder.timedUpdate();
            if (wayRecorder.getState() == STATE_RECORDING) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void startUiTimer() {
        handler.postDelayed(timeUpdater, 1000);
    }

    private static final String TAG = LocationService.class.getSimpleName();
}

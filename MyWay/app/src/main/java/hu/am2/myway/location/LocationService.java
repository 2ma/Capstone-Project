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
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import hu.am2.myway.AppExecutors;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.location.model.WayUiModel;
import hu.am2.myway.ui.permission.PermissionActivity;
import timber.log.Timber;

import static hu.am2.myway.location.WayRecorder.STATE_RECORDING;
import static hu.am2.myway.location.WayRecorder.STATE_STOP;

//based on: https://github.com/googlesamples/android-play-location/tree/master/LocationUpdatesForegroundService

public class LocationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private IBinder binder = new ServiceBinder();

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

    private MutableLiveData<Location> lastLocation = new MutableLiveData<>();


    @Inject
    AppExecutors executors;

    @Override
    public void onCreate() {

        AndroidInjection.inject(this);

        Timber.d("onCreate: Service");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        wayRecorder.setState(sharedPreferences.getInt(Constants.PREF_RECORDING_STATE, STATE_STOP));

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
        handler = new Handler(handlerThread.getLooper());

        looper = handlerThread.getLooper();

        requestLocationUpdates(looper);

        wayRecorder.loadWay();
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

        } else {
            Timber.d("Pausing recording from service");
            wayRecorder.pauseWayRecording();
            stopForeground(true);
            stopSelf();
        }
    }

    public LiveData<WayUiModel> getWayUiModelLiveData() {
        return wayRecorder.getWayUiModelLiveData();
    }

    public LiveData<Long> getElapsedTimeLiveData() {
        return wayRecorder.getElapsedTimeLiveData();
    }

    public LiveData<Integer> getStateLiveData() {
        return wayRecorder.getStateLiveData();
    }

    public LiveData<Location> getLocationLiveData() {
        return lastLocation;
    }

    @Override
    public void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        handler.removeCallbacksAndMessages(null);
        looper.quit();
        Timber.d("<--DESTROY-->");
    }

    public void stopRecording() {
        executors.getServiceExecutor().execute(() -> wayRecorder.stopWayRecording());
        stopForeground(true);
        stopSelf();
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
            wayRecorder.updateTime();
            wayRecorder.updateWidget();
            if (wayRecorder.getState() == STATE_RECORDING) {
                handler.postDelayed(this, 500);
            }
        }
    };

    private void startUiTimer() {
        handler.postDelayed(timeUpdater, 500);
    }

    //TODO WAKE LOCK
}

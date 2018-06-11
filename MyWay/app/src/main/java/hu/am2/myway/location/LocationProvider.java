package hu.am2.myway.location;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import hu.am2.myway.AppExecutors;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.appwidget.MyWayWidget;
import hu.am2.myway.appwidget.WidgetStatus;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayPoint;
import hu.am2.myway.location.model.WayStatus;
import hu.am2.myway.location.model.WayWithWayPoints;
import hu.am2.myway.ui.map.MapActivity;
import hu.am2.myway.ui.saveway.SaveWayActivity;

import static hu.am2.myway.location.LocationService.CHANNEL_ID;

@Singleton
public class LocationProvider implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int UPDATE_INTERVAL_MILLISECONDS = 5000;

    private MutableLiveData<Long> elapsedTime = new MutableLiveData<>();
    private MutableLiveData<WayStatus> wayStatus = new MutableLiveData<>();

    private LocationRequest locationRequest;

    private LocationCallback locationCallback;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private SharedPreferences sharedPreferences;

    private int timeInterval;
    private int distanceInterval;
    private int gpsAccuracy;

    private int state = WayStatus.STATE_STOP;

    private Repository repository;

    private AppExecutors executors;

    private Application application;

    private Handler handler;

    private Looper looper;

    private boolean serviceIsBound;

    private AppWidgetManager appWidgetManager;

    private ComponentName appWidgetComponent;


    @Inject
    public LocationProvider(Application application, Repository repository, AppExecutors appExecutors) {

        this.application = application;
        this.repository = repository;
        executors = appExecutors;
        appWidgetManager = AppWidgetManager.getInstance(application);
        appWidgetComponent = new ComponentName(application, MyWayWidget.class);


        HandlerThread handlerThread = new HandlerThread("locationProvider");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        looper = handlerThread.getLooper();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                handleLocationResult(locationResult);
            }
        };

        createLocationRequest();
    }

    void onBindService() {
        serviceIsBound = true;
        WayStatus status = wayStatus.getValue();
        if (status == null) {
            int state = sharedPreferences.getInt(Constants.PREF_RECORDING_STATE, WayStatus.STATE_STOP);
            long id = sharedPreferences.getLong(Constants.PREF_WAY_ID, -1);
            if (state != WayStatus.STATE_STOP) {
                executors.getDiskIO().execute(() -> {
                    WayWithWayPoints wayWithWayPoints = repository.getWayWithWayPointsForId(id);
                    WayStatus wayStatus = new WayStatus(wayWithWayPoints);
                    wayStatus.setState(state);
                    this.wayStatus.postValue(wayStatus);
                    elapsedTime.postValue(wayStatus.getWay().getTotalTime());
                    if (state == WayStatus.STATE_RECORDING || state == WayStatus.STATE_WAITING_FOR_SIGNAL) {
                        startRecording();
                    }
                });
            }
        } else if (status.getState() == WayStatus.STATE_RECORDING) {
            updateTime(status);
        }
    }

    void unBindService() {
        serviceIsBound = false;
    }

    private void handleLocationResult(LocationResult locationResult) {
        WayStatus status = wayStatus.getValue();

        if (locationResult == null || locationResult.getLastLocation() == null || !locationResult.getLastLocation().hasAccuracy()) {
            status.setState(WayStatus.STATE_WAITING_FOR_SIGNAL);
            wayStatus.postValue(status);
            return;
        }
        Location currentLocation = locationResult.getLastLocation();
        if (currentLocation.getAccuracy() > gpsAccuracy) {
            status.setState(WayStatus.STATE_WAITING_FOR_SIGNAL);
            wayStatus.postValue(status);
            return;
        }
        status.setState(WayStatus.STATE_RECORDING);
        //TODO check if location fits the accuracy, time, and distance parameters

        Location previousLocation = status.getLastLocation();
        if (previousLocation == null) {
            status.updateFirstLocation(currentLocation);
            repository.updateWay(status.getWay());
            repository.insertWayPoint(new WayPoint(currentLocation, status.getWay().getId()));
        } else {
            float dist = currentLocation.distanceTo(previousLocation);
            //TODO check if previous location has time, could be it doesn't if it was reloaded
            long time = TimeUnit.NANOSECONDS.toMillis(currentLocation.getElapsedRealtimeNanos() - previousLocation.getElapsedRealtimeNanos());
            if (dist >= distanceInterval && time >= timeInterval) {
                status.updateCurrentLocation(currentLocation);
                repository.updateWay(status.getWay());
                repository.insertWayPoint(new WayPoint(currentLocation, status.getWay().getId()));
            }
        }

        //TODO update way
        wayStatus.postValue(status);
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_MILLISECONDS);
        locationRequest.setFastestInterval(UPDATE_INTERVAL_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates(Looper looper) {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, looper)
            .addOnFailureListener(e -> {
                //TODO handle failure
            });
    }

    private void updateTime(WayStatus status) {
        elapsedTime.postValue(status.calculateTotalTime());

    }

    void startPauseRecording() {
        if (state == WayStatus.STATE_RECORDING) {
            pauseRecording();
        } else {
            startRecording();
        }
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        state = WayStatus.STATE_RECORDING;
        loadPreferencesAndRegisterListener();
        WayStatus status = wayStatus.getValue();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.PREF_RECORDING_STATE, WayStatus.STATE_RECORDING);

        if (status == null) {
            executors.getDiskIO().execute(() -> {
                Way way = new Way();
                way.setStartTime(System.currentTimeMillis());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH:mm", Locale.getDefault());
                String name = "MyWay-" + dateFormat.format(Calendar.getInstance().getTime());
                way.setWayName(name);
                long id = repository.insertWay(way);
                editor.putLong(Constants.PREF_WAY_ID, id);
                editor.apply();
                way.setId(id);
                WayStatus s = new WayStatus();
                s.setWay(way);
                s.setState(WayStatus.STATE_WAITING_FOR_SIGNAL);
                s.setLastStartTime(SystemClock.elapsedRealtime());
                wayStatus.postValue(s);
                requestLocationUpdates(looper);
            });
        } else {
            editor.apply();
            status.setState(WayStatus.STATE_WAITING_FOR_SIGNAL);
            status.setLastStartTime(SystemClock.elapsedRealtime());
            wayStatus.postValue(status);
            requestLocationUpdates(looper);
        }
        startUiTimer();
    }

    int getState() {
        return state;
    }

    WayStatus getWayStatus() {
        return wayStatus.getValue();
    }

    private void pauseRecording() {
        state = WayStatus.STATE_PAUSE;
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        WayStatus status = wayStatus.getValue();
        status.setEndTime();
        status.setState(WayStatus.STATE_PAUSE);
        wayStatus.postValue(status);
        //TODO save state and way
        repository.updateWay(status.getWay());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.PREF_RECORDING_STATE, WayStatus.STATE_PAUSE).apply();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        handler.removeCallbacksAndMessages(null);
    }

    void stopRecording() {
        state = WayStatus.STATE_STOP;
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        WayStatus status = wayStatus.getValue();
        status.setEndTime();
        status.setState(WayStatus.STATE_STOP);
        wayStatus.postValue(status);
        //TODO save state and way
        repository.updateWay(status.getWay());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(Constants.PREF_WAY_ID, -1);
        editor.putInt(Constants.PREF_RECORDING_STATE, WayStatus.STATE_STOP).apply();
        wayStatus.postValue(null);
        elapsedTime.postValue(0L);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        handler.removeCallbacksAndMessages(null);

        //start save activity for name change
        Intent saveWayIntent = new Intent(application, SaveWayActivity.class);
        saveWayIntent.putExtra(Constants.EXTRA_WAY_ID, status.getWay().getId());
        saveWayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        application.startActivity(saveWayIntent);
    }

    LiveData<WayStatus> getWayStatusLiveData() {
        return wayStatus;
    }

    LiveData<Long> getElapsedTimeLiveData() {
        return elapsedTime;
    }

    private void loadPreferencesAndRegisterListener() {
        //default value is 1s
        timeInterval = sharedPreferences.getInt(Constants.PREF_TIME_INTERVAL, 1000);
        //default value is 10meters
        distanceInterval = sharedPreferences.getInt(Constants.PREF_DISTANCE_INTERVAL, 10);
        //default value is 50meters
        gpsAccuracy = sharedPreferences.getInt(Constants.PREF_GPS_ACCURACY, 50);

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PREF_DISTANCE_INTERVAL: {
                distanceInterval = sharedPreferences.getInt(Constants.PREF_DISTANCE_INTERVAL, 10);
                break;
            }
            case Constants.PREF_TIME_INTERVAL: {
                timeInterval = sharedPreferences.getInt(Constants.PREF_TIME_INTERVAL, 1000);
                break;
            }
            case Constants.PREF_GPS_ACCURACY: {
                gpsAccuracy = sharedPreferences.getInt(Constants.PREF_GPS_ACCURACY, 50);
                break;
            }
        }
    }

    Notification getNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        Intent pauseIntent = new Intent(context, LocationService.class);
        pauseIntent.setAction(Constants.ACTION_START_PAUSE_RECORDING);
        PendingIntent pausePending = PendingIntent.getService(context, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stopIntent = new Intent(context, LocationService.class);
        stopIntent.setAction(Constants.ACTION_STOP_RECORDING);
        PendingIntent stopPending = PendingIntent.getService(context, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentMapIntent = new Intent(context, MapActivity.class);
        intentMapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent mapPending = PendingIntent.getActivity(context, 2, intentMapIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentText(context.getString(R.string.recording_notification))
            .setContentTitle(context.getString(R.string.app_name))
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_place_black_24dp)
            .setTicker("Foreground")
            .addAction(R.drawable.ic_pause_png, context.getString(R.string.pause_notification), pausePending)
            .addAction(R.drawable.ic_stop_png, context.getString(R.string.stop_notification), stopPending)
            .setContentIntent(mapPending)
            .setWhen(System.currentTimeMillis());

        return builder.build();
    }

    private void startUiTimer() {
        handler.postDelayed(timeUpdater, 500);
    }

    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            WayStatus status = wayStatus.getValue();
            if (status != null) {
                updateTime(status);
                updateWidget(status);
            }
            if (state == WayStatus.STATE_RECORDING) {
                handler.postDelayed(this, 500);
            }
        }
    };

    private void updateWidget(WayStatus status) {
        Way way = status.getWay();
        WidgetStatus widgetStatus = new WidgetStatus(way.getTotalDistance(), status.calculateTotalTime(), state);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(appWidgetComponent);
        MyWayWidget.updateAppWidget(application, appWidgetManager, appWidgetIds, widgetStatus);

    }

    void cleanUpOnDestroy() {
        handler.removeCallbacksAndMessages(null);
        looper.quit();
    }
}

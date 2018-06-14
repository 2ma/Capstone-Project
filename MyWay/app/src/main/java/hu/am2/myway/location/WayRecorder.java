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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import hu.am2.myway.AppExecutors;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.appwidget.MyWayWidget;
import hu.am2.myway.appwidget.WidgetStatus;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayPoint;
import hu.am2.myway.location.model.WayStatus;
import hu.am2.myway.location.model.WayUiModel;
import hu.am2.myway.location.model.WayWithWayPoints;
import hu.am2.myway.ui.map.MapActivity;
import hu.am2.myway.ui.saveway.SaveWayActivity;
import timber.log.Timber;

import static hu.am2.myway.location.LocationService.CHANNEL_ID;

public class WayRecorder {

    public static final int STATE_RECORDING = 0;
    public static final int STATE_PAUSE = 1;
    public static final int STATE_STOP = 2;

    public static final int STATE_WAITING_FOR_SIGNAL = 4;

    private MutableLiveData<Long> elapsedTime = new MutableLiveData<>();
    private MutableLiveData<WayUiModel> wayUiModelLiveData = new MutableLiveData<>();
    private MutableLiveData<Integer> stateLiveData = new MutableLiveData<>();

    private SharedPreferences sharedPreferences;

    //TODO see if volatile needed
    private volatile int state = STATE_STOP;

    private Repository repository;

    private AppExecutors executors;

    private Application application;

    private AppWidgetManager appWidgetManager;

    private ComponentName appWidgetComponent;

    private int timeInterval;
    private int distanceInterval;
    private int gpsAccuracy;

    //TODO check if needed
    private volatile WayStatus wayStatus;




    @Inject
    public WayRecorder(Application application, Repository repository, AppExecutors appExecutors) {

        Timber.d("<--WayRecorder: new instance-->");

        this.application = application;
        this.repository = repository;
        executors = appExecutors;
        appWidgetManager = AppWidgetManager.getInstance(application);
        appWidgetComponent = new ComponentName(application, MyWayWidget.class);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
    }

    void setState(int state) {
        Timber.d("Set state");
        this.state = state;
        stateLiveData.postValue(state);
    }

    int getState() {
        return state;
    }

    void loadPreferences() {
        //default value is 1s
        timeInterval = sharedPreferences.getInt(Constants.PREF_TIME_INTERVAL, 1000);
        //default value is 10meters
        distanceInterval = sharedPreferences.getInt(Constants.PREF_DISTANCE_INTERVAL, 10);
        //default value is 50meters
        gpsAccuracy = sharedPreferences.getInt(Constants.PREF_GPS_ACCURACY, 50);
    }

    void preferenceChanged(String key) {
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

    void loadWay() {
        Timber.d("Way loading");
        long id = sharedPreferences.getLong(Constants.PREF_WAY_ID, -1);
        if (id != -1) {
            executors.getServiceExecutor().execute(() -> {
                WayWithWayPoints wayWithWayPoints = repository.getWayWithWayPointsForId(id);
                wayStatus = new WayStatus(wayWithWayPoints);
                wayUiModelLiveData.postValue(new WayUiModel(wayStatus.getWay(), wayStatus.getWayPoints()));
                elapsedTime.postValue(wayStatus.getWay().getTotalTime());
                Timber.d("Way loaded");
            });
        }
    }

    void handleLastLocation(Location location) {
        if (!location.hasAccuracy() || location.getAccuracy() > gpsAccuracy) {
            stateLiveData.postValue(STATE_WAITING_FOR_SIGNAL);
            return;
        }

        stateLiveData.postValue(STATE_RECORDING);
        if (wayStatus != null) {
            Location previousLocation = wayStatus.getLastLocation();
            if (previousLocation == null) {
                wayStatus.updateFirstLocation(location);
                wayUiModelLiveData.postValue(new WayUiModel(wayStatus.getWay(), wayStatus.getWayPoints()));
                repository.updateWay(wayStatus.getWay());
                repository.insertWayPoint(new WayPoint(location, wayStatus.getWay().getId()));
            } else {
                float dist = location.distanceTo(previousLocation);
                //TODO check if previous location has time, could be it doesn't if it was reloaded
                long time = TimeUnit.NANOSECONDS.toMillis(location.getElapsedRealtimeNanos() - previousLocation.getElapsedRealtimeNanos());
                if (dist >= distanceInterval && time >= timeInterval) {
                    wayStatus.updateCurrentLocation(location);
                    wayUiModelLiveData.postValue(new WayUiModel(wayStatus.getWay(), wayStatus.getWayPoints()));
                    repository.updateWay(wayStatus.getWay());
                    repository.insertWayPoint(new WayPoint(location, wayStatus.getWay().getId()));
                }
            }
        }
    }

    /*void handle(LocationResult locationResult) {
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
    }*/


    void updateTime() {
        if (wayStatus != null) {
            elapsedTime.postValue(wayStatus.calculateTotalTime());
        }
    }

    @SuppressLint("MissingPermission")
    void startWayRecording() {
        state = STATE_RECORDING;
        executors.getServiceExecutor().execute(() -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(Constants.PREF_RECORDING_STATE, STATE_RECORDING);
            long id = sharedPreferences.getLong(Constants.PREF_WAY_ID, -1);
            if (id == -1) {
                Way way = new Way();
                way.setStartTime(System.currentTimeMillis());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH:mm", Locale.getDefault());
                String name = "MyWay-" + dateFormat.format(Calendar.getInstance().getTime());
                way.setWayName(name);
                id = repository.insertWay(way);
                editor.putLong(Constants.PREF_WAY_ID, id);
                way.setId(id);
                wayStatus = new WayStatus();
                wayStatus.setWay(way);
                Timber.d("new way");
            }
            editor.apply();
            wayStatus.setLastStartTime(SystemClock.elapsedRealtime());
            stateLiveData.postValue(STATE_RECORDING);
            wayUiModelLiveData.postValue(new WayUiModel(wayStatus.getWay(), wayStatus.getWayPoints()));
            Timber.d("RECORDING START: ");
        });
    }

    void pauseWayRecording() {
        state = STATE_PAUSE;
        executors.getServiceExecutor().execute(() -> {
            wayStatus.setEndTime();
            stateLiveData.postValue(STATE_PAUSE);
            wayUiModelLiveData.postValue(new WayUiModel(wayStatus.getWay(), wayStatus.getWayPoints()));
            updateWidget();
            //TODO save state and way
            repository.updateWay(wayStatus.getWay());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(Constants.PREF_RECORDING_STATE, STATE_PAUSE).apply();
            Timber.d("RECORDING PAUSE: ");
        });

    }

    void stopWayRecording() {
        state = STATE_STOP;
        executors.getServiceExecutor().execute(() -> {
            wayStatus.setEndTime();
            stateLiveData.postValue(STATE_STOP);
            elapsedTime.postValue(0L);
            wayUiModelLiveData.postValue(new WayUiModel(wayStatus.getWay(), wayStatus.getWayPoints()));
            updateWidget();
            //TODO save state and way
            repository.updateWay(wayStatus.getWay());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(Constants.PREF_WAY_ID, -1);
            editor.putInt(Constants.PREF_RECORDING_STATE, STATE_STOP).apply();
            //TODO reset widget?
            //start save activity for name change
            Intent saveWayIntent = new Intent(application, SaveWayActivity.class);
            saveWayIntent.putExtra(Constants.EXTRA_WAY_ID, wayStatus.getWay().getId());
            saveWayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            application.startActivity(saveWayIntent);
            Timber.d("RECORDING STOP: ");
        });

    }

    LiveData<WayUiModel> getWayUiModelLiveData() {
        return wayUiModelLiveData;
    }

    LiveData<Long> getElapsedTimeLiveData() {
        return elapsedTime;
    }

    LiveData<Integer> getStateLiveData() {
        return stateLiveData;
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

    void updateWidget() {
        if (wayStatus != null) {
            Way way = wayStatus.getWay();
            WidgetStatus widgetStatus = new WidgetStatus(way.getTotalDistance(), wayStatus.calculateTotalTime(), state);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(appWidgetComponent);
            MyWayWidget.updateAppWidget(application, appWidgetManager, appWidgetIds, widgetStatus);
        }
    }
}

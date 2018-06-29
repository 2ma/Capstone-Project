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
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import hu.am2.myway.AppExecutors;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.appwidget.MyWayWidget;
import hu.am2.myway.appwidget.WidgetStatus;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayPoint;
import hu.am2.myway.ui.map.MapActivity;
import hu.am2.myway.ui.saveway.SaveWayActivity;
import timber.log.Timber;

import static hu.am2.myway.location.LocationService.CHANNEL_ID;

public class WayRecorder {

    public static final int STATE_RECORDING = 0;
    public static final int STATE_PAUSE = 1;
    public static final int STATE_STOP = 2;

    public static final int STATE_WAITING_FOR_SIGNAL = 4;

    private final MutableLiveData<Integer> stateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Long> totalTimeLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> speedLiveData = new MutableLiveData<>();

    private final SharedPreferences sharedPreferences;

    private long lastRecordedTime = -1;

    private Location lastLocation;

    private long wayId;

    private volatile int state;

    private final AtomicLong totalTime = new AtomicLong(0);

    private final Repository repository;

    private final AppExecutors executors;

    private final Application application;

    private final AppWidgetManager appWidgetManager;

    private final ComponentName appWidgetComponent;

    private int timeInterval;
    private int distanceInterval;
    private int gpsAccuracy;

    private final String timeKey;
    private final String distanceKey;
    private final String gpsKey;

    private Way way;

    private final Object lock = new Object();

    @Inject
    public WayRecorder(Application application, Repository repository, AppExecutors appExecutors) {

        Timber.d("<--WayRecorder: new instance-->");

        this.application = application;
        this.repository = repository;
        executors = appExecutors;
        appWidgetManager = AppWidgetManager.getInstance(application);
        appWidgetComponent = new ComponentName(application, MyWayWidget.class);
        timeKey = application.getString(R.string.recording_time_interval_key);
        distanceKey = application.getString(R.string.recording_distance_interval_key);
        gpsKey = application.getString(R.string.gps_accuracy_key);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        wayId = sharedPreferences.getLong(Constants.PREF_WAY_ID, -1);
        if (wayId != -1) {
            loadWay();
        }
        state = sharedPreferences.getInt(Constants.PREF_RECORDING_STATE, STATE_STOP);
        stateLiveData.setValue(state);
    }

    private void loadWay() {
        executors.getServiceExecutor().execute(() -> {
            synchronized (lock) {
                way = repository.getWayForId(wayId);
                totalTime.set(way.getTotalTime());
                totalTimeLiveData.postValue(totalTime.get());
                WayPoint wayPoint = repository.getWayWithLastLocationForWayId(wayId);
                if (wayPoint != null) {
                    lastLocation = new Location("way");
                    lastLocation.setSpeed(wayPoint.getSpeed());
                    lastLocation.setAccuracy(wayPoint.getAccuracy());
                    lastLocation.setLongitude(wayPoint.getLongitude());
                    lastLocation.setLatitude(wayPoint.getLatitude());
                    lastLocation.setTime(wayPoint.getTime());
                }
            }
        });
    }

    int getState() {
        return state;
    }

    void loadPreferences() {
        //default value is 1s
        String t = sharedPreferences.getString(timeKey, "");
        timeInterval = t.length() > 0 ? Integer.valueOf(t) : 1000;
        //default value is 10meters
        String d = sharedPreferences.getString(distanceKey, "");
        distanceInterval = d.length() > 0 ? Integer.valueOf(d) : 10;
        //default value is 50meters
        String g = sharedPreferences.getString(gpsKey, "");
        gpsAccuracy = g.length() > 0 ? Integer.valueOf(g) : 50;
    }

    void preferenceChanged(String key) {
        if (timeKey.equals(key)) {
            String t = sharedPreferences.getString(timeKey, "");
            timeInterval = t.length() > 0 ? Integer.valueOf(t) : 1000;
        } else if (distanceKey.equals(key)) {
            String d = sharedPreferences.getString(distanceKey, "");
            distanceInterval = d.length() > 0 ? Integer.valueOf(d) : 10;
        } else if (gpsKey.equals(key)) {
            String g = sharedPreferences.getString(gpsKey, "");
            gpsAccuracy = g.length() > 0 ? Integer.valueOf(g) : 50;
        }
    }

   /* void loadWay() {
        Timber.d("Way loading");
        long id = sharedPreferences.getLong(Constants.PREF_WAY_ID, -1);
        if (id != -1) {
            executors.getServiceExecutor().execute(() -> {
                synchronized (lock) {
                    WayWithWayPoints wayWithWayPoints = repository.getWayWithWayPointsForId(id);
                    wayStatus.initWayStatus(wayWithWayPoints);
                    wayUiModelLiveData.postValue(new WayUiModel(wayStatus.getWay(), wayStatus.getWayPoints()));
                    elapsedTime.postValue(wayStatus.getWay().getTotalTime());
                }
                Timber.d("Way loaded");
            });
        }
    }*/

    void handleLastLocation(Location location) {
        Timber.d("Handle location");
        if (wayId == -1) {
            Timber.e("Location recording, wayId is -1");
            return;
        }

        if (!location.hasAccuracy() || location.getAccuracy() > gpsAccuracy) {
            stateLiveData.postValue(STATE_WAITING_FOR_SIGNAL);
            speedLiveData.postValue(0f);
            return;
        }
        stateLiveData.postValue(STATE_RECORDING);
        speedLiveData.postValue(location.getSpeed());
        if (lastLocation == null) {
            synchronized (lock) {
                if (location.hasAltitude()) {
                    way.setMaxAltitude(location.getAltitude());
                    way.setMinAltitude(location.getAltitude());
                } else {
                    way.setMaxAltitude(9999);
                    way.setMinAltitude(-9999);
                }
                way.setMaxSpeed(location.getSpeed());
            }
            lastLocation = location;
            executors.getDiskIO().execute(() -> {
                Way w;
                synchronized (lock) {
                    w = way.getWayCopy();
                }
                repository.updateWay(w);
                repository.insertWayPoint(new WayPoint(location, wayId));
            });
        } else {
            float dist = location.distanceTo(lastLocation);
            long time = TimeUnit.NANOSECONDS.toMillis(location.getElapsedRealtimeNanos() - lastLocation.getElapsedRealtimeNanos());
            if (dist >= distanceInterval && time >= timeInterval) {
                synchronized (lock) {
                    way.setTotalDistance(way.getTotalDistance() + dist);
                    way.setAvgSpeed(way.getTotalDistance() / (way.getTotalTime() / 1000));
                    if (location.hasSpeed() && location.getSpeed() > way.getMaxSpeed()) {
                        way.setMaxSpeed(location.getSpeed());
                    }
                    if (location.hasAltitude()) {
                        double alt = location.getAltitude();
                        if (alt > way.getMaxAltitude()) {
                            way.setMaxAltitude(alt);
                        }
                        if (alt < way.getMinAltitude()) {
                            way.setMinAltitude(alt);
                        }
                    }
                }
                lastLocation = location;
                executors.getDiskIO().execute(() -> {
                    Way w;
                    synchronized (lock) {
                        w = way.getWayCopy();
                    }
                    repository.updateWay(w);
                    repository.insertWayPoint(new WayPoint(location, wayId));
                });
            }
        }



        /*if (wayStatus.getWay() != null) {
            Location previousLocation = wayStatus.getLastLocation();
            if (previousLocation == null) {
                wayStatus.updateCurrentLocation(location);
                wayUiModelLiveData.postValue(new WayUiModel(wayStatus.getWay(), wayStatus.getWayPoints()));
                repository.updateWay(wayStatus.getWay());
                repository.insertWayPoint(new WayPoint(location, wayStatus.getWay().getId()));
            } else {
                float dist = location.distanceTo(previousLocation);
                long time = TimeUnit.NANOSECONDS.toMillis(location.getElapsedRealtimeNanos() - previousLocation.getElapsedRealtimeNanos());
                if (dist >= distanceInterval && time >= timeInterval) {
                    wayStatus.updateCurrentLocation(location);
                    wayUiModelLiveData.postValue(new WayUiModel(wayStatus.getWay(), wayStatus.getWayPoints()));
                    repository.updateWay(wayStatus.getWay());
                    repository.insertWayPoint(new WayPoint(location, wayStatus.getWay().getId()));
                }
            }
        }*/
    }

    private void calculateTotalTime() {
        if (lastRecordedTime == -1) {
            return;
        }
        long currentTime = SystemClock.elapsedRealtime();
        long t = currentTime - lastRecordedTime;
        lastRecordedTime = currentTime;
        totalTime.addAndGet(Math.max(0, t));
    }

    @SuppressLint("MissingPermission")
    void startWayRecording() {
        state = STATE_RECORDING;
        stateLiveData.postValue(STATE_RECORDING);
        lastRecordedTime = SystemClock.elapsedRealtime();
        executors.getServiceExecutor().execute(() -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(Constants.PREF_RECORDING_STATE, STATE_RECORDING);
            if (wayId == -1) {
                synchronized (lock) {
                    way = new Way();
                    way.setStartTime(System.currentTimeMillis());
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH:mm", Locale.getDefault());
                    String name = "MyWay-" + dateFormat.format(Calendar.getInstance().getTime());
                    way.setWayName(name);
                    wayId = repository.insertWay(way);
                    editor.putLong(Constants.PREF_WAY_ID, wayId);
                    way.setId(wayId);
                    Timber.d("new way");
                }
            }
            editor.apply();
            Timber.d("RECORDING START: ");
        });
    }

    void pauseWayRecording() {
        state = STATE_PAUSE;
        stateLiveData.postValue(STATE_PAUSE);
        speedLiveData.postValue(0f);
        executors.getServiceExecutor().execute(() -> {
            Way w;
            synchronized (lock) {
                calculateTotalTime();
                way.setTotalTime(totalTime.get());
                way.setEndTime(System.currentTimeMillis());
                w = way.getWayCopy();
                updateWidget();
            }
            repository.updateWay(w);
            lastRecordedTime = -1;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(Constants.PREF_RECORDING_STATE, STATE_PAUSE).apply();
            Timber.d("RECORDING PAUSE: ");
        });

    }

    void stopWayRecording() {
        state = STATE_STOP;
        stateLiveData.postValue(STATE_STOP);
        speedLiveData.postValue(0f);
        executors.getServiceExecutor().execute(() -> {
            Way w;
            synchronized (lock) {
                calculateTotalTime();
                way.setTotalTime(totalTime.get());
                way.setEndTime(System.currentTimeMillis());
                totalTime.set(0);
                totalTimeLiveData.postValue(0L);
                w = way.getWayCopy();
                way = null;
            }
            updateWidget();
            repository.updateWay(w);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(Constants.PREF_WAY_ID, -1);
            editor.putInt(Constants.PREF_RECORDING_STATE, STATE_STOP).apply();
            //start save activity for name change
            Intent saveWayIntent = new Intent(application, SaveWayActivity.class);
            saveWayIntent.putExtra(Constants.EXTRA_WAY_ID, wayId);
            saveWayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            application.startActivity(saveWayIntent);
            wayId = -1;
            lastRecordedTime = -1;
            lastLocation = null;
            totalTimeLiveData.postValue(0L);
            Timber.d("RECORDING STOP: ");
        });

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

    LiveData<Long> getTotalTimeLiveData() {
        return totalTimeLiveData;
    }

    public LiveData<Float> getSpeedLiveData() {
        return speedLiveData;
    }

    void timedUpdate() {
        if (wayId != -1) {
            updateTime();
            updateWidget();
            executors.getDiskIO().execute(() -> {
                Way w;
                synchronized (lock) {
                    way.setTotalTime(totalTime.get());
                    way.setEndTime(lastRecordedTime);
                    w = way.getWayCopy();
                }
                repository.updateWay(w);
            });
        }
    }

    private void updateTime() {
        calculateTotalTime();
        totalTimeLiveData.postValue(totalTime.get());
    }

    private void updateWidget() {
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(appWidgetComponent);
        WidgetStatus status = null;
        synchronized (lock) {
            if (way != null) {
                status = new WidgetStatus(way.getTotalDistance() / 1000, totalTime.get(), state);
            }
        }
        MyWayWidget.updateAppWidgets(application, appWidgetManager, appWidgetIds, status);
    }

    private static final String TAG = WayRecorder.class.getSimpleName();
}

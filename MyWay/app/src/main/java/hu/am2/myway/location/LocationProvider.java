package hu.am2.myway.location;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import hu.am2.myway.Constants;
import hu.am2.myway.location.model.Track;
import timber.log.Timber;

public class LocationProvider implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int UPDATE_INTERVAL_MILLISECONDS = 5000;

    private MutableLiveData<Track> track = new MutableLiveData<>();
    private MutableLiveData<Long> elapsedTime = new MutableLiveData<>();

    private LocationRequest locationRequest;

    private LocationCallback locationCallback;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private int state;

    private ContentResolver contentResolver;

    private SharedPreferences sharedPreferences;

    private int timeInterval;
    private int distanceInterval;
    private int gpsAccuracy;

    public LocationProvider(Context context) {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        contentResolver = context.getContentResolver();

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                handleLocationResult(locationResult);
            }
        };

        createLocationRequest();
    }

    private void handleLocationResult(LocationResult locationResult) {
        Track t = track.getValue();

        if (locationResult == null) {
            t.setState(Track.STATE_WAITING_FOR_SIGNAL);
            track.postValue(t);
            return;
        }
        Location lastLocation = locationResult.getLastLocation();
        if (lastLocation.getAccuracy() > gpsAccuracy) {
            t.setState(Track.STATE_WAITING_FOR_SIGNAL);
            track.postValue(t);
            return;
        }
        t.setState(Track.STATE_RECORDING);
        //TODO check if location fits the accuracy, time, and distance parameters
        Location previousLocation = t.getLastLocation();
        if (previousLocation == null) {
            t.setLastLocation(lastLocation);
        } else {
            float dist = lastLocation.distanceTo(previousLocation);
            long time = lastLocation.getTime() - previousLocation.getTime();
            if (dist >= distanceInterval && time >= timeInterval) {
                //TODO save location to db
                t.setLastLocation(lastLocation);
            }
        }
        track.postValue(t);
        Timber.d(locationResult.getLastLocation().toString());
        Timber.d("Looper from service: %s", Looper.myLooper().toString());
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_MILLISECONDS);
        locationRequest.setFastestInterval(UPDATE_INTERVAL_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    public void startRecording(Looper looper) {
        loadPreferencesAndRegisterListener();
        Track t = track.getValue();
        if (t == null) {
            t = new Track();
        }
        t.setState(Track.STATE_RECORDING);
        t.setStartTime(SystemClock.elapsedRealtime());
        track.postValue(t);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, looper)
            .addOnFailureListener(e -> {
                //TODO handle failure
            });
    }

    public void updateTime() {
        Track t = track.getValue();
        if (t != null) {
            elapsedTime.postValue(t.getTotalTime());
        }
    }

    public void stopRecording() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Track t = track.getValue();
        t.setEndTime(SystemClock.elapsedRealtime());
        t.setState(Track.STATE_PAUSE);
        track.postValue(t);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    public LiveData<Track> getTrackLiveData() {
        return track;
    }

    public LiveData<Long> getElapsedTimeLiveData() {
        return elapsedTime;
    }

    private void loadPreferencesAndRegisterListener() {
        //default value is 1s
        timeInterval = sharedPreferences.getInt(Constants.PREFERENCE_TIME_INTERVAL, 1000);
        //default value is 10meters
        distanceInterval = sharedPreferences.getInt(Constants.PREFERENCE_DISTANCE_INTERVAL, 10);
        //default value is 50meters
        gpsAccuracy = sharedPreferences.getInt(Constants.PREFERENCE_GPS_ACCURACY, 50);

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PREFERENCE_DISTANCE_INTERVAL: {
                distanceInterval = sharedPreferences.getInt(Constants.PREFERENCE_DISTANCE_INTERVAL, 10);
                break;
            }
            case Constants.PREFERENCE_TIME_INTERVAL: {
                timeInterval = sharedPreferences.getInt(Constants.PREFERENCE_TIME_INTERVAL, 1000);
                break;
            }
            case Constants.PREFERENCE_GPS_ACCURACY: {
                gpsAccuracy = sharedPreferences.getInt(Constants.PREFERENCE_GPS_ACCURACY, 50);
                break;
            }
        }
    }
}

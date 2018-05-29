package hu.am2.myway.location.model;

import android.location.Location;
import android.os.SystemClock;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Track {
    public static final int STATE_RECORDING = 0;
    public static final int STATE_PAUSE = 1;
    public static final int STATE_WAITING_FOR_SIGNAL = 2;

    private Location lastLocation;
    private List<Location> track = new ArrayList<>();
    private List<LatLng> path = new ArrayList<>();
    private long totalTime;
    private long movingTime;
    private long startTime;
    private long endTime;
    private long lastLocationTime = -1;
    private int state;
    private float totalDistance;
    private float avgSpeed;
    private float avgMovingSpeed;
    private float maxSpeed;
    private double maxAltitude;
    private double minAltitude;

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location currentLocation) {

        //save the time passed since either start, or the last location
        long currentTime = TimeUnit.NANOSECONDS.toMillis(currentLocation.getElapsedRealtimeNanos());
        long t = currentTime - lastLocationTime;
        totalTime += Math.max(0, t);
        lastLocationTime = currentTime;


        if (lastLocation != null) {
            totalDistance += currentLocation.distanceTo(lastLocation);
            if (currentLocation.hasSpeed()) {
                movingTime += t;
            }
            avgSpeed = totalDistance / totalTime;
            avgMovingSpeed = totalDistance / movingTime;
        }
        if (currentLocation.getSpeed() > maxSpeed) {
            maxSpeed = currentLocation.getSpeed();
        }
        if (currentLocation.hasAltitude()) {
            double alt = currentLocation.getAltitude();
            if (alt > maxAltitude) {
                maxAltitude = alt;
            }
            if (alt < minAltitude) {
                minAltitude = alt;
            }
        }

        this.lastLocation = currentLocation;

        track.add(lastLocation);
        path.add(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
    }

    public List<Location> getTrack() {
        return track;
    }

    public void setTrack(List<Location> track) {
        this.track = track;
    }

    public List<LatLng> getPath() {
        return path;
    }

    public void setPath(List<LatLng> path) {
        this.path = path;
    }

    public long getTotalTime() {
        if (lastLocationTime == -1) {
            return totalTime;
        }

        long t = SystemClock.elapsedRealtime() - lastLocationTime;

        return totalTime + Math.max(0, t);
    }

    public void setTotalTime() {
        totalTime = getTotalTime();
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
        lastLocationTime = startTime;
    }

    public long getLastLocationTime() {
        return lastLocationTime;
    }

    public void setLastLocationTime(long lastLocationTime) {
        this.lastLocationTime = lastLocationTime;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public long getMovingTime() {
        return movingTime;
    }

    public void setMovingTime(long movingTime) {
        this.movingTime = movingTime;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(float totalDistance) {
        this.totalDistance = totalDistance;
    }

    public float getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(float avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public float getAvgMovingSpeed() {
        return avgMovingSpeed;
    }

    public void setAvgMovingSpeed(float avgMovingSpeed) {
        this.avgMovingSpeed = avgMovingSpeed;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public double getMaxAltitude() {
        return maxAltitude;
    }

    public void setMaxAltitude(double maxAltitude) {
        this.maxAltitude = maxAltitude;
    }

    public double getMinAltitude() {
        return minAltitude;
    }

    public void setMinAltitude(double minAltitude) {
        this.minAltitude = minAltitude;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
        long t = SystemClock.elapsedRealtime() - lastLocationTime;
        totalTime += Math.max(0, t);
        lastLocationTime = -1;
    }
}

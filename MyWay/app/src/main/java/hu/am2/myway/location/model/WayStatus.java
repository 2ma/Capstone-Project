package hu.am2.myway.location.model;

import android.location.Location;
import android.os.SystemClock;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WayStatus {
    public static final int STATE_RECORDING = 0;
    public static final int STATE_PAUSE = 1;
    public static final int STATE_STOP = 2;

    public static final int STATE_WAITING_FOR_SIGNAL = 4;

    private Location lastLocation;
    private long lastStartTime = -1;
    private int state;

    private Way way;
    private List<LatLng> wayPoints = new ArrayList<>();

    public WayStatus() {
    }

    public WayStatus(WayWithWayPoints wayWithWayPoints) {
        way = wayWithWayPoints.getWay();
        List<WayPoint> wp = wayWithWayPoints.getWayPoints();
        if (wp != null && wp.size() > 0) {
            for (int i = 0, z = wp.size(); i < z; i++) {
                WayPoint tempWp = wp.get(i);
                wayPoints.add(new LatLng(tempWp.getLatitude(), tempWp.getLongitude()));
            }
            Location l = new Location("lastLocation");
            WayPoint last = wp.get(wp.size() - 1);
            l.setLatitude(last.getLatitude());
            l.setLongitude(last.getLongitude());
            lastLocation = l;
        }
    }

    public void setEndTime() {
        if (lastStartTime != -1) {
            long t = SystemClock.elapsedRealtime() - lastStartTime;
            way.setTotalTime(way.getTotalTime() + Math.max(0, t));
        }
        way.setEndTime(System.currentTimeMillis());
        lastStartTime = -1;
    }

    public long calculateTotalTime() {
        if (lastStartTime == -1) {
            return way.getTotalTime();
        }
        long t = SystemClock.elapsedRealtime() - lastStartTime;

        return way.getTotalTime() + Math.max(0, t);
    }

    public void updateCurrentLocation(Location currentLocation) {
        //save the time passed since either start, or the last location
        long currentTime = TimeUnit.NANOSECONDS.toMillis(currentLocation.getElapsedRealtimeNanos());
        long t = currentTime - lastStartTime;
        way.setTotalTime(way.getTotalTime() + Math.max(0, t));
        lastStartTime = currentTime;

        if (lastLocation != null) {
            float totalDistance = way.getTotalDistance();
            long totalTime = way.getTotalTime();
            long movingTime = way.getMovingTime();
            totalDistance += currentLocation.distanceTo(lastLocation);
            way.setTotalDistance(totalDistance);
            if (currentLocation.hasSpeed()) {
                way.setMovingTime(movingTime + t);
            }
            way.setAvgSpeed(totalDistance / totalTime);
            way.setAvgMovingSpeed(totalDistance / movingTime);
        }
        checkMaxSpeed(currentLocation);
        checkAltitude(currentLocation);
        this.lastLocation = currentLocation;
        wayPoints.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
    }

    private void checkAltitude(Location location) {
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

    private void checkMaxSpeed(Location location) {
        if (location.getSpeed() > way.getMaxSpeed()) {
            way.setMaxSpeed(location.getSpeed());
        }
    }

    public void updateFirstLocation(Location location) {
        lastLocation = location;
        lastStartTime = TimeUnit.NANOSECONDS.toMillis(location.getElapsedRealtimeNanos());
        checkMaxSpeed(location);
        checkAltitude(location);
        wayPoints.add(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    public long getLastStartTime() {
        return lastStartTime;
    }

    public void setLastStartTime(long lastStartTime) {
        this.lastStartTime = lastStartTime;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Way getWay() {
        return way;
    }

    public void setWay(Way way) {
        this.way = way;
    }

    public List<LatLng> getWayPoints() {
        return wayPoints;
    }

    public void setWayPoints(List<LatLng> wayPoints) {
        this.wayPoints = wayPoints;
    }
}

package hu.am2.myway.location.model;

import android.location.Location;
import android.os.SystemClock;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class WayStatus {

    private Location lastLocation;
    private long lastRecordedTime = -1;

    private Way way;
    private final List<LatLng> wayPoints = new ArrayList<>();

    public WayStatus() {
    }

    public void initWayStatus(WayWithWayPoints wayWithWayPoints) {
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

    public void clear() {
        lastLocation = null;
        lastRecordedTime = -1;
        way = null;
        wayPoints.clear();
    }

    public void setEndTime() {
        if (lastRecordedTime != -1) {
            long t = SystemClock.elapsedRealtime() - lastRecordedTime;
            way.setTotalTime(way.getTotalTime() + Math.max(0, t));
        }
        way.setEndTime(System.currentTimeMillis());
        lastRecordedTime = -1;
    }

    public void calculateTotalTime(long currentTime) {
        if (lastRecordedTime == -1) {
            return;
        }
        long t = currentTime - lastRecordedTime;
        way.setTotalTime(way.getTotalTime() + Math.max(0, t));
        lastRecordedTime = currentTime;
    }

    public void updateCurrentLocation(Location currentLocation) {
        //save the time passed since either start, or the last location
        long currentTime = SystemClock.elapsedRealtime();
        calculateTotalTime(currentTime);
        lastRecordedTime = currentTime;

        if (lastLocation != null) {
            float totalDistance = way.getTotalDistance();
            long totalTime = way.getTotalTime();
            totalDistance += currentLocation.distanceTo(lastLocation);
            way.setTotalDistance(totalDistance);
            way.setAvgSpeed(totalDistance / (totalTime / 1000));
        }
        checkMaxSpeed(currentLocation);
        checkAltitude(currentLocation);
        lastLocation = currentLocation;
        wayPoints.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
    }

    private void checkAltitude(Location location) {
        if (lastLocation == null && location.hasAltitude()) {
            way.setMaxAltitude(location.getAltitude());
            way.setMinAltitude(location.getAltitude());
        } else if (location.hasAltitude()) {
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
        if (lastLocation == null && location.hasSpeed()) {
            way.setMaxSpeed(location.getSpeed());
        } else if (location.hasSpeed() && location.getSpeed() > way.getMaxSpeed()) {
            way.setMaxSpeed(location.getSpeed());
        }
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastRecordedTime(long lastRecordedTime) {
        this.lastRecordedTime = lastRecordedTime;
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
}

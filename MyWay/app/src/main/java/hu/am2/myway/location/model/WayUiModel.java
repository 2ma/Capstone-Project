package hu.am2.myway.location.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class WayUiModel {

    private final long totalTime;
    private final float totalDistance;
    private final float avSpeed;
    private final float avgMovingSpeed;
    private final double maxAltitude;
    private final double minAltitude;

    private final List<LatLng> wayPoints;

    public WayUiModel(Way way, List<LatLng> wayPoints) {
        this.totalTime = way.getTotalTime();
        this.totalDistance = way.getTotalDistance();
        this.avSpeed = way.getAvgSpeed();
        this.avgMovingSpeed = way.getAvgMovingSpeed();
        this.maxAltitude = way.getMaxAltitude();
        this.minAltitude = way.getMinAltitude();
        this.wayPoints = wayPoints;

    }

    public long getTotalTime() {
        return totalTime;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public float getAvSpeed() {
        return avSpeed;
    }

    public float getAvgMovingSpeed() {
        return avgMovingSpeed;
    }

    public double getMaxAltitude() {
        return maxAltitude;
    }

    public double getMinAltitude() {
        return minAltitude;
    }

    public List<LatLng> getWayPoints() {
        return wayPoints;
    }
}

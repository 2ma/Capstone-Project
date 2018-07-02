package hu.am2.myway.location.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class WayUiModel {

    //in km
    private final float totalDistance;
    //in km/h
    private final float avgSpeed;
    //max speed
    private final float maxSpeed;
    //in meter
    private final double maxAltitude;
    //in meter
    private final double minAltitude;
    //list of way sections
    private final List<List<LatLng>> waySegments;

    public WayUiModel(Way way, List<List<LatLng>> waySegments) {
        this.totalDistance = way.getTotalDistance() / 1000;
        this.avgSpeed = way.getAvgSpeed() * 3.6f;
        this.maxAltitude = way.getMaxAltitude();
        this.minAltitude = way.getMinAltitude();
        this.waySegments = waySegments;
        this.maxSpeed = way.getMaxSpeed();
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public float getAvgSpeed() {
        return avgSpeed;
    }

    public double getMaxAltitude() {
        return maxAltitude;
    }

    public double getMinAltitude() {
        return minAltitude;
    }

    public List<List<LatLng>> getWaySegments() {
        return waySegments;
    }
}

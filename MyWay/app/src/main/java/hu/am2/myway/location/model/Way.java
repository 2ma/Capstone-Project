package hu.am2.myway.location.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "way")
public class Way {

    private String wayName;
    private long totalTime;
    private long startTime;
    private long endTime;
    private float totalDistance;
    private float avgSpeed;
    private float maxSpeed;
    private double maxAltitude;
    private double minAltitude;
    @PrimaryKey(autoGenerate = true)
    private long id;

    public Way() {
    }

    public Way getWayCopy() {
        return new Way(
            wayName,
            totalTime,
            startTime,
            endTime,
            totalDistance,
            avgSpeed,
            maxSpeed,
            maxAltitude,
            minAltitude,
            id);
    }

    private Way(String wayName, long totalTime, long startTime, long endTime, float totalDistance, float avgSpeed, float maxSpeed,
                double maxAltitude, double minAltitude, long id) {
        this.wayName = wayName;
        this.totalTime = totalTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalDistance = totalDistance;
        this.avgSpeed = avgSpeed;
        this.maxSpeed = maxSpeed;
        this.maxAltitude = maxAltitude;
        this.minAltitude = minAltitude;
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }


    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
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
    }

    public String getWayName() {
        return wayName;
    }

    public void setWayName(String wayName) {
        this.wayName = wayName;
    }
}

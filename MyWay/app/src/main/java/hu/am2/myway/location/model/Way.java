package hu.am2.myway.location.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "way")
public class Way {

    private String wayName;
    private long totalTime;
    private long movingTime;
    private long startTime;
    private long endTime;
    private float totalDistance;
    private float avgSpeed;
    private float avgMovingSpeed;
    private float maxSpeed;
    private double maxAltitude;
    private double minAltitude;
    @PrimaryKey(autoGenerate = true)
    private long id;

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
    }

    public String getWayName() {
        return wayName;
    }

    public void setWayName(String wayName) {
        this.wayName = wayName;
    }
}

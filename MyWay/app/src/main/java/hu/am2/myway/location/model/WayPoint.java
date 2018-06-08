package hu.am2.myway.location.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.location.Location;

@Entity(tableName = "way_point", foreignKeys = @ForeignKey(entity = Way.class,
    parentColumns = "id",
    childColumns = "wayId",
    onDelete = ForeignKey.CASCADE))
public class WayPoint {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private double latitude;
    private double longitude;
    private long time;
    private float speed;
    private float accuracy;
    private long wayId;

    public WayPoint() {
    }

    @Ignore
    public WayPoint(Location location, long wayId) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        time = location.getTime();
        if (location.hasSpeed()) {
            speed = location.getSpeed();
        }
        if (location.hasAccuracy()) {
            accuracy = location.getAccuracy();
        }
        this.wayId = wayId;
    }

    public long getWayId() {
        return wayId;
    }

    public void setWayId(long wayId) {
        this.wayId = wayId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }
}

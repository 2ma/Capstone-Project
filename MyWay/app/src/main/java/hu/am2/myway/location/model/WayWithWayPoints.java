package hu.am2.myway.location.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;

public class WayWithWayPoints {

    @Embedded
    private Way way;

    @Relation(parentColumn = "id", entityColumn = "wayId")
    private List<WayPoint> wayPoints;

    public Way getWay() {
        return way;
    }

    public void setWay(Way way) {
        this.way = way;
    }

    public List<WayPoint> getWayPoints() {
        return wayPoints;
    }

    public void setWayPoints(List<WayPoint> wayPoints) {
        this.wayPoints = wayPoints;
    }
}

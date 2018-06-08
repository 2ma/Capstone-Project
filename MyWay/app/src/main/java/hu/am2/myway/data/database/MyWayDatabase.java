package hu.am2.myway.data.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import hu.am2.myway.data.dao.WayDao;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayPoint;

@Database(entities = {Way.class, WayPoint.class}, version = 1)
public abstract class MyWayDatabase extends RoomDatabase {
    public abstract WayDao wayDao();
}

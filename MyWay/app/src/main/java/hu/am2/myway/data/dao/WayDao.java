package hu.am2.myway.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import java.util.List;

import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayPoint;
import hu.am2.myway.location.model.WayWithWayPoints;

@Dao
public interface WayDao {

    @Query("SELECT * FROM way WHERE id=:id")
    Way getWayForId(long id);

    @Query("SELECT * FROM way WHERE id=:id")
    LiveData<Way> getWayLiveDataForId(long id);

    @Query("SELECT * FROM way")
    List<Way> getAllWays();

    @Query("SELECT * FROM way")
    LiveData<List<Way>> getAllWaysAsync();

    @Transaction
    @Query("SELECT * FROM way WHERE id=:id")
    WayWithWayPoints getWayWithWayPointsForId(long id);

    @Transaction
    @Query("SELECT * FROM way WHERE id=:id")
    LiveData<WayWithWayPoints> getWayWithWayPointsLiveDataForId(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertWay(Way way);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateWay(Way way);

    @Delete
    void deleteWay(Way way);

    @Query("SELECT * FROM way_point WHERE wayId=:wayId")
    List<WayPoint> getAllWayPointsForWayId(long wayId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWayPoint(WayPoint wayPoint);

    @Query("DELETE FROM way_point WHERE wayId=:wayId")
    void deleteAllWayPointsForWayId(long wayId);
}

package hu.am2.myway.data;

import android.arch.lifecycle.LiveData;
import android.support.annotation.WorkerThread;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import hu.am2.myway.AppExecutors;
import hu.am2.myway.data.dao.WayDao;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayPoint;
import hu.am2.myway.location.model.WayWithWayPoints;

@Singleton
public class Repository {

    private final WayDao wayDao;
    private final AppExecutors executors;

    @Inject
    public Repository(WayDao wayDao, AppExecutors executors) {
        this.wayDao = wayDao;
        this.executors = executors;
    }

    @WorkerThread
    public long insertWay(Way way) {
        return wayDao.insertWay(way);
    }

    @WorkerThread
    public Way getWayForId(long id) {
        return wayDao.getWayForId(id);
    }

    public LiveData<Way> getWayLiveDataForId(long id) {
        return wayDao.getWayLiveDataForId(id);
    }

    @WorkerThread
    public WayWithWayPoints getWayWithWayPointsForId(long id) {
        return wayDao.getWayWithWayPointsForId(id);
    }

    @WorkerThread
    public LiveData<WayWithWayPoints> getWayWithWayPointsLiveDataForId(long id) {
        return wayDao.getWayWithWayPointsLiveDataForId(id);
    }

    public LiveData<List<Way>> getAllWaysLiveData() {
        return wayDao.getAllWaysAsync();
    }

    public void updateWay(Way way) {
        executors.getDiskIO().execute(() -> wayDao.updateWay(way));
    }

    public void deleteWay(Way way) {
        executors.getDiskIO().execute(() -> wayDao.deleteWay(way));
    }

    public void insertWayPoint(WayPoint wayPoint) {
        executors.getDiskIO().execute(() -> wayDao.insertWayPoint(wayPoint));
    }

    public LiveData<List<Way>> getWaysForQuery(String query) {
        return wayDao.getAllWaysForQuery(query);
    }

    public WayPoint getWayWithLastLocationForWayId(long wayId) {
        return wayDao.getLastWayPointForWayId(wayId);
    }
}

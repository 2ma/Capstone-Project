package hu.am2.myway.data.dao;

import android.arch.core.executor.testing.CountingTaskExecutorRule;
import android.arch.persistence.room.Room;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import hu.am2.myway.LiveDataTestUtil;
import hu.am2.myway.data.database.MyWayDatabase;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayPoint;
import hu.am2.myway.location.model.WayWithWayPoints;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class WayDaoTest {

    @Rule
    public CountingTaskExecutorRule countingTaskExecutorRule = new CountingTaskExecutorRule();

    private WayDao wayDao;
    private MyWayDatabase database;

    @Before
    public void init() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(), MyWayDatabase.class).build();
        wayDao = database.wayDao();
    }

    @After
    public void closeDb() {
        database.close();
    }

    @Test
    public void checkEmptyDbTest() {
        List<Way> result = wayDao.getAllWays();
        assertThat(result.size(), is(0));
    }

    @Test
    public void getWayForIdTest() {
        Way w = new Way();
        w.setStartTime(3);
        w.setEndTime(4);
        w.setTotalDistance(45.3f);
        long id = wayDao.insertWay(w);

        Way result = wayDao.getWayForId(id);

        assertNotNull(result);
        assertThat(result.getStartTime(), is(3L));
        assertThat(result.getEndTime(), is(4L));
        assertThat(result.getTotalDistance(), is(45.3f));

    }

    @Test
    public void getWayLiveDataForIdTest() {
        Way w = new Way();
        w.setStartTime(3);
        w.setEndTime(4);
        w.setTotalDistance(45.3f);
        w.setWayName("lovely way");
        long id = wayDao.insertWay(w);

        Way way = LiveDataTestUtil.getValue(wayDao.getWayLiveDataForId(id));

        assertNotNull(way);
        assertThat(way.getStartTime(), is(3L));
        assertThat(way.getEndTime(), is(4L));
        assertThat(way.getTotalDistance(), is(45.3f));
        assertThat(way.getWayName(), is("lovely way"));
    }

    @Test
    public void getAllWaysTest() {
        for (int i = 0; i < 10; i++) {
            Way w = new Way();
            w.setStartTime(i);
            w.setEndTime(i + 10);
            w.setTotalDistance(45.3f + i);
            wayDao.insertWay(w);
        }

        List<Way> result = wayDao.getAllWays();

        assertThat(result.size(), is(10));
    }

    @Test
    public void getAllWaysForQueryTest() {
        for (int i = 0; i < 10; i++) {
            Way w = new Way();
            w.setStartTime(i);
            w.setEndTime(i + 10);
            w.setWayName("very lovely" + i);
            w.setTotalDistance(45.3f + i);
            wayDao.insertWay(w);
            Way w2 = new Way();
            w2.setStartTime(i);
            w2.setEndTime(i + 10);
            w2.setWayName("just a way" + i);
            w2.setTotalDistance(45.3f + i);
            wayDao.insertWay(w2);
        }

        List<Way> result = LiveDataTestUtil.getValue(wayDao.getAllWaysForQuery("lovely"));

        assertThat(result.size(), is(10));
        for (Way rWay : result) {
            assertThat(rWay.getWayName(), containsString("lovely"));
        }
    }

    @Test
    public void getAllWaysAsyncTest() {
        for (int i = 0; i < 10; i++) {
            Way w = new Way();
            w.setStartTime(i);
            w.setEndTime(i + 10);
            w.setTotalDistance(45.3f + i);
            w.setWayName("lovely way" + i);
            wayDao.insertWay(w);
        }

        List<Way> result = LiveDataTestUtil.getValue(wayDao.getAllWaysAsync());

        assertThat(result.size(), is(10));
    }

    @Test
    public void getWayWithWayPointsForIdTest() {
        long[] id = new long[2];
        for (int i = 0; i < 2; i++) {
            Way w = new Way();
            w.setStartTime(i);
            w.setEndTime(i + 10);
            w.setTotalDistance(45.3f + i);
            id[i] = wayDao.insertWay(w);
        }
        for (int i = 0; i < 30; i++) {
            WayPoint wayPoint = new WayPoint();
            wayPoint.setWayId(i % 2 == 0 ? id[0] : id[1]);
            wayPoint.setLongitude(i + 10L);
            wayPoint.setLatitude(i + 20L);
            wayDao.insertWayPoint(wayPoint);
        }

        WayWithWayPoints result = wayDao.getWayWithWayPointsForId(id[0]);

        assertNotNull(result);
        assertThat(result.getWay().getId(), is(id[0]));
        assertThat(result.getWayPoints().size(), is(15));

    }

    @Test
    public void getWayWithWayPointsLiveDataForIdTest() {
        long[] id = new long[2];
        for (int i = 0; i < 2; i++) {
            Way w = new Way();
            w.setStartTime(i);
            w.setEndTime(i + 10);
            w.setTotalDistance(45.3f + i);
            id[i] = wayDao.insertWay(w);
        }
        for (int i = 0; i < 30; i++) {
            WayPoint wayPoint = new WayPoint();
            wayPoint.setWayId(i % 2 == 0 ? id[0] : id[1]);
            wayPoint.setLongitude(i + 10L);
            wayPoint.setLatitude(i + 20L);
            wayDao.insertWayPoint(wayPoint);
        }

        WayWithWayPoints result = LiveDataTestUtil.getValue(wayDao.getWayWithWayPointsLiveDataForId(id[0]));

        assertNotNull(result);
        assertThat(result.getWay().getId(), is(id[0]));
        assertThat(result.getWayPoints().size(), is(15));

    }



    @Test
    public void wayPointCascadeDeleteTest() {
        long[] id = new long[2];
        for (int i = 0; i < 2; i++) {
            Way w = new Way();
            w.setStartTime(i);
            w.setEndTime(i + 10);
            w.setTotalDistance(45.3f + i);
            id[i] = wayDao.insertWay(w);
        }
        for (int i = 0; i < 30; i++) {
            WayPoint wayPoint = new WayPoint();
            wayPoint.setWayId(i % 2 == 0 ? id[0] : id[1]);
            wayPoint.setLongitude(i + 10L);
            wayPoint.setLatitude(i + 20L);
            wayDao.insertWayPoint(wayPoint);
        }

        WayWithWayPoints result = wayDao.getWayWithWayPointsForId(id[0]);

        assertNotNull(result);
        assertThat(result.getWay().getId(), is(id[0]));
        assertThat(result.getWayPoints().size(), is(15));

        wayDao.deleteWay(result.getWay());

        List<WayPoint> result2 = wayDao.getAllWayPointsForWayId(result.getWay().getId());

        assertThat(result2.size(), is(0));
    }

    @Test
    public void insertWyTest() {
        Way w = new Way();
        w.setStartTime(300);
        w.setEndTime(500);
        long id = wayDao.insertWay(w);

        Way result = wayDao.getWayForId(id);

        assertNotNull(result);
        assertThat(result.getStartTime(), is(300L));
        assertThat(result.getEndTime(), is(500L));

    }

    @Test
    public void updateWayTest() {
        Way w = new Way();
        w.setStartTime(3);
        w.setEndTime(4);
        w.setTotalDistance(45.3f);
        long id = wayDao.insertWay(w);

        w.setId(id);
        w.setStartTime(100);
        w.setEndTime(150);
        wayDao.updateWay(w);

        Way result = wayDao.getWayForId(id);

        assertNotNull(result);
        assertThat(result.getStartTime(), is(100L));
        assertThat(result.getEndTime(), is(150L));
        assertThat(result.getTotalDistance(), is(45.3f));
    }

    @Test
    public void deleteWayTest() {
        Way w = new Way();
        w.setStartTime(3);
        w.setEndTime(4);
        w.setTotalDistance(45.3f);
        long id = wayDao.insertWay(w);

        w.setId(id);

        wayDao.deleteWay(w);

        List<Way> result = wayDao.getAllWays();

        assertThat(result.size(), is(0));
    }

    @Test
    public void getAllWayPointsForIdTest() {
        for (int i = 0; i < 10; i++) {
            WayPoint wp = new WayPoint();
            wp.setLatitude(i);
            wp.setLongitude(i);
            wp.setWayId(3);
            wayDao.insertWayPoint(wp);
        }

        List<WayPoint> result = wayDao.getAllWayPointsForWayId(3);

        assertThat(result.size(), is(10));
    }

    @Test
    public void deleteAllWayPointsForWayIdTest() {
        for (int i = 0; i < 10; i++) {
            WayPoint wp = new WayPoint();
            wp.setLatitude(i);
            wp.setLongitude(i);
            wp.setWayId(3);
            wayDao.insertWayPoint(wp);
        }

        wayDao.deleteAllWayPointsForWayId(3);

        List<WayPoint> result = wayDao.getAllWayPointsForWayId(3);

        assertThat(result.size(), is(0));
    }
}

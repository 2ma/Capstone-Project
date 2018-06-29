package hu.am2.myway.ui.map;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import hu.am2.myway.Constants;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.model.WayPoint;
import hu.am2.myway.location.model.WayUiModel;
import hu.am2.myway.location.model.WayWithWayPoints;

public class MapViewModel extends ViewModel implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final SharedPreferences sharedPreferences;

    private final MediatorLiveData<WayUiModel> wayWithWayPointsLiveData = new MediatorLiveData<>();
    private MutableLiveData<Long> wayId = new MutableLiveData<>();
    private LiveData<WayWithWayPoints> temp;

    @Inject
    public MapViewModel(Repository repository, SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        wayId.setValue(sharedPreferences.getLong(Constants.PREF_WAY_ID, -1));
        wayWithWayPointsLiveData.addSource(wayId, id -> {
            if (temp != null) {
                wayWithWayPointsLiveData.removeSource(temp);
                temp = null;
            }
            if (id == null || id == -1) {
                wayWithWayPointsLiveData.postValue(null);
            } else {
                temp = repository.getWayWithWayPointsLiveDataForId(id);
                wayWithWayPointsLiveData.addSource(repository.getWayWithWayPointsLiveDataForId(id), wayWithWayPoints -> {
                    if (wayWithWayPoints != null && wayWithWayPoints.getWayPoints() != null) {
                        List<List<LatLng>> segments = new ArrayList<>();
                        List<WayPoint> wayPoints = wayWithWayPoints.getWayPoints();
                        int size = wayPoints.size();
                        if (wayPoints.size() > 0) {
                            List<LatLng> waySegment = new ArrayList<>();
                            for (int i = 0; i < size; i++) {
                                WayPoint w = wayPoints.get(i);
                                if (w.getLatitude() == Constants.WAY_END_COORDINATE) {
                                    segments.add(waySegment);
                                    waySegment = new ArrayList<>();
                                } else {
                                    waySegment.add(new LatLng(w.getLatitude(), w.getLongitude()));
                                }
                            }
                        }
                        wayWithWayPointsLiveData.postValue(new WayUiModel(wayWithWayPoints.getWay(), segments));
                    }
                });
            }
        });
    }

    LiveData<WayUiModel> getWayUiModelLiveData() {
        return wayWithWayPointsLiveData;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Constants.PREF_WAY_ID.equals(key)) {
            wayId.postValue(sharedPreferences.getLong(Constants.PREF_WAY_ID, -1));
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
}

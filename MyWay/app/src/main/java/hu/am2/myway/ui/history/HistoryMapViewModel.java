package hu.am2.myway.ui.history;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;

import javax.inject.Inject;

import hu.am2.myway.data.Repository;
import hu.am2.myway.location.model.WayWithWayPoints;

public class HistoryMapViewModel extends ViewModel {

    private Repository repository;

    private MutableLiveData<Long> wayId = new MutableLiveData<>();
    private LiveData<WayWithWayPoints> wayWitWayPoints;

    @Inject
    public HistoryMapViewModel(Repository repository) {
        this.repository = repository;
        wayWitWayPoints = Transformations.switchMap(wayId, repository::getWayWithWayPointsLiveDataForId);
    }

    public void setWayId(long id) {
        if (wayId.getValue() == null || wayId.getValue() != id) {
            wayId.setValue(id);
        }
    }

    public LiveData<WayWithWayPoints> getWayWitWayPoints() {
        return wayWitWayPoints;
    }
}

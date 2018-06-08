package hu.am2.myway.ui.history;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;

import java.util.List;

import javax.inject.Inject;

import hu.am2.myway.AppExecutors;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayWithWayPoints;


public class HistoryViewModel extends ViewModel {

    static final int STATE_LIST = 0;
    static final int STATE_MAP = 1;

    private Repository repository;
    private AppExecutors executors;

    private MutableLiveData<Long> wayId = new MutableLiveData<>();
    private LiveData<WayWithWayPoints> wayWitWayPoints;
    private MutableLiveData<Integer> historyState = new MutableLiveData<>();

    @Inject
    public HistoryViewModel(Repository repository, AppExecutors executors) {
        this.repository = repository;
        this.executors = executors;
        historyState.setValue(STATE_LIST);
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

    public LiveData<List<Way>> getAllWays() {
        return repository.getAllWaysLiveData();
    }

    public LiveData<Integer> getHistoryState() {
        return historyState;
    }

    public void setHistoryState(int state) {
        if (historyState.getValue() != state) {
            historyState.setValue(state);
        }
    }

}

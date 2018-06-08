package hu.am2.myway.ui.saveway;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;

import javax.inject.Inject;

import hu.am2.myway.AppExecutors;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.model.Way;

public class SaveWayViewModel extends ViewModel {

    private Repository repository;
    private AppExecutors appExecutors;

    private LiveData<Way> way;
    private MutableLiveData<Long> wayId = new MutableLiveData<>();

    @Inject
    public SaveWayViewModel(Repository repository, AppExecutors appExecutors) {
        this.repository = repository;
        this.appExecutors = appExecutors;
        way = Transformations.switchMap(wayId, repository::getWayLiveDataForId);
    }

    public void setWayId(long id) {
        if (wayId.getValue() == null || wayId.getValue() != id) {
            wayId.setValue(id);
        }
    }

    public LiveData<Way> getWay() {
        return way;
    }

    public void saveWay(String wayName) {
        Way w = way.getValue();
        if (w != null) {
            w.setWayName(wayName);
            appExecutors.getDiskIO().execute(() -> repository.updateWay(w));
        }
    }
}

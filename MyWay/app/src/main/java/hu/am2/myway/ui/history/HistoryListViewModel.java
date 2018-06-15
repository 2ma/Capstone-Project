package hu.am2.myway.ui.history;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import java.util.List;

import javax.inject.Inject;

import hu.am2.myway.AppExecutors;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.model.Way;


public class HistoryListViewModel extends ViewModel {

    private Repository repository;
    private AppExecutors executors;

    private LiveData<List<Way>> ways;

    @Inject
    public HistoryListViewModel(Repository repository, AppExecutors executors) {
        this.repository = repository;
        this.executors = executors;
        ways = repository.getAllWaysLiveData();
    }


    public LiveData<List<Way>> getAllWays() {
        return ways;
    }


    public void deleteWay(Way way) {
        executors.getDiskIO().execute(() -> repository.deleteWay(way));
    }
}

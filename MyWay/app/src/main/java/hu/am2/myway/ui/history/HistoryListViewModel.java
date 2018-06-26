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


public class HistoryListViewModel extends ViewModel {

    private final Repository repository;
    private final AppExecutors executors;

    private final LiveData<List<Way>> ways;

    private final MutableLiveData<String> query = new MutableLiveData<>();

    @Inject
    public HistoryListViewModel(Repository repository, AppExecutors executors) {
        this.repository = repository;
        this.executors = executors;
        query.setValue("");
        ways = Transformations.switchMap(query, q -> {
            if (q.trim().length() > 0) {
                return repository.getWaysForQuery(q);
            } else {
                return repository.getAllWaysLiveData();
            }
        });
    }


    public LiveData<List<Way>> getAllWays() {
        return ways;
    }


    public void deleteWay(Way way) {
        executors.getDiskIO().execute(() -> repository.deleteWay(way));
    }

    public void searchHistory(String q) {
        query.setValue(q);
    }
}

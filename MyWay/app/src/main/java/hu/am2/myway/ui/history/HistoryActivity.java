package hu.am2.myway.ui.history;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import hu.am2.myway.R;
import hu.am2.myway.data.Repository;

public class HistoryActivity extends AppCompatActivity implements HasSupportFragmentInjector {


    @Inject
    DispatchingAndroidInjector<Fragment> supportInjectionFragment;

    @Inject
    Repository repository;

    @BindView(R.id.bottomNavigation)
    BottomNavigationView bottomNavigationView;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private HistoryViewModel viewModel;

    private static final String TAG = "HistoryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ButterKnife.bind(this);

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(HistoryViewModel.class);

        viewModel.getHistoryState().observe(this, this::changeState);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_history_list: {
                    viewModel.setHistoryState(HistoryViewModel.STATE_LIST);
                    return true;
                }
                case R.id.menu_history_map: {
                    viewModel.setHistoryState(HistoryViewModel.STATE_MAP);
                    return true;
                }
                default:
                    return false;
            }
        });

        if (savedInstanceState == null) {
            addHistoryFragment();
        }
    }

    private void changeState(Integer state) {
        if (state == HistoryViewModel.STATE_LIST) {
            if (getSupportFragmentManager().findFragmentByTag("history") == null) {
                addHistoryFragment();
            }
        } else if (state == HistoryViewModel.STATE_MAP) {
            if (getSupportFragmentManager().findFragmentByTag("map") == null) {
                addMapFragment();
            }
        }
    }

    private void addMapFragment() {
        HistoryMapFragment historyMapFragment = new HistoryMapFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.historyContainer, historyMapFragment, "map").commit();
    }

    private void addHistoryFragment() {
        HistoryListFragment historyListFragment = new HistoryListFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.historyContainer, historyListFragment, "history").commit();
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return supportInjectionFragment;
    }
}

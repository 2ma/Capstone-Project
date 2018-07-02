package hu.am2.myway.ui.history;

import android.app.ActivityOptions;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.ImageView;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.AndroidInjection;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.location.model.Way;

public class HistoryListActivity extends AppCompatActivity implements HistoryAdapter.HistoryClickListener {

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.searchView)
    SearchView searchView;

    @BindView(R.id.historyList)
    RecyclerView historyList;

    @BindView(R.id.emptyView)
    ImageView emptyView;

    private HistoryAdapter adapter;

    private HistoryListViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_list);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        searchView.setLayoutParams(new Toolbar.LayoutParams(Gravity.END));

        historyList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(getResources(), historyList, emptyView, this);
        historyList.setAdapter(adapter);
        ItemTouchHelper.SimpleCallback callback = new SwipeToDeleteCallback(this) {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                Way way = adapter.getWay(viewHolder.getAdapterPosition());
                viewModel.deleteWay(way);
            }
        };

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.trim().length() > 0) {
                    viewModel.searchHistory(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    viewModel.searchHistory("");
                }
                return false;
            }
        });



        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(historyList);

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(HistoryListViewModel.class);

        viewModel.getAllWays().observe(this, this::displayWayList);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayWayList(List<Way> ways) {
        adapter.setWays(ways);
    }

    @Override
    public void onItemClicked(long id) {
        Intent intent = new Intent(this, HistoryMapActivity.class);
        intent.putExtra(Constants.EXTRA_WAY_ID, id);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Bundle activityOptions = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
            startActivity(intent, activityOptions);
        } else {
            startActivity(intent);
        }
    }
}

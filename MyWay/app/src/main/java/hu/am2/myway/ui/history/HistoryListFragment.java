package hu.am2.myway.ui.history;


import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.AndroidSupportInjection;
import hu.am2.myway.R;
import hu.am2.myway.location.model.Way;

/**
 * A simple {@link Fragment} subclass.
 */
public class HistoryListFragment extends Fragment implements HistoryAdapter.HistoryClickListener {


    public HistoryListFragment() {
        // Required empty public constructor
    }

    private HistoryViewModel viewModel;

    @BindView(R.id.historyList)
    RecyclerView historyList;

    @BindView(R.id.emptyView)
    TextView emptyView;

    private HistoryAdapter adapter;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(getActivity(), viewModelFactory).get(HistoryViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_list, container, false);
        ButterKnife.bind(this, view);

        historyList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(getResources(), historyList, emptyView, this);
        historyList.setAdapter(adapter);
        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.getAllWays().observe(this, this::displayWays);
    }

    private void displayWays(List<Way> ways) {
        adapter.setWays(ways);
    }

    @Override
    public void onItemClicked(Way way) {
        viewModel.setWayId(way.getId());
        viewModel.setHistoryState(HistoryViewModel.STATE_MAP);
    }
}

package hu.am2.myway.ui.history;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hu.am2.myway.R;
import hu.am2.myway.Utils;
import hu.am2.myway.location.model.Way;

class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<Way> ways = Collections.emptyList();
    private final Resources resources;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timeFormat;
    private final RecyclerView historyList;
    private final TextView emptyView;

    public interface HistoryClickListener {
        void onItemClicked(long id);
    }

    private final HistoryClickListener listener;

    public HistoryAdapter(Resources resources, RecyclerView historyList, TextView emptyView, HistoryClickListener listener) {
        this.resources = resources;
        this.historyList = historyList;
        this.emptyView = emptyView;
        this.listener = listener;
        dateFormat = new SimpleDateFormat("MMM.dd", Locale.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);

        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bindView(ways.get(position));
    }

    @Override
    public int getItemCount() {
        return ways.size();
    }

    public void setWays(List<Way> ways) {
        this.ways = ways == null ? Collections.emptyList() : ways;
        notifyDataSetChanged();
        checkEmpty();
    }

    public Way getWay(int position) {
        return ways.get(position);
    }

    private void checkEmpty() {
        if (getItemCount() > 0) {
            historyList.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            historyList.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView wayNameText;
        private final TextView distanceText;
        private final TextView totalTimeText;
        private final TextView dateText;
        private final TextView timeText;

        HistoryViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            wayNameText = itemView.findViewById(R.id.wayName);
            distanceText = itemView.findViewById(R.id.distance);
            totalTimeText = itemView.findViewById(R.id.totalTime);
            dateText = itemView.findViewById(R.id.date);
            timeText = itemView.findViewById(R.id.time);
        }

        void bindView(Way way) {
            wayNameText.setText(way.getWayName());
            distanceText.setText(resources.getString(R.string.distance_unit, way.getTotalDistance()));
            totalTimeText.setText(Utils.getTimeFromMilliseconds(way.getTotalTime()));
            Date d = new Date(way.getStartTime());
            dateText.setText(dateFormat.format(d));
            timeText.setText(timeFormat.format(d));
        }

        @Override
        public void onClick(View v) {
            listener.onItemClicked(ways.get(getAdapterPosition()).getId());
        }
    }
}

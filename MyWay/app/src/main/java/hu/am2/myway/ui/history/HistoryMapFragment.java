package hu.am2.myway.ui.history;


import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.AndroidSupportInjection;
import hu.am2.myway.R;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayPoint;
import hu.am2.myway.location.model.WayWithWayPoints;

/**
 * A simple {@link Fragment} subclass.
 */
public class HistoryMapFragment extends Fragment implements OnMapReadyCallback {

    @BindView(R.id.mapView)
    MapView mapView;

    private GoogleMap map;
    private Polyline path;
    private Marker startPoint;
    private Marker endPoint;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private HistoryViewModel viewModel;

    public HistoryMapFragment() {
        // Required empty public constructor
    }

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

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        ButterKnife.bind(this, view);

        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this);
        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //TODO decide if this is the best part to start observing
        viewModel.getWayWitWayPoints().observe(this, this::displayMapHistory);
    }

    private void displayMapHistory(WayWithWayPoints wayWithWayPoints) {
        if (path != null) {
            path.remove();
            path = null;
        }
        if (startPoint != null) {
            startPoint.remove();
            startPoint = null;
        }
        if (endPoint != null) {
            endPoint.remove();
            endPoint = null;
        }
        Way way = wayWithWayPoints.getWay();
        List<WayPoint> wayPoints = wayWithWayPoints.getWayPoints();
        int size = wayPoints.size();
        if (size > 0) {
            List<LatLng> poly = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                WayPoint wp = wayPoints.get(i);
                poly.add(new LatLng(wp.getLatitude(), wp.getLongitude()));
            }
            path = map.addPolyline(new PolylineOptions()
                .color(Color.GREEN)
                .width(3)
                .addAll(poly)
            );
            startPoint = map.addMarker(new MarkerOptions()
                .position(poly.get(0)));
            if (size > 1) {
                endPoint = map.addMarker(new MarkerOptions().position(poly.get(size - 1)));
            }
        }
        //TODO setup data
    }

    @Override
    public void onStart() {
        mapView.onStart();
        super.onStart();
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        mapView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }
}

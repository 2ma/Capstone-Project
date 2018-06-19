package hu.am2.myway.ui.history;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.AndroidInjection;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayPoint;
import hu.am2.myway.location.model.WayWithWayPoints;

public class HistoryMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;

    private Polyline path;
    private Marker startPoint;
    private Marker endPoint;

    //details pager layout 1
    TextView speedText;
    TextView timeText;
    TextView distanceText;

    //details pager layout 2
    TextView avgSpeed;
    TextView avgMovingSpeed;
    TextView maxAltitude;
    TextView minAltitude;

    @BindView(R.id.tabDots)
    TabLayout tabLayout;

    @BindView(R.id.detailViewPager)
    ViewPager detailViewPager;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Inject
    ViewModelProvider.Factory factory;

    private HistoryMapViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_map);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setupDetailsPager();

        viewModel = ViewModelProviders.of(this, factory).get(HistoryMapViewModel.class);

        long id = getIntent().getLongExtra(Constants.EXTRA_WAY_ID, -1);
        viewModel.setWayId(id);
    }

    private void setupDetailsPager() {
        View layoutOne = getLayoutInflater().inflate(R.layout.map_details, null);
        speedText = layoutOne.findViewById(R.id.speed);
        timeText = layoutOne.findViewById(R.id.time);
        distanceText = layoutOne.findViewById(R.id.distance);

        View layoutTwo = getLayoutInflater().inflate(R.layout.map_details_more, null);
        avgSpeed = layoutTwo.findViewById(R.id.avgSpeed);
        avgMovingSpeed = layoutTwo.findViewById(R.id.avgMovingSpeed);
        maxAltitude = layoutTwo.findViewById(R.id.maxAltitude);
        minAltitude = layoutTwo.findViewById(R.id.minAltitude);

        DetailsPagerAdapter detailsPagerAdapter = new DetailsPagerAdapter();
        detailsPagerAdapter.addLayout(layoutOne);
        detailsPagerAdapter.addLayout(layoutTwo);

        detailViewPager.setAdapter(detailsPagerAdapter);
        tabLayout.setupWithViewPager(detailViewPager, true);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
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
            LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();
            for (int i = 0; i < size; i++) {
                WayPoint wp = wayPoints.get(i);
                LatLng point = new LatLng(wp.getLatitude(), wp.getLongitude());
                poly.add(point);
                latLngBoundsBuilder.include(point);

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
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBoundsBuilder.build(), 100));
        }
        distanceText.setText(getString(R.string.distance_unit, way.getTotalDistance()));
        avgSpeed.setText(getString(R.string.speed_unit, way.getAvgSpeed()));
        avgMovingSpeed.setText(getString(R.string.speed_unit, way.getAvgMovingSpeed()));
        maxAltitude.setText(getString(R.string.altitude_unit, way.getMaxAltitude()));
        minAltitude.setText(getString(R.string.altitude_unit, way.getMinAltitude()));
    }
}
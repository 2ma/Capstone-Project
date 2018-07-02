package hu.am2.myway.ui.history;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
import hu.am2.myway.Utils;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayPoint;
import hu.am2.myway.location.model.WayWithWayPoints;
import timber.log.Timber;

import static hu.am2.myway.ui.map.MapActivity.MAP_TYPE;

public class HistoryMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;

    private final List<Polyline> paths = new ArrayList<>();
    private Marker startPointMarker;
    private Marker endPointMarker;
    private final List<Marker> segmentMarkers = new ArrayList<>();

    //details pager layout 1
    private TextView avgSpeedText;
    private TextView timeText;
    private TextView distanceText;

    //details pager layout 2
    private TextView maxSpeedText;
    private TextView maxAltitudeText;
    private TextView minAltitudeText;

    private final int[] pathColors = {Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.RED};

    private int mapType = GoogleMap.MAP_TYPE_NORMAL;

    @BindView(R.id.tabDots)
    TabLayout tabLayout;

    @BindView(R.id.detailViewPager)
    InterceptViewPager detailViewPager;

    @BindView(R.id.startPauseFab)
    FloatingActionButton startPauseFab;

    @BindView(R.id.stopFab)
    FloatingActionButton stopFab;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Inject
    ViewModelProvider.Factory factory;

    private HistoryMapViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        ButterKnife.bind(this);

        stopFab.setVisibility(View.GONE);
        startPauseFab.setVisibility(View.GONE);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setupDetailsPager();

        if (savedInstanceState != null) {
            mapType = savedInstanceState.getInt(MAP_TYPE);
        } else {
            String m = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.map_type_key), "");
            mapType = m.length() > 0 ? Integer.valueOf(m) : GoogleMap.MAP_TYPE_NORMAL;
        }

        viewModel = ViewModelProviders.of(this, factory).get(HistoryMapViewModel.class);

        long id = getIntent().getLongExtra(Constants.EXTRA_WAY_ID, -1);
        viewModel.setWayId(id);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(MAP_TYPE, mapType);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.menu_map_normal: {
                mapType = GoogleMap.MAP_TYPE_NORMAL;
                break;
            }
            case R.id.menu_map_hybrid: {
                mapType = GoogleMap.MAP_TYPE_HYBRID;
                break;
            }
            case R.id.menu_map_satellite: {
                mapType = GoogleMap.MAP_TYPE_SATELLITE;
                break;
            }
            case R.id.menu_map_terrain: {
                mapType = GoogleMap.MAP_TYPE_TERRAIN;
                break;
            }
            default:
                super.onOptionsItemSelected(item);
        }

        if (map != null) {
            map.setMapType(mapType);
        }

        return true;
    }

    private void setupDetailsPager() {
        View layoutOne = getLayoutInflater().inflate(R.layout.map_details, null);
        //details pager layout 1
        avgSpeedText = layoutOne.findViewById(R.id.speed);
        int iconSize = (int) getResources().getDimension(R.dimen.detail_icon_size);
        Drawable drawable = getResources().getDrawable(R.drawable.avg_speed_icon);
        drawable.setBounds(0, 0, iconSize, iconSize);
        avgSpeedText.setCompoundDrawables(null, null, null, drawable);
        timeText = layoutOne.findViewById(R.id.time);
        distanceText = layoutOne.findViewById(R.id.distance);

        View layoutTwo = getLayoutInflater().inflate(R.layout.map_details_more, null);
        layoutTwo.findViewById(R.id.avgSpeed).setVisibility(View.GONE);
        maxSpeedText = layoutTwo.findViewById(R.id.maxSpeed);
        maxAltitudeText = layoutTwo.findViewById(R.id.maxAltitude);
        minAltitudeText = layoutTwo.findViewById(R.id.minAltitude);

        DetailsPagerAdapter detailsPagerAdapter = new DetailsPagerAdapter();
        detailsPagerAdapter.addLayout(layoutOne);
        detailsPagerAdapter.addLayout(layoutTwo);

        detailViewPager.setAdapter(detailsPagerAdapter);
        tabLayout.setupWithViewPager(detailViewPager, true);
        detailViewPager.setInterceptViews(new View[]{avgSpeedText, timeText, distanceText}, new View[]{maxSpeedText, maxAltitudeText,
            minAltitudeText});

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(mapType);
        viewModel.getWayWitWayPoints().observe(this, this::displayMapHistory);
    }

    private void displayMapHistory(WayWithWayPoints wayWithWayPoints) {
        if (paths.size() > 0) {
            clearPaths();
        }
        if (startPointMarker != null) {
            startPointMarker.remove();
            startPointMarker = null;
        }
        if (endPointMarker != null) {
            endPointMarker.remove();
            endPointMarker = null;
        }
        if (segmentMarkers.size() > 0) {
            int s = segmentMarkers.size();
            for (int i = 0; i < s; i++) {
                segmentMarkers.get(i).remove();
            }
            segmentMarkers.clear();
        }
        Way way = wayWithWayPoints.getWay();
        List<WayPoint> wayPoints = wayWithWayPoints.getWayPoints();
        Timber.d("Waypoints size: %d", wayPoints.size());
        int wayPointsSize = wayPoints.size();
        if (wayPointsSize > 0) {
            WayPoint start = wayPoints.get(0);
            startPointMarker = map.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .position(new LatLng(start.getLatitude(), start.getLongitude()))
                .title(getString(R.string.start))
                .snippet(Utils.epochToStringDate(start.getTime()))
            );

            //builder so we can zoom the map to fit the whole route
            LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();
            List<LatLng> waySegment = new ArrayList<>();
            Location last = new Location("gps");
            last.setLatitude(wayPoints.get(0).getLatitude());
            last.setLongitude(wayPoints.get(0).getLongitude());
            for (int i = 0; i < wayPointsSize; i++) {
                WayPoint w = wayPoints.get(i);
                if (w.getLatitude() == Constants.WAY_END_COORDINATE) {
                    if (waySegment.size() > 0) {
                        //adds start marker for segment if it's not the first
                        if (startPointMarker != null && !startPointMarker.getPosition().equals(waySegment.get(0))) {
                            segmentMarkers.add(map.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory
                                    .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                                .position(waySegment.get(0))
                                .title(getString(R.string.segment_start, paths.size() + 1))
                                .snippet(Utils.epochToStringDate(wayPoints.get(i - waySegment.size()).getTime()))
                            ));
                        }
                        //add segment path if segment has at least 2 markers
                        if (waySegment.size() > 1) {
                            paths.add(map.addPolyline(new PolylineOptions()
                                .color(pathColors[paths.size() % pathColors.length])
                                .width(6)
                                .addAll(waySegment)
                            ));
                            //add end marker for segment or final end marker
                            if (i + 1 < wayPointsSize) {
                                segmentMarkers.add(map.addMarker(new MarkerOptions()
                                    .icon(BitmapDescriptorFactory
                                        .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                                    .position(waySegment.get(waySegment.size() - 1))
                                    .title(getString(R.string.segment_end, paths.size()))
                                    .snippet(Utils.epochToStringDate(wayPoints.get(i - 1).getTime()))
                                ));
                            } else {
                                WayPoint end = wayPoints.get(i - 1);
                                endPointMarker = map.addMarker(new MarkerOptions().position(new LatLng(end.getLatitude(), end.getLongitude())).icon
                                    (BitmapDescriptorFactory
                                        .defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                    .title(getString(R.string.finish))
                                    .snippet(Utils.epochToStringDate(end.getTime()))
                                );
                            }
                        }
                        waySegment.clear();
                    }
                } else {
                    LatLng ll = new LatLng(w.getLatitude(), w.getLongitude());
                    latLngBoundsBuilder.include(ll);
                    waySegment.add(ll);
                }
            }
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBoundsBuilder.build(), 100));
        }
        String dist = getString(R.string.distance_unit, way.getTotalDistance() / 1000);
        distanceText.setText(Utils.getSmallSpannable(dist, dist.length() - 3));
        String as = getString(R.string.speed_unit, way.getAvgSpeed() * 3.6f);
        avgSpeedText.setText(Utils.getSmallSpannable(as, as.length() - 5));
        String ms = getString(R.string.speed_unit, way.getMaxSpeed() * 3.6f);
        maxSpeedText.setText(Utils.getSmallSpannable(ms, ms.length() - 5));
        if (way.getMaxAltitude() == 9999) {
            maxAltitudeText.setText(R.string.empty_altitude);
        } else {
            maxAltitudeText.setText(getString(R.string.altitude_unit, way.getMaxAltitude()));
        }
        if (way.getMinAltitude() == -9999) {
            minAltitudeText.setText(R.string.empty_altitude);
        } else {
            minAltitudeText.setText(getString(R.string.altitude_unit, way.getMinAltitude()));
        }
        timeText.setText(Utils.getTimeFromMilliseconds(way.getTotalTime()));
    }

    private void clearPaths() {
        for (int i = 0; i < paths.size(); i++) {
            paths.get(i).remove();
        }
        paths.clear();
    }
}

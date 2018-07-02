package hu.am2.myway.ui.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
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
import butterknife.OnClick;
import dagger.android.AndroidInjection;
import hu.am2.myway.R;
import hu.am2.myway.Utils;
import hu.am2.myway.location.LocationService;
import hu.am2.myway.location.WayRecorder;
import hu.am2.myway.location.model.WayUiModel;
import hu.am2.myway.ui.history.DetailsPagerAdapter;
import hu.am2.myway.ui.history.InterceptViewPager;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String MAP_TYPE = "MAP_TYPE";
    private GoogleMap map;

    private LocationService locationService;

    private final List<Polyline> paths = new ArrayList<>();
    private Marker currentMarker;
    private Circle circle;

    private boolean bound = false;

    private int mapType = GoogleMap.MAP_TYPE_NORMAL;

    @BindView(R.id.startPauseFab)
    ImageButton startPauseBtn;

    @BindView(R.id.tabDots)
    TabLayout tabLayout;

    //details pager layout 1
    private TextView speedText;
    private TextView timeText;
    private TextView distanceText;

    //details pager layout 2
    private TextView avgSpeedText;
    private TextView maxSpeedText;
    private TextView maxAltitudeText;
    private TextView minAltitudeText;

    private final int[] pathColors = {Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.RED};

    @BindView(R.id.detailViewPager)
    InterceptViewPager detailViewPager;

    @BindView(R.id.waitingForSignal)
    TextView waitingForSignal;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Inject
    ViewModelProvider.Factory factory;

    private boolean gpsStatus = false;

    private MapViewModel viewModel;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            locationService = ((LocationService.ServiceBinder) service).getService();
            observeData();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationService = null;
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (savedInstanceState != null) {
            mapType = savedInstanceState.getInt(MAP_TYPE);
        } else {
            String m = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.map_type_key), "");
            mapType = m.length() > 0 ? Integer.valueOf(m) : GoogleMap.MAP_TYPE_NORMAL;
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setupDetailsPager();
        viewModel = ViewModelProviders.of(this, factory).get(MapViewModel.class);
    }

    private void observeData() {
        locationService.getTotalTimeLiveData().observe(MapActivity.this, this::updateElapsedTime);
        locationService.getStateLiveData().observe(MapActivity.this, this::handleState);
        locationService.getLocationLiveData().observe(MapActivity.this, this::handleLocation);
        locationService.getSpeedLiveData().observe(MapActivity.this, this::handleSpeed);
        viewModel.getWayUiModelLiveData().observe(this, this::handleWayUiModel);
    }

    private void handleSpeed(Float speed) {
        //location speed is in m/s
        String s = getString(R.string.speed_unit, speed * 3.6f);
        speedText.setText(Utils.getSmallSpannable(s, s.length() - 5));
    }

    private void handleLocation(Location location) {
        if (location != null) {
            LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());

            if (currentMarker == null) {
                currentMarker = map.addMarker(new MarkerOptions().position(pos));
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
            } else {
                currentMarker.setPosition(pos);
                map.animateCamera(CameraUpdateFactory.newLatLng(pos));
            }
            if (circle == null) {
                circle = map.addCircle(new CircleOptions().center(pos).radius(location.getAccuracy())
                    .fillColor(R.color.circleBackground)
                    .strokeColor(R.color.circleLine)
                    .strokeWidth(1));
            } else {
                circle.setCenter(pos);
            }
        }
    }

    private void handleState(Integer state) {
        playPauseButtonState(state == WayRecorder.STATE_RECORDING || state == WayRecorder.STATE_WAITING_FOR_SIGNAL);
        waitingForSignal.setVisibility(state == WayRecorder.STATE_WAITING_FOR_SIGNAL ? View.VISIBLE : View.GONE);
    }

    private void handleWayUiModel(WayUiModel wayModel) {
        if (wayModel == null) {
            clearUi();
            return;
        }
        String dist = getString(R.string.distance_unit, wayModel.getTotalDistance());
        distanceText.setText(Utils.getSmallSpannable(dist, dist.length() - 3));
        showWayPath(wayModel.getWaySegments());
        if (wayModel.getMaxAltitude() == 9999) {
            maxAltitudeText.setText(R.string.empty_altitude);
        } else {
            maxAltitudeText.setText(getString(R.string.altitude_unit, wayModel.getMaxAltitude()));
        }
        if (wayModel.getMinAltitude() == -9999) {
            minAltitudeText.setText(R.string.empty_altitude);
        } else {
            minAltitudeText.setText(getString(R.string.altitude_unit, wayModel.getMinAltitude()));
        }
        String avgSpeed = getString(R.string.speed_unit, wayModel.getAvgSpeed());
        avgSpeedText.setText(Utils.getSmallSpannable(avgSpeed, avgSpeed.length() - 5));
        String maxSpeed = getString(R.string.speed_unit, wayModel.getMaxSpeed());
        maxSpeedText.setText(Utils.getSmallSpannable(maxSpeed, maxSpeed.length() - 5));
    }

    private void clearPaths() {
        for (int i = 0; i < paths.size(); i++) {
            paths.get(i).remove();
        }
        paths.clear();
    }

    private void updateElapsedTime(Long time) {
        String t = Utils.getTimeFromMilliseconds(time);
        timeText.setText(t);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(MAP_TYPE, mapType);
        super.onSaveInstanceState(outState);
    }

    private void setupDetailsPager() {
        View layoutOne = getLayoutInflater().inflate(R.layout.map_details, null);
        speedText = layoutOne.findViewById(R.id.speed);
        timeText = layoutOne.findViewById(R.id.time);
        distanceText = layoutOne.findViewById(R.id.distance);

        View layoutTwo = getLayoutInflater().inflate(R.layout.map_details_more, null);
        avgSpeedText = layoutTwo.findViewById(R.id.avgSpeed);
        maxSpeedText = layoutTwo.findViewById(R.id.maxSpeed);
        maxAltitudeText = layoutTwo.findViewById(R.id.maxAltitude);
        minAltitudeText = layoutTwo.findViewById(R.id.minAltitude);

        DetailsPagerAdapter detailsPagerAdapter = new DetailsPagerAdapter();
        detailsPagerAdapter.addLayout(layoutOne);
        detailsPagerAdapter.addLayout(layoutTwo);

        detailViewPager.setAdapter(detailsPagerAdapter);
        tabLayout.setupWithViewPager(detailViewPager, true);
        detailViewPager.setInterceptViews(new View[]{speedText, timeText, distanceText}, new View[]{avgSpeedText, maxSpeedText, maxAltitudeText,
            minAltitudeText});
    }

    @Override
    public void onBackPressed() {
        if (stopRecordingDialog()) return;
        super.onBackPressed();
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
                if (stopRecordingDialog()) {
                    return true;
                } else {
                    onBackPressed();
                    return true;
                }
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

    private boolean stopRecordingDialog() {
        if (!gpsStatus) {
            return false;
        }
        int state = locationService.getRecordingState();
        if (state == WayRecorder.STATE_RECORDING || state == WayRecorder.STATE_PAUSE) {
            AlertDialog.Builder aBuilder = new AlertDialog.Builder(this);
            aBuilder.setMessage(R.string.stop_recording);
            aBuilder.setPositiveButton(R.string.yes, (dialog, which) -> onStopClicked());
            aBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            });
            AlertDialog dialog = aBuilder.create();
            dialog.show();
            return true;
        }
        return false;
    }

    private void clearUi() {
        speedText.setText(R.string.speed_default);
        timeText.setText(R.string.time_default);
        distanceText.setText(R.string.distance_default);

        avgSpeedText.setText(R.string.speed_default);
        maxSpeedText.setText(R.string.speed_default);
        maxAltitudeText.setText(R.string.altitude_default);
        minAltitudeText.setText(R.string.altitude_default);

        if (paths.size() > 0) {
            clearPaths();
        }
        if (circle != null) {
            circle.remove();
            circle = null;
        }
        if (currentMarker != null) {
            currentMarker.remove();
            currentMarker = null;
        }
    }

    @OnClick(R.id.startPauseFab)
    public void recordPause(View view) {
        if (gpsStatus) {
            recording();
        }
    }

    private void playPauseButtonState(boolean state) {
        if (state) {
            startPauseBtn.setImageResource(R.drawable.ic_pause_black_24dp);
        } else {
            startPauseBtn.setImageResource(R.drawable.ic_record_24dp);
        }
    }

    @OnClick(R.id.stopFab)
    public void onStopClicked() {
        if (gpsStatus) {
            int state = locationService.getRecordingState();
            if (state != WayRecorder.STATE_STOP) {
                locationService.stopRecording();
            }
        }
    }

    private void showWayPath(List<List<LatLng>> waySegments) {
        int size = waySegments.size();
        if (size > 0) {
            if (paths.size() > 0) {
                clearPaths();
            }
            //add all segments with different colors
            for (int i = 0; i < size; i++) {
                paths.add(map.addPolyline(new PolylineOptions()
                    .color(pathColors[i % pathColors.length])
                    .width(6)
                    .addAll(waySegments.get(0))));
            }
        } else if (paths.size() > 0) {
            clearPaths();
        }
    }

    private void recording() {
        locationService.startPauseRecording();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 13);
            return;
        }
        try {
            int gps = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            if (gps == Settings.Secure.LOCATION_MODE_OFF) {
                showLocationDialog();
                gpsStatus = false;
                return;
            } else {
                gpsStatus = true;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        bindService(new Intent(this, LocationService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void showLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.gps_disabled);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            Intent gpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(gpsIntent);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
        });
        builder.create().show();
    }

    @Override
    protected void onStop() {
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 13 && grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //TODO not necessary true
            recording();
        } else {
            //TODO handle missing permission differently?
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMyLocationEnabled(true);
        map.setMapType(mapType);
    }
}

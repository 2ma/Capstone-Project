package hu.am2.myway.ui.map;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.Utils;
import hu.am2.myway.location.LocationService;
import hu.am2.myway.location.WayRecorder;
import hu.am2.myway.location.model.WayUiModel;
import hu.am2.myway.ui.history.DetailsPagerAdapter;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;

    private LocationService locationService = null;

    private Polyline path = null;
    private Marker currentMarker = null;
    private Circle circle = null;

    private boolean bound = false;

    @BindView(R.id.startPauseBtn)
    ImageButton startPauseBtn;

    //details pager layout 1
    TextView speedText;
    TextView timeText;
    TextView distanceText;

    //details pager layout 2
    TextView avgSpeed;
    TextView avgMovingSpeed;
    TextView maxAltitude;
    TextView minAltitude;

    @BindView(R.id.detailViewPager)
    ViewPager detailViewPager;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            locationService = ((LocationService.ServiceBinder) service).getService();
            locationService.getWayUiModelLiveData().observe(MapActivity.this, wayModel -> {
                if (wayModel != null) {
                    handleWayUiModel(wayModel);
                } else {
                    clearUi();
                }
            });
            locationService.getElapsedTimeLiveData().observe(MapActivity.this, time -> updateElapsedTime(time));
            locationService.getStateLiveData().observe(MapActivity.this, state -> handleState(state));
            locationService.getLocationLiveData().observe(MapActivity.this, location -> handleLocation(location));
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationService = null;
            bound = false;
        }
    };

    private void handleLocation(Location location) {
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        speedText.setText(getString(R.string.speed_unit, location.getSpeed()));
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

    private void handleState(Integer state) {
        playPauseButtonState(state == WayRecorder.STATE_RECORDING || state == WayRecorder.STATE_WAITING_FOR_SIGNAL);
        //TODO visibility for accuracy ui element
    }

    private void updateElapsedTime(Long time) {
        timeText.setText(Utils.getTimeFromMilliseconds(time));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setupDetailsPager();

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
    }

    @Override
    public void onBackPressed() {
        if (stopRecordingDialog()) return;
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (stopRecordingDialog()) return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean stopRecordingDialog() {
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
        //TODO check this
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt(Constants.PREF_RECORDING_STATE, WayRecorder.STATE_STOP).apply();
        return false;
    }

    private void clearUi() {
        speedText.setText(R.string.speed_default);
        timeText.setText(R.string.time_default);
        distanceText.setText(R.string.distance_default);
        if (path != null) {
            path.remove();
            path = null;
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

    @OnClick(R.id.startPauseBtn)
    public void recordPause(View view) {
        recording();
    }

    private void playPauseButtonState(boolean state) {
        if (state) {
            startPauseBtn.setImageResource(R.drawable.ic_pause_black_24dp);
        } else {
            startPauseBtn.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp);
        }
    }

    @OnClick(R.id.stopBtn)
    public void onStopClicked() {
        int state = locationService.getRecordingState();
        if (state != WayRecorder.STATE_STOP) {
            locationService.stopRecording();
        }
    }

    private void handleWayUiModel(WayUiModel wayModel) {

        distanceText.setText(getString(R.string.distance_unit, wayModel.getTotalDistance()));
        showWayPath(wayModel.getWayPoints());
        if (wayModel.getWayPoints().size() > 0) {
            showWayPath(wayModel.getWayPoints());
        } else if (path != null) {
            path.remove();
            path = null;
        }

        //TODO display other stats

        /*case WayStatus.STATE_RECORDING: {
                Timber.d("startPauseRecording");
                playPauseButtonState(true);
                List<LatLng> wayPoints = wayModel.getWayPoints();
                if (wayPoints.size() > 0) {
                    LatLng lastPos = wayPoints.get(wayPoints.size() - 1);
                    showWayPath(wayPoints);

                    if (currentMarker == null) {
                        currentMarker = map.addMarker(new MarkerOptions().position(lastPos));
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPos, 15f));
                    } else {
                        currentMarker.setPosition(lastPos);
                        map.animateCamera(CameraUpdateFactory.newLatLng(lastPos));
                    }
                    if (circle == null) {
                        circle = map.addCircle(new CircleOptions().center(lastPos).radius(location.getAccuracy())
                            .fillColor(R.color.circleBackground)
                            .strokeColor(R.color.circleLine)
                            .strokeWidth(1));
                    } else {
                        circle.setCenter(pos);
                    }
                    speedText.setText(getString(R.string.speed_unit, location.getSpeed()));
                    distanceText.setText(getString(R.string.distance_unit, status.getWay().getTotalDistance()));
                }
                break;
            }*/

    }

    private void showWayPath(List<LatLng> wayPoints) {
        if (wayPoints.size() > 0) {
            if (path != null) {
                path.remove();
                path = null;
            }
            path = map.addPolyline(new PolylineOptions()
                .color(Color.GREEN)
                .width(3)
                .addAll(wayPoints));
        } else if (path != null) {
            path.remove();
            path = null;
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
        bindService(new Intent(this, LocationService.class), serviceConnection, Context.BIND_AUTO_CREATE);
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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        map.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }
}

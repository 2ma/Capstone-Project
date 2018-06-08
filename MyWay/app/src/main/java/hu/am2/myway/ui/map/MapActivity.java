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
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hu.am2.myway.AppExecutors;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.Utils;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.LocationService;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.location.model.WayStatus;
import timber.log.Timber;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;

    private LocationService locationService = null;

    private Polyline path = null;
    private Marker currentMarker = null;
    private Circle circle = null;

    private boolean bound = false;

    @Inject
    AppExecutors executors;

    @Inject
    Repository repository;

    @BindView(R.id.startPauseBtn)
    ImageButton startPauseBtn;

    @BindView(R.id.speed)
    TextView speedText;

    @BindView(R.id.time)
    TextView timeText;

    @BindView(R.id.distance)
    TextView distanceText;

    @BindView(R.id.bottomSheet)
    ConstraintLayout bottomSheet;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            locationService = ((LocationService.ServiceBinder) service).getService();
            locationService.getWayStatusLiveData().observe(MapActivity.this, wayStatus -> {
                if (wayStatus != null) {
                    handleWayStatus(wayStatus);
                } else {
                    clearUi();
                }
            });
            locationService.getElapsedTimeLiveData().observe(MapActivity.this, time -> updateElapsedTime(time));
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationService = null;
            bound = false;
        }
    };

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
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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
        if (state == WayStatus.STATE_RECORDING || state == WayStatus.STATE_PAUSE) {
            AlertDialog.Builder aBuilder = new AlertDialog.Builder(this);
            aBuilder.setMessage(R.string.stop_recording);
            aBuilder.setPositiveButton(R.string.yes, (dialog, which) -> onStopClicked());
            aBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            });
            AlertDialog dialog = aBuilder.create();
            dialog.show();
            return true;
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt(Constants.PREF_RECORDING_STATE, WayStatus.STATE_STOP).apply();
        return false;
    }

    private void clearUi() {
        playPauseButtonState(false);
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
        WayStatus status = locationService.getWayStatus();
        if (status != null) {
            String name = status.getWay().getWayName();
            locationService.stopRecording();
            AlertDialog.Builder aBuilder = new AlertDialog.Builder(this);
            EditText wayNameView = new EditText(this);
            wayNameView.setText(name);
            aBuilder.setView(wayNameView);
            aBuilder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                String n = wayNameView.getText().toString();
                if (n.trim().length() > 0 && !name.equals(n)) {
                    Way w = status.getWay();
                    w.setWayName(n);
                    executors.getDiskIO().execute(() -> repository.updateWay(w));
                }
                finish();
            });
            AlertDialog nameDialog = aBuilder.create();
            nameDialog.show();
        }
    }

    private void handleWayStatus(WayStatus status) {

        switch (status.getState()) {
            case WayStatus.STATE_WAITING_FOR_SIGNAL: {
                //TODO display waiting signal message
                Toast.makeText(this, "Waiting for signal", Toast.LENGTH_SHORT).show();
                playPauseButtonState(true);
                break;
            }
            case WayStatus.STATE_PAUSE: {
                playPauseButtonState(false);
                showWayPath(status.getWayPoints());
                distanceText.setText(getString(R.string.distance_unit, status.getWay().getTotalDistance()));
                break;
            }
            case WayStatus.STATE_RECORDING: {
                Timber.d("startPauseRecording");
                playPauseButtonState(true);
                Location location = status.getLastLocation();
                if (location != null) {
                    LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                    showWayPath(status.getWayPoints());

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
                    speedText.setText(getString(R.string.speed_unit, location.getSpeed()));
                    distanceText.setText(getString(R.string.distance_unit, status.getWay().getTotalDistance()));
                }
                break;
            }
        }
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 13);
            return;
        }
        locationService.startPauseRecording();
    }

    @Override
    protected void onStart() {
        super.onStart();
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
            Snackbar.make(null, R.string.missing_permission, Snackbar.LENGTH_SHORT).show();
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

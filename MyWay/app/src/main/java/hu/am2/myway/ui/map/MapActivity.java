package hu.am2.myway.ui.map;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hu.am2.myway.R;
import hu.am2.myway.Utils;
import hu.am2.myway.location.LocationService;
import hu.am2.myway.location.model.Track;
import timber.log.Timber;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;

    private LocationService locationService = null;

    private Polyline path = null;
    private Marker currentPosition = null;
    private Circle circle = null;

    private boolean bound = false;

    @BindView(R.id.fab)
    FloatingActionButton recordButton;

    @BindView(R.id.speed)
    TextView speedText;

    @BindView(R.id.time)
    TextView timeText;

    @BindView(R.id.distance)
    TextView distanceText;

    @BindView(R.id.bottomSheet)
    ConstraintLayout bottomSheet;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            locationService = ((LocationService.ServiceBinder) service).getService();
            locationService.getTrackLiveData().observe(MapActivity.this, track -> handleTrackData(track));
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
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @OnClick(R.id.fab)
    public void recordPause(View view) {
        recording();
    }

    private void playPauseButtonState(boolean state) {
        if (state) {
            recordButton.setImageResource(R.drawable.ic_pause_black_24dp);
        } else {
            recordButton.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp);
        }
    }

    private void handleTrackData(Track track) {

        switch (track.getState()) {
            case Track.STATE_WAITING_FOR_SIGNAL: {
                //TODO display waiting signal message
                Toast.makeText(this, "Waiting for signal", Toast.LENGTH_SHORT).show();
                playPauseButtonState(true);
                Timber.d("trackWaiting");
                break;
            }
            case Track.STATE_PAUSE: {
                playPauseButtonState(false);
                Snackbar.make(bottomSheet, R.string.save_track, Snackbar.LENGTH_INDEFINITE).setAction(android.R.string.ok, v -> locationService
                    .saveTrack()).show();
                Timber.d("trackPause");
                break;
            }
            case Track.STATE_RECORDING: {
                Timber.d("trackRecord");
                playPauseButtonState(true);
                Location location = track.getLastLocation();
                if (location != null) {
                    LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                    if (path != null) {
                        path.remove();
                        path = null;
                    }
                    path = map.addPolyline(new PolylineOptions()
                        .color(Color.GREEN)
                        .width(3)
                        .addAll(track.getPath()));

                    if (currentPosition == null) {
                        currentPosition = map.addMarker(new MarkerOptions().position(pos));
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
                    } else {
                        currentPosition.setPosition(pos);
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
                    distanceText.setText(getString(R.string.distance_unit, track.getTotalDistance()));
                }
                break;
            }
        }
    }

    private void recording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 13);
            return;
        }
        locationService.recording();
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

package hu.am2.myway.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.AndroidInjection;
import hu.am2.myway.AppExecutors;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.WayRecorder;
import hu.am2.myway.ui.history.HistoryActivity;
import hu.am2.myway.ui.map.MapActivity;
import hu.am2.myway.ui.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    @Inject
    AppExecutors executors;

    @Inject
    Repository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 14);
            return;
        }
        //TODO advise to turn on gps if it's off
        //TODO check if startPauseRecording, launch MapActivity
        resumeActiveRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 14 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            resumeActiveRecording();
        } else {
            //TODO handle missing permission differently?
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void resumeActiveRecording() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int status = sharedPreferences.getInt(Constants.PREF_RECORDING_STATE, WayRecorder.STATE_STOP);
        if (status != WayRecorder.STATE_STOP) {
            startActivity(new Intent(this, MapActivity.class));
        }
    }

    @OnClick(R.id.newWayBtn)
    public void onNewWayClick() {
        startActivity(new Intent(MainActivity.this, MapActivity.class));
    }

    @OnClick(R.id.historyBtn)
    public void onHistoryClick() {
        startActivity(new Intent(this, HistoryActivity.class));
    }

    @OnClick(R.id.settingsBtn)
    public void onSettingsClick() {
        startActivity(new Intent(this, SettingsActivity.class));
    }


}

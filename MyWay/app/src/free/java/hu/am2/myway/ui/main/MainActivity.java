package hu.am2.myway.ui.main;

import android.Manifest;
import android.app.ActivityOptions;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.AndroidInjection;
import hu.am2.myway.AppExecutors;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.WayRecorder;
import hu.am2.myway.ui.history.HistoryListActivity;
import hu.am2.myway.ui.map.MapActivity;
import hu.am2.myway.ui.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    @Inject
    AppExecutors executors;

    @Inject
    Repository repository;

    @BindView(R.id.adView)
    AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        AdRequest adRequest = new AdRequest.Builder().build();

        adView.loadAd(adRequest);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 14);
            return;
        }
        resumeActiveRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 14 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            resumeActiveRecording();
        } else {
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

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    @OnClick(R.id.newWayBtn)
    public void onNewWayClick() {
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Bundle activityOptions = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
            startActivity(intent, activityOptions);
        } else {
            startActivity(intent);
        }
    }

    @OnClick(R.id.historyBtn)
    public void onHistoryClick() {
        Intent intent = new Intent(MainActivity.this, HistoryListActivity.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Bundle activityOptions = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
            startActivity(intent, activityOptions);
        } else {
            startActivity(intent);
        }
    }

    @OnClick(R.id.settingsBtn)
    public void onSettingsClick() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Bundle activityOptions = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
            startActivity(intent, activityOptions);
        } else {
            startActivity(intent);
        }
    }


}

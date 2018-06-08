package hu.am2.myway.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.AndroidInjection;
import hu.am2.myway.AppExecutors;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.model.WayStatus;
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
        //TODO check if startPauseRecording, launch MapActivity
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int status = sharedPreferences.getInt(Constants.PREF_RECORDING_STATE, WayStatus.STATE_STOP);
        if (status != WayStatus.STATE_STOP) {
            startActivity(new Intent(this, MapActivity.class));
        }
    }

    @OnClick(R.id.newWayBtn)
    public void onNewWayClick() {
        executors.getDiskIO().execute(() -> {
            //TODO check if necessary
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.putInt(Constants.PREF_RECORDING_STATE, WayStatus.STATE_STOP);
            editor.apply();
            startActivity(new Intent(MainActivity.this, MapActivity.class));
        });
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

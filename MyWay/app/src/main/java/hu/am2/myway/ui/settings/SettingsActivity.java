package hu.am2.myway.ui.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import hu.am2.myway.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}

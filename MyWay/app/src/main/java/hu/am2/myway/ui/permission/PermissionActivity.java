package hu.am2.myway.ui.permission;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.location.LocationService;

public class PermissionActivity extends AppCompatActivity {

    public static final String ACTION_PERMISSION = "hu.am2.myway.ui.permission.ACTION_PERMISSION";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ACTION_PERMISSION.equals(getIntent().getAction())) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 13);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 13 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, LocationService.class);
            intent.setAction(Constants.ACTION_START_PAUSE_RECORDING);
            startService(intent);
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}

package hu.am2.myway.ui.saveway;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.EditText;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.AndroidInjection;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.ui.main.MainActivity;

public class SaveWayActivity extends AppCompatActivity {

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private SaveWayViewModel viewModel;

    @BindView(R.id.wayNameEditText)
    EditText wayNameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_way);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ButterKnife.bind(this);

        long id = getIntent().getLongExtra(Constants.EXTRA_WAY_ID, -1);

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SaveWayViewModel.class);
        viewModel.setWayId(id);
        viewModel.getWay().observe(this, this::showWay);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            launchMain();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showWay(Way way) {
        if (way != null) {
            wayNameEditText.setText(way.getWayName());
            wayNameEditText.setSelection(way.getWayName().length());
        }
    }

    @OnClick(R.id.saveBtn)
    public void saveWayName() {
        if (wayNameEditText.getText().toString().trim().length() > 0) {
            viewModel.saveWay(wayNameEditText.getText().toString());
        }
        launchMain();
    }

    @Override
    public void onBackPressed() {
        launchMain();
    }

    private void launchMain() {
        startActivity(new Intent(this, MainActivity.class));
    }
}

package hu.am2.myway.ui.saveway;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.AndroidInjection;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.location.model.Way;

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

        ButterKnife.bind(this);

        long id = getIntent().getLongExtra(Constants.EXTRA_WAY_ID, -1);

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SaveWayViewModel.class);
        viewModel.setWayId(id);
        viewModel.getWay().observe(this, this::showWay);
    }

    private void showWay(Way way) {
        if (way != null) {
            wayNameEditText.setText(way.getWayName());
        }
    }

    @OnClick(R.id.saveBtn)
    public void saveWayName() {
        if (wayNameEditText.getText().toString().trim().length() > 0) {
            viewModel.saveWay(wayNameEditText.getText().toString());
        }
        //TODO if coming for widget finish else go to main activity
        finish();
    }
}

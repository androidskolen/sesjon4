package no.bouvet.androidskolen.nearbycontacts.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

import no.bouvet.androidskolen.nearbycontacts.BuildConfig;
import no.bouvet.androidskolen.nearbycontacts.R;

public class AboutView extends LinearLayout {


    private final TextView versionName;
    private final TextView versionCode;
    private final TextView timeStamp;
    private final TextView buildSource;

    public AboutView(Context context) {
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.aboutview, this, true);

        versionName = (TextView) findViewById(R.id.versionName);
        versionCode = (TextView) findViewById(R.id.versionCode);
        timeStamp = (TextView) findViewById(R.id.timeStamp);
        buildSource = (TextView) findViewById(R.id.buildSource);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        versionName.setText(getResources().getString(R.string.versionName, BuildConfig.VERSION_NAME));
        versionCode.setText(getResources().getString(R.string.versionCode, String.valueOf(BuildConfig.VERSION_CODE)));

        Date date = new Date(BuildConfig.TIMESTAMP);
        timeStamp.setText(getResources().getString(R.string.timeStamp, date.toString()));

        if (BuildConfig.IDE_BUILD)
            buildSource.setText(R.string.build_from_ide);

    }
}

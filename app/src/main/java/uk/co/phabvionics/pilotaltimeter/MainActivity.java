package uk.co.phabvionics.pilotaltimeter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mPressure;
    private Altimeter mAltimeter;
    private TextView mText;
    private VerticalSpeedIndicator mVSI;
    private android.content.SharedPreferences mSettings;
    private android.content.SharedPreferences.Editor mEditor;
    private boolean mMetric;

    private File mFile;
    private BufferedWriter mBufferedWriter;
    private boolean mLogging;
    private long mLogEnd;
    private long mLastTimestamp;
    private int mPressureSelection;

    static final int SEL_QNH = 0;
    static final int SEL_QFE = 1;
    static final int SEL_FL = 2;

    static final String PRESSURE_DATUM = "PressureDatum";
    static final String PRESSURE_SELECTION = "PressureSelection";
    static final String PRESSURE_QNH = "PressureQNH";
    static final String PRESSURE_QFE = "PressureQFE";
    static final String METRIC = "Metric";

    private static final String TAG = "MainActivity";
    private static final boolean LoggingActive = false;

    private Button mButtonPlus10;
    private Button mButtonPlus1;
    private Button mButtonMinus10;
    private Button mButtonMinus1;
    private Button mButtonPressureAlt;
    private Button mButtonQFE;
    private Button mButtonQNH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (LoggingActive) {
            mFile = new File(
                    Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "nexus-4-pressure-log.txt");
            try {
                mBufferedWriter = new BufferedWriter(new FileWriter(mFile));
                mLogging = true;
            } catch (IOException e) {
                e.printStackTrace();
                mLogging = false;
            }
            mLogEnd = 0;
        }

        // Get an instance of the sensor service, and use that to get an
        // instance of a particular sensor.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mAltimeter = (Altimeter) findViewById(R.id.altimeter1);
        mVSI = (VerticalSpeedIndicator) findViewById(R.id.verticalSpeedIndicator1);
        mText = (TextView) findViewById(R.id.textView1);

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSettings.edit();
        mMetric = mSettings.getBoolean(METRIC, true);
        mAltimeter.setMetric(mMetric);
        mAltimeter.setPressureDatum(mSettings
                .getFloat(PRESSURE_DATUM, 1013.25f));
        mPressureSelection = mSettings.getInt(PRESSURE_SELECTION, SEL_QNH);

        if (!mSettings.getBoolean("show_vsi", true)) {
            mVSI.setVisibility(View.GONE);
        }

        mButtonPlus10 = (Button) findViewById(R.id.buttonPlus10);
        mButtonPlus10.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.putFloat(PRESSURE_DATUM,
                        mAltimeter.Add10ToPressureDatum());
                if (mPressureSelection == SEL_QNH) {
                    mEditor.putFloat(PRESSURE_QNH, mAltimeter.getPressureDatum());
                } else if (mPressureSelection == SEL_QFE) {
                    mEditor.putFloat(PRESSURE_QFE, mAltimeter.getPressureDatum());
                }
                mEditor.commit();
            }
        });

        mButtonPlus1 = (Button) findViewById(R.id.buttonPlus1);
        mButtonPlus1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.putFloat(PRESSURE_DATUM,
                        mAltimeter.Add1ToPressureDatum());
                if (mPressureSelection == SEL_QNH) {
                    mEditor.putFloat(PRESSURE_QNH, mAltimeter.getPressureDatum());
                } else if (mPressureSelection == SEL_QFE) {
                    mEditor.putFloat(PRESSURE_QFE, mAltimeter.getPressureDatum());
                }
                mEditor.commit();
            }
        });

        mButtonMinus10 = (Button) findViewById(R.id.buttonMinus10);
        mButtonMinus10.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.putFloat(PRESSURE_DATUM,
                        mAltimeter.Sub10FromPressureDatum());
                if (mPressureSelection == SEL_QNH) {
                    mEditor.putFloat(PRESSURE_QNH, mAltimeter.getPressureDatum());
                } else if (mPressureSelection == SEL_QFE) {
                    mEditor.putFloat(PRESSURE_QFE, mAltimeter.getPressureDatum());
                }
                mEditor.commit();
            }
        });

        mButtonMinus1 = (Button) findViewById(R.id.buttonMinus1);
        mButtonMinus1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.putFloat(PRESSURE_DATUM,
                        mAltimeter.Sub1FromPressureDatum());
                if (mPressureSelection == SEL_QNH) {
                    mEditor.putFloat(PRESSURE_QNH, mAltimeter.getPressureDatum());
                } else if (mPressureSelection == SEL_QFE) {
                    mEditor.putFloat(PRESSURE_QFE, mAltimeter.getPressureDatum());
                }
                mEditor.commit();
            }
        });

        mButtonPressureAlt = (Button) findViewById(R.id.buttonPressureAlt);
        mButtonPressureAlt.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.putFloat(PRESSURE_DATUM, mAltimeter.setPressureAlt());
                mEditor.putInt(PRESSURE_SELECTION, SEL_FL);
                mPressureSelection = SEL_FL;
                mEditor.commit();
                mButtonPlus10.setEnabled(false);
                mButtonPlus1.setEnabled(false);
                mButtonMinus10.setEnabled(false);
                mButtonMinus1.setEnabled(false);
                mAltimeter.SetDatumText("Press Alt");
            }
        });

        mButtonQNH = (Button) findViewById(R.id.buttonQNH);
        mButtonQNH.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.putInt(PRESSURE_SELECTION, SEL_QNH);
                mPressureSelection = SEL_QNH;
                mAltimeter.setPressureDatum(mSettings
                        .getFloat(PRESSURE_QNH, 1013.25f));
                mEditor.putFloat(PRESSURE_DATUM,
                        mAltimeter.getPressureDatum());
                mEditor.commit();
                mButtonPlus10.setEnabled(true);
                mButtonPlus1.setEnabled(true);
                mButtonMinus10.setEnabled(true);
                mButtonMinus1.setEnabled(true);
                mAltimeter.SetDatumText("QNH");
            }
        });

        mButtonQFE = (Button) findViewById(R.id.buttonQFE);
        mButtonQFE.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.putInt(PRESSURE_SELECTION, SEL_QFE);
                mPressureSelection = SEL_QFE;
                mAltimeter.setPressureDatum(mSettings
                        .getFloat(PRESSURE_QFE, 1013.25f));
                mEditor.putFloat(PRESSURE_DATUM,
                        mAltimeter.getPressureDatum());
                mEditor.commit();
                mButtonPlus10.setEnabled(true);
                mButtonPlus1.setEnabled(true);
                mButtonMinus10.setEnabled(true);
                mButtonMinus1.setEnabled(true);
                mAltimeter.SetDatumText("QFE");
            }
        });

        mButtonMinus10.setEnabled(mPressureSelection != SEL_FL);
        mButtonMinus1.setEnabled(mPressureSelection != SEL_FL);
        mButtonPlus10.setEnabled(mPressureSelection != SEL_FL);
        mButtonPlus1.setEnabled(mPressureSelection != SEL_FL);
        switch (mPressureSelection) {
            case SEL_FL:
                mAltimeter.SetDatumText("Press Alt");
                break;
            case SEL_QFE:
                mAltimeter.SetDatumText("QFE");
                break;
            case SEL_QNH:
                mAltimeter.SetDatumText("QNH");
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        // Handle item selected
        switch (item.getItemId()) {
            case R.id.action_settings:
                intent = new Intent(this, UserSettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_about:
                String url = "http://phabvionics.co.uk/android-pilot-altimeter/?pk_campaign=App";
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();

        int rate = 0;
        switch (Integer.parseInt(mSettings.getString("sample_rate", "0"))) {
            case 0:
                rate = SensorManager.SENSOR_DELAY_FASTEST; // 33ms approx on Nexus 4
                break;
            case 1:
                rate = SensorManager.SENSOR_DELAY_GAME; // 33ms approx on Nexus 4
                break;
            case 2:
                rate = SensorManager.SENSOR_DELAY_UI; // 63ms approx on Nexus 4
                break;
            case 3:
                rate = SensorManager.SENSOR_DELAY_NORMAL; // 200ms approx on Nexus 4
                break;
        }
        mSensorManager.registerListener(this, mPressure, rate);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public File getFileStorageDir(String filename) {
        // Get the directory for the user's public pictures directory.
        File file = new File(
                Environment.getExternalStoragePublicDirectory(null), filename);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        if (mSettings.getBoolean(METRIC, true) != mMetric) {
            mMetric = !mMetric;
            mAltimeter.setMetric(mMetric);
        }
        switch (Integer.parseInt(mSettings.getString("sample_filter_strength",
                "3"))) {
            case 0:
                mAltimeter.SetFilterSettlingTime(0);
                break;
            case 1:
                mAltimeter.SetFilterSettlingTime(0.5f);
                break;
            case 2:
                mAltimeter.SetFilterSettlingTime(1.0f);
                break;
            case 3:
                mAltimeter.SetFilterSettlingTime(2.5f);
                break;
            case 4:
                mAltimeter.SetFilterSettlingTime(5.0f);
                break;
            case 5:
                mAltimeter.SetFilterSettlingTime(10.0f);
                break;
        }
        float millibars_of_pressure = event.values[0];
        float deltaTime = event.timestamp - mLastTimestamp;
        deltaTime /= 1000000.0f;
        if (mSettings.getBoolean("show_status", true)) {
            mText.setText(String.format("Status: Pressure = %04.2fhPa; Time interval = %.1fms (%.0fHz)", millibars_of_pressure, deltaTime, 1000 / deltaTime));
        } else {
            mText.setText(getString(R.string.not_approved_for_aircraft_navigation_use));
        }
        mLastTimestamp = event.timestamp;
        mAltimeter.SetDisplayInFeet(mSettings.getBoolean("display_feet", true));
        mAltimeter.SetDisplayGraph(mSettings.getBoolean("display_alt_history", true));
        mVSI.setDisplayInFeet(mSettings.getBoolean("display_feet", true));
        mVSI.setDisplayGraph(mSettings.getBoolean("display_vsi_history", true));
        mAltimeter.setPressure(millibars_of_pressure, event.timestamp);
        if (mVSI != null) {
            mVSI.setPressure(millibars_of_pressure, event.timestamp);
        }
        if (LoggingActive) {
            if (mLogging) {
                if (mLogEnd == 0) {
                    mLogEnd = event.timestamp + 305 * 1000000000L;
                }
                if (event.timestamp > mLogEnd) {
                    try {
                        mBufferedWriter.flush();
                        mBufferedWriter.close();
                        Toast.makeText(getApplicationContext(),
                                "Logging complete", Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mLogging = false;
                } else {
                    try {
                        mBufferedWriter.write(String.format("%f,%f\n",
                                (double) event.timestamp / 1000000000.0,
                                millibars_of_pressure));
                    } catch (IOException e) {
                        e.printStackTrace();
                        mLogging = false;
                    }
                }
            }
        }
        if (mAltimeter != null) {
            mAltimeter.invalidate();
        }
        if (mVSI != null) {
            if (!mSettings.getBoolean("show_vsi", true)) {
                mVSI.setVisibility(View.GONE);
            } else {
                mVSI.setVisibility(View.VISIBLE);
            }
            mVSI.invalidate();
        }
    }
}

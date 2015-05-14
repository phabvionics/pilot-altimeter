package uk.co.phabvionics.pilotaltimeter;

import android.graphics.Paint;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 */

/**
 * @author david
 *
 */
public class Altimeter
{
    private static final Altimeter INSTANCE = new Altimeter();
    public static Altimeter getInstance()
    {
        return INSTANCE;
    }

    private float mPressureDatum; // Pressure datum is defined in hectoPascals (equivalent to millibars).
    private float mPressureMeasurement; // Pressure measurement is defined in hectoPascals (equivalent to millibars).
    private boolean mPressureMeasurementValid; // Set to true when pressure measurement has been set.
    private Paint mWhitePaint;
    private Paint mTickPaint;
    private Paint mBlackPaint;
    private Paint mGraphPaint;
    private Paint mGreyLeftPaint;
    private Paint mGreyMidPaint;
    private Paint mGreyRightPaint;
    private Paint mAltitudePaint;
    private Paint mDatumPaint;
    private float mSize;
    private float mxCentre;
    private float myCentre;
    private float mxAltPos;
    private float myAltPos;
    private float mxFLPos;
    private float myFLPos;
    private float mxDatumPos;
    private float myDatumPos;
    private float mxDatumTextPos;
    private float myDatumTextPos;
    private float mxTimePos;
    private float myTimePos;
    private Paint mTimePaint;
    private float[] mTickCoords;
    private float[] mDotCoords;
    private float[] mNumberCoords;
    private boolean mMetricMode;
    private long mTimestamp;
    private long mSampleTimestamp;
    private float mFilteredPressure;
    private static final long TIME_STEP = 10000000; // 10ms
    private IIRLowPassFilter mFilter;
    private FilterStrength mFilterStrength;
    private String mDatumText;
    private boolean mDisplayInFeet;
    private boolean mDisplayGraph;
    private int mWidth;
    private int mHeight;
    private float mGraphTop;
    private float mGraphBottom;
    private float mGraphLeft;
    private float mGraphRight;
    private float[] mHistory;
    private int mHistorySize;
    private long mHistoryEndTime;
    private static final long HISTORY_TIME_STEP = 500000000; // 500ms

    public boolean isPressureMeasurementValid() {
        return mPressureMeasurementValid;
    }

    public float getFilteredPressure() {
        return mFilteredPressure;
    }

    public boolean isMetricPressure() {
        return mMetricMode;
    }

    public String getDatumText() {
        return mDatumText;
    }

    public float[] getHistory() {
        return mHistory;
    }

    public int getHistorySize() {
        return mHistorySize;
    }

    private boolean mStopped = true;

    public boolean isStopped() {
        return mStopped;
    }

    public boolean isDisplayGraph() {
        return mDisplayGraph;
    }

    public enum FilterStrength {
        FILTER_OFF,
        FILTER_WEAK,
        FILTER_MEDIUM,
        FILTER_STRONG,
        FILTER_VERY_STRONG
    }

    private Altimeter() {
        mPressureMeasurementValid = false;
        mMetricMode = true;
        mPressureDatum = 1013.25f;
        SetFilterStrength(FilterStrength.FILTER_MEDIUM);
        mHistorySize = 61;
        mHistory = new float[mHistorySize];
    }

    public void SetDisplayInFeet(boolean displayOption) {
        mDisplayInFeet = displayOption;
    }

    public void SetDisplayGraph(boolean displayOption) {
        mDisplayGraph = displayOption;
    }

    public void SetDatumText(String datumText) {
        mDatumText = datumText;
    }

    /**
     * Set the time it takes for the filtered sample to reach 99.3%
     * of the difference between the filtered sample at the start of
     * the time period and the unfiltered sample (5 time constants)
     * @param strength FilterStrength.FILTER_xMS where x is 0, 500, 1000, 1500 or 2000
     */
    public void SetFilterStrength(FilterStrength strength) {
        if (mFilterStrength != strength) {
            switch (strength) {
                case FILTER_OFF:
                    break;
                case FILTER_WEAK:
                    mFilter = new IIRLowPassFilter(10); // time constant = 10 samples; settling time = 0.25s
                    break;
                case FILTER_MEDIUM:
                    mFilter = new IIRLowPassFilter(20); // time constant = 20 samples; settling time = 0.5s
                    break;
                case FILTER_STRONG:
                    mFilter = new IIRLowPassFilter(40); // time constant = 40 samples; settling time = 1s
                    break;
                case FILTER_VERY_STRONG:
                    mFilter = new IIRLowPassFilter(400); // time constant = 400 samples; settling time = 10s
                    break;
            }
            mFilterStrength = strength;
        }
    }

    private float mSettlingTime;

    public void SetFilterSettlingTime(float settlingTime) {
        if (mSettlingTime != settlingTime) {
            mSettlingTime = settlingTime;
            mFilter = new IIRLowPassFilter(settlingTime * 20); // time constant = 20 * settlingTime samples in seconds
            // settling time is considered to be 5 time constants
        }
    }

    /**
     * Set the pressure datum (the pressure at which the altimeter shows zero altitude).
     * @param pressure The pressure datum to set in hectoPascals (equivalent to millibars).
     */
    public void setPressureDatum(float pressure) {
        mPressureDatum = pressure;
    }

    public float getPressureDatum() {
        return mPressureDatum;
    }

    public float Add1ToPressureDatum() {
        if (mMetricMode) {
            mPressureDatum = (float) Math.ceil(mPressureDatum + 0.01);
        } else {
            mPressureDatum = inHg_to_hPa((float) (Math.floor(hPa_to_inHg(mPressureDatum) * 100.0 + 1.01) / 100.0));
        }
        return mPressureDatum;
    }

    public float Add10ToPressureDatum() {
        if (mMetricMode) {
            mPressureDatum = (float) Math.ceil(mPressureDatum + 9.01);
        } else {
            mPressureDatum = inHg_to_hPa((float) (Math.floor(hPa_to_inHg(mPressureDatum) * 100.0 + 10.01) / 100.0));
        }
        return mPressureDatum;
    }

    public float Sub1FromPressureDatum() {
        if (mMetricMode) {
            mPressureDatum = (float) Math.floor(mPressureDatum - 0.01);
        } else {
            mPressureDatum = inHg_to_hPa((float) (Math.floor(hPa_to_inHg(mPressureDatum) * 100.0 - 0.01) / 100.0));
        }
        return mPressureDatum;
    }

    public float Sub10FromPressureDatum() {
        if (mMetricMode) {
            mPressureDatum = (float) Math.floor(mPressureDatum - 9.01);
        } else {
            mPressureDatum = inHg_to_hPa((float) (Math.floor(hPa_to_inHg(mPressureDatum) * 100.0 - 9.01) / 100.0));
        }
        return mPressureDatum;
    }

    public float setPressureAlt() {
        mPressureDatum = 1013.25f;
        return mPressureDatum;
    }

    private void filterPressure(long timestamp) {
        // Run the IIR filter on the pressure to bring it up to the given timestamp.
        while (mTimestamp + TIME_STEP < timestamp) {
            float pressure = mPressureMeasurement;
            pressure = mFilter.Filter(pressure);
            mFilteredPressure = pressure;
            mTimestamp += TIME_STEP;
            if (mTimestamp >= mHistoryEndTime + HISTORY_TIME_STEP) {
                mHistoryEndTime = mTimestamp;
                System.arraycopy(mHistory, 1, mHistory, 0, mHistorySize - 1);
                mHistory[mHistorySize - 1] = pressureToHeight(mPressureDatum, mFilteredPressure);
            }
        }
    }

    /**
     * The time before it is considered that the altimeter is no longer receiving data in ms.
     */
    private static final long STOPPED_RECEIVING_DATA_TIME = 1000;

    /**
     * Provide this object with the latest raw pressure measurement.
     * @param pressure Pressure in hPa.
     * @param timestamp Time in nanoseconds.
     */
    public void setPressure(float pressure, long timestamp) {
        if (!mPressureMeasurementValid) {
            mFilteredPressure = pressure;
            mPressureMeasurementValid = true;
            mTimestamp = timestamp;
        }
        mPressureMeasurement = pressure;
        mSampleTimestamp = timestamp;
        filterPressure(timestamp);
        mStopped = false;
        if (mIndicateStoppedThread != null) mIndicateStoppedThread.cancel(true);
        mIndicateStoppedThread = mExecutor.schedule(mIndicateStopped, STOPPED_RECEIVING_DATA_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * The Runnable that indicates that data is no longer being received.
     */
    private final ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(1);
    private final IndicateStopped mIndicateStopped = new IndicateStopped();
    private ScheduledFuture<?> mIndicateStoppedThread;
    class IndicateStopped implements Runnable
    {
        @Override
        public void run()
        {
            mStopped = true;
        }
    }

    public void setMetric(boolean metric) {
        mMetricMode = metric;
    }

    /**
     * Convert hPa to inches of mercury (1013.25hPa = 29.92126inHg)
     * @param hPa The pressure in hectopascals.
     * @return The pressure in inches of mercury.
     */
    static float hPa_to_inHg(float hPa) {
        return hPa / 1013.25f * 29.92126f;
    }

    static float inHg_to_hPa(float inHg) {
        return inHg / 29.92126f * 1013.25f;
    }

    /**
     * Convert hPa to atmospheres (1 atmosphere = 1013.25hPa)
     * @param hPa Hectopascals to convert
     * @return The number of atmospheres calculated
     */
    static float hPa_to_atm(float hPa) {
        return hPa / 1013.25f;
    }

    /**
     * From the information already set, calculate the indicated altitude
     * given the atmospheric pressure and the pressure datum set (this is
     * what would be set in the Kollsman window of an altimeter).
     * @return The height of indicated altitude in feet.
     */
    float pressureToHeight(float datum, float measurement) {
        // Formula derived from the Aviation Formulary.
        float pressureDatumAtm = hPa_to_atm(datum);
        float pressureAtm = hPa_to_atm(measurement);
        float pressureAlt = (float) Math.pow(pressureAtm, 1/5.2558797);
        pressureAlt = 1 - pressureAlt;
        pressureAlt = pressureAlt / 6.8755856e-6f;
        float pressureAltCorr = (float) Math.pow(pressureDatumAtm, 1/5.2558797);
        pressureAltCorr = 1 - pressureAltCorr;
        pressureAltCorr = pressureAltCorr / 6.8755856e-6f;
        float indicatedAlt = pressureAlt - pressureAltCorr;

        // If display is in metres, convert height from feet to metres
        if (!mDisplayInFeet) {
            indicatedAlt = indicatedAlt * 12.0f * 25.4f / 1000.0f;
        }

        return indicatedAlt;
    }

    public boolean isDisplayInFeet() {
        return mDisplayInFeet;
    }
}

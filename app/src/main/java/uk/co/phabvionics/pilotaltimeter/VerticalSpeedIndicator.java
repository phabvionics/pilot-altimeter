package uk.co.phabvionics.pilotaltimeter;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class VerticalSpeedIndicator
{
    private static final VerticalSpeedIndicator INSTANCE = new VerticalSpeedIndicator();

    private float mPressureMeasurement; // Pressure measurement is defined in hectoPascals (equivalent to millibars).
    private long mTimestamp;
    private long mSampleTimestamp;
    private float mFilteredPressureHistory[];
    private static final long TIME_STEP = 10000000; // 10ms
    private IIRLowPassFilter mFilter;
    private static final int PRESSURE_HISTORY = 100;
    private static final int DELTA_SAMPLES = 100;
    private IIRLowPassFilter mOutputFilter;
    private float mVerticalSpeed;
    private boolean mDisplayInFeet;
    private boolean mDisplayGraph;
    private float[] mHistory;
    private int mHistorySize;
    private long mHistoryEndTime;
    private static final long HISTORY_TIME_STEP = 500000000; // 500ms
    private boolean mStopped;

    public VerticalSpeedIndicator() {
        mFilteredPressureHistory = new float[PRESSURE_HISTORY + 1];
        //mFilter = new IIRLowPassFilter(0.980198673307f); // time constant = 50 samples
        //mFilter = new IIRLowPassFilter(0.960789439152f); // time constant = 25 samples
        mFilter = new IIRLowPassFilter(100); // time constant = 100 samples
        //mFilter = new IIRLowPassFilter(0.01f); // time constant = 0 samples
        //mFilter = new IIRLowPassFilter(0.951229424501f); // time constant = 20 samples
        mOutputFilter = new IIRLowPassFilter(200); // time constant = 200 samples
        mDisplayGraph = true;
        mHistorySize = 61;
        mHistory = new float[mHistorySize];
    }

    public static VerticalSpeedIndicator getInstance() {
        return INSTANCE;
    }

    public void setDisplayInFeet(boolean displayOption) {
        if (mDisplayInFeet != displayOption) {
            mDisplayInFeet = displayOption;
        }
    }

    public void setDisplayGraph(boolean displayOption) {
        if (mDisplayGraph != displayOption) {
            mDisplayGraph = displayOption;
        }
    }

    public boolean isIndicationValid() {
        // If indication is valid, the oldest mFilteredPressureHistory value will be non-zero.
        return mFilteredPressureHistory[PRESSURE_HISTORY] != 0.0f;
    }

    private void filterPressure(long timestamp) {
        // Run the IIR filter on the pressure to bring it up to the given timestamp.
        while (mTimestamp + TIME_STEP < timestamp) {
            float pressure = mPressureMeasurement;
            pressure = mFilter.Filter(pressure);
            System.arraycopy(mFilteredPressureHistory, 0, mFilteredPressureHistory, 1, PRESSURE_HISTORY);
            mFilteredPressureHistory[0] = pressure;
            mTimestamp += TIME_STEP;
            if (isIndicationValid()) {
                mVerticalSpeed = mOutputFilter.Filter(calcVerticalSpeed());
                if (mTimestamp >= mHistoryEndTime + HISTORY_TIME_STEP) {
                    mHistoryEndTime = mTimestamp;
                    System.arraycopy(mHistory, 1, mHistory, 0, mHistorySize - 1);
                    mHistory[mHistorySize - 1] = mVerticalSpeed;
                }
            }
        }
    }

    /**
     * The time before it is considered that the altimeter is no longer receiving data in ms.
     */
    private static final long STOPPED_RECEIVING_DATA_TIME = 1000;

    public void setPressure(float pressure, long timestamp) {
        if (mTimestamp == 0) {
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

    public float getSpeed() {
        return calcVerticalSpeed();
    }

    public boolean isStopped() {
        return mStopped;
    }

    public int getHistorySize() {
        return mHistorySize;
    }

    public float[] getHistory() {
        return mHistory;
    }

    class IndicateStopped implements Runnable
    {
        @Override
        public void run()
        {
            mStopped = true;
        }
    }

    private float calcVerticalSpeed() {
        // Calculate the pressure delta over the last second.
        float result = mFilteredPressureHistory[PRESSURE_HISTORY] - mFilteredPressureHistory[PRESSURE_HISTORY - DELTA_SAMPLES];
        // Convert to the number of feet that have been traversed over the last second.
        result *= 27.3104136394385f / (DELTA_SAMPLES / 100.0f);
        // Convert to the number of feet that would be traversed over 60 seconds.
        result *= 60.0f;
        return result;
        // TEST: Sweep up and down slowly over a period of one minute.
//		long time = System.currentTimeMillis();
//		time = time % 60000;
//		float vs;
//		if (time < 15000) {
//			vs = time / 6;
//		} else if (time < 45000) {
//			vs = (30000 - time) / 6;
//		} else {
//			vs = (time - 60000) / 6;
//		}
//		return vs;
    }

    public boolean isDisplayGraph() {
        return mDisplayGraph;
    }

    public boolean isDisplayInFeet() {
        return mDisplayInFeet;
    }
}

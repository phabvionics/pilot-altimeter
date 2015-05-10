package uk.co.phabvionics.pilotaltimeter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 *
 */

/**
 * @author david
 *
 */
public class Altimeter extends View {
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

    public enum FilterStrength {
        FILTER_OFF,
        FILTER_WEAK,
        FILTER_MEDIUM,
        FILTER_STRONG,
        FILTER_VERY_STRONG
    };

    public Altimeter(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPressureMeasurementValid = false;
        mWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWhitePaint.setColor(Color.WHITE);
        mTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTickPaint.setColor(Color.WHITE);
        mTickPaint.setStrokeWidth(5.0f);
        mTickPaint.setStyle(Paint.Style.STROKE);
        mAltitudePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAltitudePaint.setColor(Color.WHITE);
        mAltitudePaint.setTextAlign(Paint.Align.CENTER);
        mDatumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDatumPaint.setColor(Color.WHITE);
        mDatumPaint.setTextAlign(Paint.Align.CENTER);
        mBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBlackPaint.setColor(Color.BLACK);
        mGraphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGraphPaint.setColor(Color.BLACK);
        mGraphPaint.setStrokeWidth(5.0f);
        mGreyLeftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGreyLeftPaint.setColor(Color.GRAY);
        mGreyMidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGreyMidPaint.setColor(Color.GRAY);
        mGreyMidPaint.setTextAlign(Paint.Align.CENTER);
        mGreyRightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGreyRightPaint.setColor(Color.GRAY);
        mGreyRightPaint.setTextAlign(Paint.Align.RIGHT);
        mTimePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTimePaint.setColor(Color.WHITE);
        mTimePaint.setTextAlign(Paint.Align.CENTER);
        mTickCoords = new float[40];
        mDotCoords = new float[80];
        mNumberCoords = new float[20];
        mMetricMode = true;
        mPressureDatum = 1013.25f;
        SetFilterStrength(FilterStrength.FILTER_MEDIUM);
        mDatumText = "Press Alt";
        mHistorySize = 61;
        mHistory = new float[mHistorySize];
    }

    public void SetDisplayInFeet(boolean displayOption) {
        if (mDisplayInFeet != displayOption) {
            mDisplayInFeet = displayOption;
            invalidate();
        }
    }

    public void SetDisplayGraph(boolean displayOption) {
        if (mDisplayGraph != displayOption) {
            mDisplayGraph = displayOption;
            setCoordinates();
            invalidate();
        }
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
        invalidate();
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
        invalidate();
        return mPressureDatum;
    }

    public float Add10ToPressureDatum() {
        if (mMetricMode) {
            mPressureDatum = (float) Math.ceil(mPressureDatum + 9.01);
        } else {
            mPressureDatum = inHg_to_hPa((float) (Math.floor(hPa_to_inHg(mPressureDatum) * 100.0 + 10.01) / 100.0));
        }
        invalidate();
        return mPressureDatum;
    }

    public float Sub1FromPressureDatum() {
        if (mMetricMode) {
            mPressureDatum = (float) Math.floor(mPressureDatum - 0.01);
        } else {
            mPressureDatum = inHg_to_hPa((float) (Math.floor(hPa_to_inHg(mPressureDatum) * 100.0 - 0.01) / 100.0));
        }
        invalidate();
        return mPressureDatum;
    }

    public float Sub10FromPressureDatum() {
        if (mMetricMode) {
            mPressureDatum = (float) Math.floor(mPressureDatum - 9.01);
        } else {
            mPressureDatum = inHg_to_hPa((float) (Math.floor(hPa_to_inHg(mPressureDatum) * 100.0 - 9.01) / 100.0));
        }
        invalidate();
        return mPressureDatum;
    }

    public float setPressureAlt() {
        mPressureDatum = 1013.25f;
        invalidate();
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
                for (int index = 0; index < mHistorySize - 1; index++) {
                    mHistory[index] = mHistory[index + 1];
                }
                mHistory[mHistorySize - 1] = pressureToHeight(mPressureDatum, mFilteredPressure);
            }
        }
    }

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
    }

    public void setMetric(boolean metric) {
        mMetricMode = metric;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        setCoordinates();
    }

    private void setCoordinates() {
        final float cx, cy, size;
        final float minGraphPixels = Math.max(mWidth, mHeight) / 4.0f;
        final float graphGuardPixels = 5.0f;
        // If mDisplayGraph is set,
        if (mDisplayGraph) {
            // If component wider than high or square,
            if (mWidth >= mHeight) {
                // altimeter to display to the right and graph to the left.
                size = Math.min(mWidth - minGraphPixels, mHeight);
                cx = mWidth - size / 2.0f;
                cy = size / 2.0f;
                mGraphTop = graphGuardPixels;
                mGraphBottom = mHeight - graphGuardPixels;
                mGraphLeft = graphGuardPixels;
                mGraphRight = mWidth - size - graphGuardPixels;
                // If component higher than width,
            } else {
                // altimeter to display to the top and graph to the bottom.
                size = Math.min(mWidth, mHeight - minGraphPixels);
                cx = mWidth / 2.0f;
                cy = size / 2.0f;
                mGraphTop = size + graphGuardPixels;
                mGraphBottom = mHeight - graphGuardPixels;
                mGraphLeft = graphGuardPixels;
                mGraphRight = mWidth - graphGuardPixels;
            }
            // If mDisplayGraph is cleared, altimeter should be centralised.
        } else {
            size = Math.min(mWidth, mHeight);
            cx = mWidth / 2.0f;
            cy = mHeight / 2.0f;
        }

        mSize = size;

        mxAltPos = cx;
        myAltPos = cy - 0.15f * size;

        mxFLPos = cx;
        myFLPos = cy + 0.17f * size;

        mxDatumPos = cx;
        myDatumPos = cy + 0.24f * size;

        mxDatumTextPos = cx;
        myDatumTextPos = cy + 0.31f * size;

        mxTimePos = cx;
        myTimePos = cy - 0.075f * size;

        float tickStart = size * 0.43f;
        float tickEnd = size * 0.47f;
        float numberCentre = size * 0.38f;
        for (int index = 0; index < 10; index++) {
            double angle = 2.0 * Math.PI * index / 10.0;

            mTickCoords[index * 4 + 0] = (float) (cx + tickStart * Math.sin(angle));
            mTickCoords[index * 4 + 1] = (float) (cy - tickStart * Math.cos(angle));
            mTickCoords[index * 4 + 2] = (float) (cx + tickEnd * Math.sin(angle));
            mTickCoords[index * 4 + 3] = (float) (cy - tickEnd * Math.cos(angle));
            mNumberCoords[index * 2 + 0] = (float) (cx + numberCentre * Math.sin(angle));
            mNumberCoords[index * 2 + 1] = (float) (cy - numberCentre * Math.cos(angle)) - mTickPaint.ascent();
            for (int subindex = 1; subindex < 5; subindex++) {
                int arrayIndex = 8 * index + (subindex - 1) * 2;
                angle = 2.0 * Math.PI * (index / 10.0 + subindex / 50.0);
                mDotCoords[arrayIndex++] = (float) (cx + tickEnd * Math.sin(angle));
                mDotCoords[arrayIndex++] = (float) (cy - tickEnd * Math.cos(angle));
            }
        }

        mAltitudePaint.setTextSize(0.15f * size);
        mDatumPaint.setTextSize(0.075f * size);
        mTimePaint.setTextSize(0.075f * size);
        mGreyLeftPaint.setTextSize(0.075f * size);
        mGreyMidPaint.setTextSize(0.075f * size);
        mGreyRightPaint.setTextSize(0.075f * size);
        mxCentre = cx;
        myCentre = cy;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw a circle.
        canvas.drawCircle(mxCentre, myCentre, mSize * 0.49f, mWhitePaint);
        canvas.drawCircle(mxCentre, myCentre, mSize * 0.48f, mBlackPaint);

        // Draw the tick lines and points.
        canvas.drawLines(mTickCoords, mTickPaint);
        canvas.drawPoints(mDotCoords, mTickPaint);

        // Draw the numbers from 0 to 9.
        for (int number = 0; number <= 9; number++) {
            canvas.drawText(Integer.toString(number), mNumberCoords[number * 2], mNumberCoords[number * 2 + 1], mDatumPaint);
        }

        float height = 0.0f;
        if (mPressureMeasurementValid) {
            height = pressureToHeight(mPressureDatum, mFilteredPressure);

            canvas.drawText(String.format("%.0f", height), mxAltPos, myAltPos, mAltitudePaint);
            if (mPressureDatum == 1013.25 && mDisplayInFeet) {
                canvas.drawText(String.format("FL%.0f", height / 100.0f), mxFLPos, myFLPos, mDatumPaint);
            }
            float angle = (float) ((height % 1000.0) / 1000.0 * Math.PI * 2.0);
            float x = (float) (mxCentre + mSize * (Math.sin(angle) * 0.45));
            float y = (float) (myCentre - mSize * (Math.cos(angle) * 0.45));
            canvas.drawLine(mxCentre, myCentre, x, y, mTickPaint);
            if (System.currentTimeMillis() - mSampleTimestamp / 1000000 >= 1000) {
                canvas.drawText("STOPPED", mxTimePos, myTimePos, mTimePaint);
            } else if (mDisplayInFeet) {
                canvas.drawText("FT", mxTimePos, myTimePos, mTimePaint);
            } else {
                canvas.drawText("METRES", mxTimePos, myTimePos, mTimePaint);
            }
        } else {
            canvas.drawText("No Data", mxAltPos, myAltPos, mAltitudePaint);
        }

        if (mPressureDatum == 1013.25) {
            canvas.drawText(mMetricMode ? "1013.25hPa" : "29.92126\"Hg", mxDatumPos, myDatumPos, mDatumPaint);
        } else if (mMetricMode) {
            canvas.drawText(String.format("%.0fhPa", mPressureDatum), mxDatumPos, myDatumPos, mDatumPaint);
        } else {
            canvas.drawText(String.format("%.2f\"Hg", hPa_to_inHg(mPressureDatum)), mxDatumPos, myDatumPos, mDatumPaint);
        }

        canvas.drawText(mDatumText, mxDatumTextPos, myDatumTextPos, mDatumPaint);

        // If the graph is to be displayed,
        if (mDisplayGraph) {
            // Determine the units in use.
            String units = mDisplayInFeet ? "ft" : "m";
            // Determine the scale.
            float scale = mDisplayInFeet ? 100 : 50;

            // Draw a box round the graph.
            canvas.drawLine(mGraphLeft, mGraphTop, mGraphRight, mGraphTop, mBlackPaint);
            canvas.drawLine(mGraphLeft, mGraphBottom, mGraphRight, mGraphBottom, mBlackPaint);
            canvas.drawLine(mGraphLeft, mGraphTop, mGraphLeft, mGraphBottom, mBlackPaint);
            canvas.drawLine(mGraphRight, mGraphTop, mGraphRight, mGraphBottom, mBlackPaint);
            // Find nearest display units.
            float mNearestHeight = Math.round(height / scale) * scale;
            float mTopHeight = mNearestHeight + scale * 2;
            // Draw x-axis parallels.
            float graphHeight = mGraphBottom - mGraphTop;
            canvas.drawLine(mGraphLeft, mGraphTop + graphHeight / 4.0f, mGraphRight, mGraphTop + graphHeight / 4.0f, mGreyLeftPaint);
            canvas.drawLine(mGraphLeft, mGraphTop + graphHeight / 2.0f, mGraphRight, mGraphTop + graphHeight / 2.0f, mGreyLeftPaint);
            canvas.drawLine(mGraphLeft, mGraphBottom - graphHeight / 4.0f, mGraphRight, mGraphBottom - graphHeight / 4.0f, mGreyLeftPaint);
            // Draw labels.
            canvas.drawText("" + (int)(mTopHeight - scale) + units, mGraphLeft, mGraphTop + graphHeight / 4.0f, mGreyLeftPaint);
            canvas.drawText("" + (int)(mTopHeight - 2 * scale) + units, mGraphLeft, mGraphTop + graphHeight / 2.0f, mGreyLeftPaint);
            canvas.drawText("" + (int)(mTopHeight - 3 * scale) + units, mGraphLeft, mGraphBottom - graphHeight / 4.0f, mGreyLeftPaint);
            // Draw y-axis parallels.
            float graphWidth = mGraphRight - mGraphLeft;
            for (int interval = 1; interval < 6; interval++) {
                canvas.drawLine(mGraphLeft + graphWidth * interval / 6.0f, mGraphTop, mGraphLeft + graphWidth * interval / 6.0f, mGraphBottom, mGreyLeftPaint);
            }
            // Draw labels.
            canvas.drawText("<-30s", mGraphLeft, mGraphBottom, mGreyLeftPaint);
            canvas.drawText("-15s", mGraphLeft + graphWidth / 2, mGraphBottom, mGreyMidPaint);
            canvas.drawText("Now>", mGraphRight, mGraphBottom, mGreyRightPaint);
            // Calculate the x- and y-coordinate of the first graph position.
            float x0 = mGraphLeft;
            float y0 = mGraphTop + (mTopHeight - mHistory[0]) / scale / 4 * graphHeight;
            // For each of the next coordinates,
            for (int index = 1; index < mHistorySize; index++) {
                // Calculate the x- and y-coordinate of the graph position.
                float x1 = mGraphLeft + graphWidth * ((float)index / (mHistorySize - 1));
                float y1 = mGraphTop + (mTopHeight - mHistory[index]) / scale / 4 * graphHeight;
                // Draw a line between the two coordinates.
                canvas.drawLine(x0, y0, x1, y1, mGraphPaint);
                // Retain memory of the last coordinate.
                x0 = x1;
                y0 = y1;
            }
        }
    }

    /**
     * Convert hPa to inches of mercury (1013.25hPa = 29.92126inHg)
     * @param hPa
     * @return
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
}

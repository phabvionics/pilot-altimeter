package uk.co.phabvionics.pilotaltimeter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class VerticalSpeedIndicator extends View {
    private float mPressureMeasurement; // Pressure measurement is defined in hectoPascals (equivalent to millibars).
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
    private float mxTimePos;
    private float myTimePos;
    private Paint mTimePaint;
    private float[] mTickCoords;
    private float[] mDotCoords;
    private float[] mNumberCoords;
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

    public VerticalSpeedIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);

        mFilteredPressureHistory = new float[PRESSURE_HISTORY + 1];
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
        mTickCoords = new float[36];
        mDotCoords = new float[32];
        mNumberCoords = new float[18];
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

    public void setDisplayInFeet(boolean displayOption) {
        if (mDisplayInFeet != displayOption) {
            mDisplayInFeet = displayOption;
            invalidate();
        }
    }

    public void setDisplayGraph(boolean displayOption) {
        if (mDisplayGraph != displayOption) {
            mDisplayGraph = displayOption;
            setCoordinates();
            invalidate();
        }
    }

    private boolean indicationValid() {
        // If indication is valid, the oldest mFilteredPressureHistory value will be non-zero.
        return mFilteredPressureHistory[PRESSURE_HISTORY] != 0.0f;
    }

    private void filterPressure(long timestamp) {
        // Run the IIR filter on the pressure to bring it up to the given timestamp.
        while (mTimestamp + TIME_STEP < timestamp) {
            float pressure = mPressureMeasurement;
            pressure = mFilter.Filter(pressure);
            for (int index = PRESSURE_HISTORY; index > 0; index--) {
                mFilteredPressureHistory[index] = mFilteredPressureHistory[index - 1];
            }
            mFilteredPressureHistory[0] = pressure;
            mTimestamp += TIME_STEP;
            if (indicationValid()) {
                mVerticalSpeed = mOutputFilter.Filter(calcVerticalSpeed());
                if (mTimestamp >= mHistoryEndTime + HISTORY_TIME_STEP) {
                    mHistoryEndTime = mTimestamp;
                    for (int index = 0; index < mHistorySize - 1; index++) {
                        mHistory[index] = mHistory[index + 1];
                    }
                    mHistory[mHistorySize - 1] = mVerticalSpeed;
                }
            }
        }
    }

    public void setPressure(float pressure, long timestamp) {
        if (mTimestamp == 0) {
            mTimestamp = timestamp;
        }
        mPressureMeasurement = pressure;
        mSampleTimestamp = timestamp;
        filterPressure(timestamp);
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
                // VSI to display to the right and graph to the left.
                size = Math.min(mWidth - minGraphPixels, mHeight);
                cx = mWidth - size / 2.0f;
                cy = size / 2.0f;
                mGraphTop = graphGuardPixels;
                mGraphBottom = mHeight - graphGuardPixels;
                mGraphLeft = graphGuardPixels;
                mGraphRight = mWidth - size - graphGuardPixels;
                // If component higher than width,
            } else {
                // VSI to display to the top and graph to the bottom.
                size = Math.min(mWidth, mHeight - minGraphPixels);
                cx = mWidth / 2.0f;
                cy = size / 2.0f;
                mGraphTop = size + graphGuardPixels;
                mGraphBottom = mHeight - graphGuardPixels;
                mGraphLeft = graphGuardPixels;
                mGraphRight = mWidth - graphGuardPixels;
            }
            // If mDisplayGraph is cleared, VSI should be centralised.
        } else {
            size = Math.min(mWidth, mHeight);
            cx = mWidth / 2.0f;
            cy = mHeight / 2.0f;
        }

        mSize = size;

        mxAltPos = cx;
        myAltPos = cy - 0.15f * size;

        mxTimePos = cx;
        myTimePos = cy - 0.075f * size;

        mAltitudePaint.setTextSize(0.15f * size);
        mDatumPaint.setTextSize(0.075f * size);
        mTimePaint.setTextSize(0.075f * size);

        float tickStart = size * 0.43f;
        float tickEnd = size * 0.47f;
        float numberCentre = size * 0.37f;

        float yOffset;
        Paint.FontMetrics fm;
        fm = mDatumPaint.getFontMetrics();
        yOffset = -(fm.ascent) * 0.4f;

        for (int index = 0; index < 9; index++) {
            double angle = (10 + 42.5 * index) / 360.0 * 2.0 * Math.PI;

            mTickCoords[index * 4 + 0] = (float) (cx + tickStart * Math.cos(angle));
            mTickCoords[index * 4 + 1] = (float) (cy - tickStart * Math.sin(angle));
            mTickCoords[index * 4 + 2] = (float) (cx + tickEnd * Math.cos(angle));
            mTickCoords[index * 4 + 3] = (float) (cy - tickEnd * Math.sin(angle));
            mNumberCoords[index * 2 + 0] = (float) (cx + numberCentre * Math.cos(angle));
            mNumberCoords[index * 2 + 1] = (float) (cy - numberCentre * Math.sin(angle)) + yOffset;// - mDatumPaint.ascent();
        }

        for (int index = 0; index < 8; index++) {
            double angle = (171.5 - 8.5 * (index + (index >= 4 ? 1 : 0))) / 360.0 * 2.0 * Math.PI;

            mDotCoords[index * 4 + 0] = (float) (cx + tickEnd * Math.cos(angle));
            mDotCoords[index * 4 + 1] = (float) (cy - tickEnd * Math.sin(angle));
            mDotCoords[index * 4 + 2] = (float) (cx + tickEnd * Math.cos(angle));
            mDotCoords[index * 4 + 3] = (float) (cy + tickEnd * Math.sin(angle));
        }

        mGreyLeftPaint.setTextSize(0.075f * size);
        mGreyMidPaint.setTextSize(0.075f * size);
        mGreyRightPaint.setTextSize(0.075f * size);
        mxCentre = cx;
        myCentre = cy;
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

    /**
     * Calculate the angle in radians for a given vertical speed.
     * @param verticalSpeed The vertical speed.
     * @return The angle that represents that vertical speed.
     */
    static float calcAngle(float verticalSpeed) {
        if (verticalSpeed > 2050) {
            verticalSpeed = 2050;
        } else if (verticalSpeed < -2050) {
            verticalSpeed = -2050;
        }
        float angle = (float) (Math.PI - verticalSpeed / 2000.0 * 17 / 18 * Math.PI);
        return angle;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw a circle.
        canvas.drawCircle(mxCentre, myCentre, mSize * 0.49f, mWhitePaint);
        canvas.drawCircle(mxCentre, myCentre, mSize * 0.48f, mBlackPaint);

        canvas.drawLines(mTickCoords, mTickPaint);

        canvas.drawPoints(mDotCoords, mTickPaint);

        for (int number = 0; number < 9; number++) {
            canvas.drawText(Integer.toString(20 - number * 5), mNumberCoords[number * 2], mNumberCoords[number * 2 + 1], mDatumPaint);
        }

        if (indicationValid()) {
            //canvas.drawText(String.format("%d", Math.round(speed / 10) * 10), mxAltPos, myAltPos, mAltitudePaint);
            float speed = mVerticalSpeed;
            if (!mDisplayInFeet) {
                speed = speed * 12.0f * 25.4f / 1000.0f;
            }
            canvas.drawText(String.format("%.0f", speed), mxAltPos, myAltPos, mAltitudePaint);
            float angle = calcAngle(speed);
            float x = (float) (mxCentre + mSize * (Math.cos(angle) * 0.45));
            float y = (float) (myCentre - mSize * (Math.sin(angle) * 0.45));
            canvas.drawLine(mxCentre, myCentre, x, y, mTickPaint);
            if (System.currentTimeMillis() - mSampleTimestamp / 1000000 >= 1000) {
                canvas.drawText("STOPPED", mxTimePos, myTimePos, mTimePaint);
            } else if (mDisplayInFeet) {
                canvas.drawText("FT/MIN", mxTimePos, myTimePos, mTimePaint);
            } else {
                canvas.drawText("METRES/MIN", mxTimePos, myTimePos, mTimePaint);
            }
        } else {
            canvas.drawText("No Data", mxAltPos, myAltPos, mAltitudePaint);
        }
        // If the graph is to be displayed,
        if (mDisplayGraph) {
            // Determine the units in use.
            String units = mDisplayInFeet ? "ft/min" : "m/min";
            // Determine the scale.
            int scaleMantissa = mDisplayInFeet ? 2 : 1;
            float scaleExponent = 2;
            float scale = scaleMantissa * (float)Math.pow(10, scaleExponent);

            for (int index = 0; index < mHistorySize; index++) {
                while (Math.abs(mHistory[index]) > scale) {
                    switch (scaleMantissa) {
                        case 1:
                            scaleMantissa = 2;
                            break;
                        case 2:
                            scaleMantissa = 5;
                            break;
                        case 5:
                            scaleMantissa = 1;
                            scaleExponent++;
                            break;
                    }
                    scale = scaleMantissa * (float)Math.pow(10, scaleExponent);
                }
            }

            // Draw a box round the graph.
            canvas.drawLine(mGraphLeft, mGraphTop, mGraphRight, mGraphTop, mBlackPaint);
            canvas.drawLine(mGraphLeft, mGraphBottom, mGraphRight, mGraphBottom, mBlackPaint);
            canvas.drawLine(mGraphLeft, mGraphTop, mGraphLeft, mGraphBottom, mBlackPaint);
            canvas.drawLine(mGraphRight, mGraphTop, mGraphRight, mGraphBottom, mBlackPaint);
            // Draw x-axis parallels.
            float graphHeight = mGraphBottom - mGraphTop;
            canvas.drawLine(mGraphLeft, mGraphTop + graphHeight / 4.0f, mGraphRight, mGraphTop + graphHeight / 4.0f, mGreyLeftPaint);
            canvas.drawLine(mGraphLeft, mGraphTop + graphHeight / 2.0f, mGraphRight, mGraphTop + graphHeight / 2.0f, mGreyLeftPaint);
            canvas.drawLine(mGraphLeft, mGraphBottom - graphHeight / 4.0f, mGraphRight, mGraphBottom - graphHeight / 4.0f, mGreyLeftPaint);
            // Draw labels.
            canvas.drawText("+" + (int)(scale / 2.0f) + units, mGraphLeft, mGraphTop + graphHeight / 4.0f, mGreyLeftPaint);
            canvas.drawText("+0" + units, mGraphLeft, mGraphTop + graphHeight / 2.0f, mGreyLeftPaint);
            canvas.drawText("-" + (int)(scale / 2.0f) + units, mGraphLeft, mGraphBottom - graphHeight / 4.0f, mGreyLeftPaint);
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
            float y0 = mGraphTop + (1 - mHistory[0] / scale) * graphHeight / 2.0f;
            // For each of the next coordinates,
            for (int index = 1; index < mHistorySize; index++) {
                // Calculate the x- and y-coordinate of the graph position.
                float x1 = mGraphLeft + graphWidth * ((float)index / (mHistorySize - 1));
                float y1 = mGraphTop + (1 - mHistory[index] / scale) * graphHeight / 2.0f;
                // Draw a line between the two coordinates.
                canvas.drawLine(x0, y0, x1, y1, mGraphPaint);
                // Retain memory of the last coordinate.
                x0 = x1;
                y0 = y1;
            }
        }
    }
}

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
public class AltimeterView extends View {
    private final Altimeter mAltimeter;
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
    private long mSampleTimestamp;
    private int mWidth;
    private int mHeight;
    private float mGraphTop;
    private float mGraphBottom;
    private float mGraphLeft;
    private float mGraphRight;
    private static final long HISTORY_TIME_STEP = 500000000; // 500ms
    private int mDisplayGraphSeconds;

    public AltimeterView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAltimeter = Altimeter.getInstance();
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
        if (mAltimeter.getDisplayGraphSeconds() > 0) {
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

            mTickCoords[index * 4    ] = (float) (cx + tickStart * Math.sin(angle));
            mTickCoords[index * 4 + 1] = (float) (cy - tickStart * Math.cos(angle));
            mTickCoords[index * 4 + 2] = (float) (cx + tickEnd * Math.sin(angle));
            mTickCoords[index * 4 + 3] = (float) (cy - tickEnd * Math.cos(angle));
            mNumberCoords[index * 2    ] = (float) (cx + numberCentre * Math.sin(angle));
            mNumberCoords[index * 2 + 1] = (float) (cy - numberCentre * Math.cos(angle)) - mTickPaint.ascent();
            for (int subindex = 1; subindex < 5; subindex++) {
                int arrayIndex = 8 * index + (subindex - 1) * 2;
                angle = 2.0 * Math.PI * (index / 10.0 + subindex / 50.0);
                mDotCoords[arrayIndex++] = (float) (cx + tickEnd * Math.sin(angle));
                mDotCoords[arrayIndex  ] = (float) (cy - tickEnd * Math.cos(angle));
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
        if (mAltimeter.getDisplayGraphSeconds() != mDisplayGraphSeconds)
        {
            mDisplayGraphSeconds = mAltimeter.getDisplayGraphSeconds();
            setCoordinates();
        }
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
        float pressureDatum = mAltimeter.getPressureDatum();
        if (mAltimeter.isPressureMeasurementValid()) {
            height = pressureToHeight(pressureDatum, mAltimeter.getFilteredPressure());

            canvas.drawText(String.format("%.0f", height), mxAltPos, myAltPos, mAltitudePaint);
            if (pressureDatum == 1013.25 && mAltimeter.isDisplayInFeet()) {
                canvas.drawText(String.format("FL%.0f", height / 100.0f), mxFLPos, myFLPos, mDatumPaint);
            }
            float angle = (float) ((height % 1000.0) / 1000.0 * Math.PI * 2.0);
            float x = (float) (mxCentre + mSize * (Math.sin(angle) * 0.45));
            float y = (float) (myCentre - mSize * (Math.cos(angle) * 0.45));
            canvas.drawLine(mxCentre, myCentre, x, y, mTickPaint);
            if (mAltimeter.isStopped()) {
                canvas.drawText("STOPPED", mxTimePos, myTimePos, mTimePaint);
            } else if (mAltimeter.isDisplayInFeet()) {
                canvas.drawText("FT", mxTimePos, myTimePos, mTimePaint);
            } else {
                canvas.drawText("METRES", mxTimePos, myTimePos, mTimePaint);
            }
        } else {
            canvas.drawText("No Data", mxAltPos, myAltPos, mAltitudePaint);
        }

        if (pressureDatum == 1013.25) {
            canvas.drawText(mAltimeter.isMetricPressure() ? "1013.25hPa" : "29.92126\"Hg", mxDatumPos, myDatumPos, mDatumPaint);
        } else if (mAltimeter.isMetricPressure()) {
            canvas.drawText(String.format("%.0fhPa", pressureDatum), mxDatumPos, myDatumPos, mDatumPaint);
        } else {
            canvas.drawText(String.format("%.2f\"Hg", Altimeter.hPa_to_inHg(pressureDatum)), mxDatumPos, myDatumPos, mDatumPaint);
        }

        canvas.drawText(mAltimeter.getDatumText(), mxDatumTextPos, myDatumTextPos, mDatumPaint);

        // If the graph is to be displayed,
        if (mAltimeter.getDisplayGraphSeconds() > 0) {
            // Determine the units in use.
            String units = mAltimeter.isDisplayInFeet() ? "ft" : "m";
            // Determine the scale.
            float scale = mAltimeter.isDisplayInFeet() ? 100 : 50;

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
            int interval = mAltimeter.getDisplayGraphSeconds();
            canvas.drawText("<-" + interval + "s", mGraphLeft, mGraphBottom, mGreyLeftPaint);
            canvas.drawText("-" + (interval / 2) + "s", mGraphLeft + graphWidth / 2, mGraphBottom, mGreyMidPaint);
            canvas.drawText("Now>", mGraphRight, mGraphBottom, mGreyRightPaint);
            // Calculate the x- and y-coordinate of the first graph position.
            float x0 = mGraphLeft;
            float[] history = mAltimeter.getHistory();
            float y0 = mGraphTop + (mTopHeight - history[0]) / scale / 4 * graphHeight;
            // For each of the next coordinates,
            int historySize = mAltimeter.getHistorySize();
            for (int index = 1; index < historySize; index++) {
                // Calculate the x- and y-coordinate of the graph position.
                float x1 = mGraphLeft + graphWidth * ((float)index / (historySize - 1));
                float y1 = mGraphTop + (mTopHeight - history[index]) / scale / 4 * graphHeight;
                // Draw a line between the two coordinates.
                canvas.drawLine(x0, y0, x1, y1, mGraphPaint);
                // Retain memory of the last coordinate.
                x0 = x1;
                y0 = y1;
            }
        }
    }

    /**
     * From the information already set, calculate the indicated altitude
     * given the atmospheric pressure and the pressure datum set (this is
     * what would be set in the Kollsman window of an altimeter).
     * @return The height of indicated altitude in feet.
     */
    float pressureToHeight(float datum, float measurement) {
        // Formula derived from the Aviation Formulary.
        float pressureDatumAtm = Altimeter.hPa_to_atm(datum);
        float pressureAtm = Altimeter.hPa_to_atm(measurement);
        float pressureAlt = (float) Math.pow(pressureAtm, 1/5.2558797);
        pressureAlt = 1 - pressureAlt;
        pressureAlt = pressureAlt / 6.8755856e-6f;
        float pressureAltCorr = (float) Math.pow(pressureDatumAtm, 1/5.2558797);
        pressureAltCorr = 1 - pressureAltCorr;
        pressureAltCorr = pressureAltCorr / 6.8755856e-6f;
        float indicatedAlt = pressureAlt - pressureAltCorr;

        // If display is in metres, convert height from feet to metres
        if (!mAltimeter.isDisplayInFeet()) {
            indicatedAlt = indicatedAlt * 12.0f * 25.4f / 1000.0f;
        }

        return indicatedAlt;
    }
}

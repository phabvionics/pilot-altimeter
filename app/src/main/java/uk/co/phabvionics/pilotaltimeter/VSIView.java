package uk.co.phabvionics.pilotaltimeter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class VSIView extends View {
    private VerticalSpeedIndicator mVSI = VerticalSpeedIndicator.getInstance();
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
    private int mWidth;
    private int mHeight;
    private float mGraphTop;
    private float mGraphBottom;
    private float mGraphLeft;
    private float mGraphRight;
    private int mDisplayGraphSeconds;

    public VSIView(Context context, AttributeSet attrs) {
        super(context, attrs);

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
        if (mVSI.getDisplayGraphSeconds() > 0) {
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

            mTickCoords[index * 4    ] = (float) (cx + tickStart * Math.cos(angle));
            mTickCoords[index * 4 + 1] = (float) (cy - tickStart * Math.sin(angle));
            mTickCoords[index * 4 + 2] = (float) (cx + tickEnd * Math.cos(angle));
            mTickCoords[index * 4 + 3] = (float) (cy - tickEnd * Math.sin(angle));
            mNumberCoords[index * 2    ] = (float) (cx + numberCentre * Math.cos(angle));
            mNumberCoords[index * 2 + 1] = (float) (cy - numberCentre * Math.sin(angle)) + yOffset;// - mDatumPaint.ascent();
        }

        for (int index = 0; index < 8; index++) {
            double angle = (171.5 - 8.5 * (index + (index >= 4 ? 1 : 0))) / 360.0 * 2.0 * Math.PI;

            mDotCoords[index * 4    ] = (float) (cx + tickEnd * Math.cos(angle));
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
        return (float) (Math.PI - verticalSpeed / 2000.0 * 17 / 18 * Math.PI);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mVSI.getDisplayGraphSeconds() != mDisplayGraphSeconds)
        {
            mDisplayGraphSeconds = mVSI.getDisplayGraphSeconds();
            setCoordinates();
        }
        // Draw a circle.
        canvas.drawCircle(mxCentre, myCentre, mSize * 0.49f, mWhitePaint);
        canvas.drawCircle(mxCentre, myCentre, mSize * 0.48f, mBlackPaint);

        canvas.drawLines(mTickCoords, mTickPaint);

        canvas.drawPoints(mDotCoords, mTickPaint);

        for (int number = 0; number < 9; number++) {
            canvas.drawText(Integer.toString(20 - number * 5), mNumberCoords[number * 2], mNumberCoords[number * 2 + 1], mDatumPaint);
        }

        boolean displayInFeet = mVSI.isDisplayInFeet();
        if (mVSI.isIndicationValid()) {
            //canvas.drawText(String.format("%d", Math.round(speed / 10) * 10), mxAltPos, myAltPos, mAltitudePaint);
            float speed = mVSI.getSpeed();
            if (!displayInFeet) {
                speed = speed * 12.0f * 25.4f / 1000.0f;
            }
            canvas.drawText(String.format("%.0f", speed), mxAltPos, myAltPos, mAltitudePaint);
            float angle = calcAngle(speed);
            float x = (float) (mxCentre + mSize * (Math.cos(angle) * 0.45));
            float y = (float) (myCentre - mSize * (Math.sin(angle) * 0.45));
            canvas.drawLine(mxCentre, myCentre, x, y, mTickPaint);
            if (mVSI.isStopped()) {
                canvas.drawText("STOPPED", mxTimePos, myTimePos, mTimePaint);
            } else if (displayInFeet) {
                canvas.drawText("FT/MIN", mxTimePos, myTimePos, mTimePaint);
            } else {
                canvas.drawText("METRES/MIN", mxTimePos, myTimePos, mTimePaint);
            }
        } else {
            canvas.drawText("No Data", mxAltPos, myAltPos, mAltitudePaint);
        }
        // If the graph is to be displayed,
        if (mVSI.getDisplayGraphSeconds() > 0) {
            // Determine the units in use.
            String units = displayInFeet ? "ft/min" : "m/min";
            // Determine the scale.
            int scaleMantissa = displayInFeet ? 2 : 1;
            float scaleExponent = 2;
            float scale = scaleMantissa * (float)Math.pow(10, scaleExponent);

            int historySize = mVSI.getHistorySize();
            float[] history = mVSI.getHistory();
            for (int index = 0; index < historySize; index++) {
                while (Math.abs(history[index]) > scale) {
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
            int interval = mVSI.getDisplayGraphSeconds();
            canvas.drawText("<-" + interval + "s", mGraphLeft, mGraphBottom, mGreyLeftPaint);
            canvas.drawText("-" + (interval / 2) + "s", mGraphLeft + graphWidth / 2, mGraphBottom, mGreyMidPaint);
            canvas.drawText("Now>", mGraphRight, mGraphBottom, mGreyRightPaint);
            // Calculate the x- and y-coordinate of the first graph position.
            float x0 = mGraphLeft;
            float y0 = mGraphTop + (1 - history[0] / scale) * graphHeight / 2.0f;
            // For each of the next coordinates,
            for (int index = 1; index < historySize; index++) {
                // Calculate the x- and y-coordinate of the graph position.
                float x1 = mGraphLeft + graphWidth * ((float)index / (historySize - 1));
                float y1 = mGraphTop + (1 - history[index] / scale) * graphHeight / 2.0f;
                // Draw a line between the two coordinates.
                canvas.drawLine(x0, y0, x1, y1, mGraphPaint);
                // Retain memory of the last coordinate.
                x0 = x1;
                y0 = y1;
            }
        }
    }
}

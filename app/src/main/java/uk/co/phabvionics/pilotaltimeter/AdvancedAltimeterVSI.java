/**
 *
 */
package uk.co.phabvionics.pilotaltimeter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author david
 *
 */
public class AdvancedAltimeterVSI extends View {
    int mWidth;
    int mHeight;
    Paint mPaint;

    public AdvancedAltimeterVSI(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Use the maximum space allowed.
        int width;
        int height;
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            width = 999999;
        } else {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            height = 999999;
        } else {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO: Replace with display.
        canvas.drawLine(0, mHeight - 1, mWidth - 1, 0, mPaint);
        canvas.drawLine(0, 0, mWidth - 1, mHeight - 1, mPaint);
        canvas.drawLine(0, 0, mWidth - 1, 0, mPaint);
        canvas.drawLine(mWidth - 1, 0, mWidth - 1, mHeight - 1, mPaint);
        canvas.drawLine(mWidth - 1, mHeight - 1, 0, mHeight - 1, mPaint);
        canvas.drawLine(0, mHeight - 1, 0, 0, mPaint);
    }
}

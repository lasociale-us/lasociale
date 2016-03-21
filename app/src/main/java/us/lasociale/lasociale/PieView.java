package us.lasociale.lasociale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by stefkolman on 15/03/16.
 */
public class PieView extends View {

    Paint mPaint;
    RectF rectf = new RectF(36, 48, 325, 330);
    Context mContext;

    public PieView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLUE);

        mContext = context;

    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(Color.RED);
        RectF r = new RectF(10,10,600,600);
        float f = ActivityStorage.ReadActivity(mContext);
        canvas.drawArc(r, -90f, f * 360, true, mPaint);
    }

}

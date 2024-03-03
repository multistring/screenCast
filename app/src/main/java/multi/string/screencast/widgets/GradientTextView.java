package multi.string.screencast.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;

public class GradientTextView extends androidx.appcompat.widget.AppCompatTextView {
    /**
     * 画笔
     */
    private Paint mPaint;
    /**
     * 宽度
     */
    private int mViewWidth;
    /**
     * 线性渐变对象
     */
    private LinearGradient mLinearGradient;
    /**
     * 矩阵对象
     */
    private Matrix mGradientMatrix;
    /**
     * 平移距离
     */
    private int mTranslate;

    public GradientTextView(Context context) {
        super(context);
    }

    public GradientTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GradientTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 测量出文本宽度后,再初始化画笔等基础设置
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mViewWidth == 0){
            mViewWidth = getMeasuredWidth();
            if (mViewWidth > 0){
                mPaint = getPaint();
                mLinearGradient = new LinearGradient(
                        0,
                        0,
                        mViewWidth,
                        0,
                        new int[]{Color.WHITE,0xffffff,Color.WHITE},
                        null ,
                        Shader.TileMode.CLAMP);
                mPaint.setShader(mLinearGradient);
                mGradientMatrix = new Matrix();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //绘制文字之前
        super.onDraw(canvas);
        //绘制文字之后
        if (mGradientMatrix != null){
            mTranslate += mViewWidth/5;
            if (mTranslate > 2 * mViewWidth){//决定文字闪烁的频繁:快慢
                mTranslate =  -mViewWidth;
            }
            mGradientMatrix.setTranslate(mTranslate,0);
            mLinearGradient.setLocalMatrix(mGradientMatrix);
            postInvalidateDelayed(100);
        }
    }
}
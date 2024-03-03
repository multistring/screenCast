package multi.string.screencast.utils;

import android.content.Context;
import android.util.TypedValue;

public class DensityUtil {
    public  static int getStatusHeightByDensity(Context context){
        int statusBarHeight1 = -1;
        //获取status_bar_height资源的ID
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            //根据资源ID获取响应的尺寸值
            statusBarHeight1 = context.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight1;
    }


    /**
     * dp转px
     *
     * @param context
     * @return
     */
    public static int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, context.getResources()
                .getDisplayMetrics());
    }

    /**
     * sp转px
     *
     * @param context
     * @return
     */
    public static int sp2px(Context context, float spVal) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spVal * fontScale + 0.5f);
    }

    /**
     * px转dp
     *
     * @param context
     * @param pxVal
     * @return
     */
    public static float px2dp(Context context, float pxVal) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (pxVal / scale);
    }

    /**
     * px转sp
     *
     * @param pxVal
     * @return
     */
    public static float px2sp(Context context, float pxVal) {
        return (pxVal / context.getResources().getDisplayMetrics().scaledDensity);
    }
    /**
     * 得到屏幕宽度
     *
     * @param context
     * @return
     */
    public static int getDisplayWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 得到屏幕高度
     *
     * @param context
     * @return
     */
    public static int getDisplayHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
}

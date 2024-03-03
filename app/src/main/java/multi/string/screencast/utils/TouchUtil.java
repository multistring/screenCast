package multi.string.screencast.utils;

public class TouchUtil {
    // 两次点击按钮之间的点击间隔不能少于1000毫秒
    private static final int MIN_CLICK_DELAY_TIME = 500;
    private static long lastClickTime;
    private static final int MIN_CLICK_DELAY_TIME_200 = 200;
    public static boolean isFastClick() {
        boolean flag = false;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
            flag = true;
        }
        lastClickTime = curClickTime;
        return flag;
    }

    public static boolean isFastClickFor200() {
        boolean flag = false;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME_200) {
            flag = true;
        }
        lastClickTime = curClickTime;
        return flag;
    }
}


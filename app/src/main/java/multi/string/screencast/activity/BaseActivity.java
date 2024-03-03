package multi.string.screencast.activity;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.gyf.immersionbar.ImmersionBar;

import multi.string.screencast.R;

public abstract class BaseActivity extends FragmentActivity {
    public Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
    }

    /**
     * @param isFullStatus     是否是全屏
     * @param hasNavigationBar //是否存在虚拟返回键
     */
    public void immersionBar(boolean isFullStatus, boolean hasNavigationBar) {
        ImmersionBar immer = ImmersionBar.with(this).transparentBar();
        if (!isFullStatus) {
            immer.statusBarColor(R.color.color_ffffff).fitsSystemWindows(true).fullScreen(false);
        } else {
            if (hasNavigationBar) {
                immer.fullScreen(false);
            }
        }
        immer.statusBarDarkFont(true).keyboardEnable(ImmersionBar.hasNavigationBar(this)).init();
    }
}

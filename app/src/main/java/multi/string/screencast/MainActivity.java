package multi.string.screencast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaCodecInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.flyco.tablayout.CommonTabLayout;
import com.flyco.tablayout.listener.CustomTabEntity;
import com.flyco.tablayout.listener.OnTabSelectListener;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.multimedia.lib.screening.DLNAManager;
import com.multimedia.lib.screening.DLNAPlayer;
import com.yuyh.library.imgsel.ISNav;
import com.yuyh.library.imgsel.common.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import multi.string.screencast.activity.FreeScreenActivity;
import multi.string.screencast.dlna.DlnaWrapper;
import multi.string.screencast.fragment.AudioFragment;
import multi.string.screencast.fragment.PhotoFolderFragment;
import multi.string.screencast.fragment.VideoFragment;
import multi.string.screencast.utils.PermissionInterceptor;
import multi.string.screencast.utils.TouchUtil;
import multi.string.screencast.widgets.StatusBar;
import multi.string.screencast.widgets.TabEntity;

public class MainActivity extends FragmentActivity{
    private static final String TAG = "multistring";
    private ViewPager viewPager;
    private ArrayList<Fragment> mFragments2 = new ArrayList<>();
    //点击相关
    private Lock mClickLock = new ReentrantLock();
    private boolean mClickPressed = false;

    //核心功能
    private Button screen_mirror = null;  //镜像投屏
    private Button screen_free   = null;

    private int[] mIconUnselectIds = {
            R.drawable.fp_video_unselected, R.drawable.fp_pic_unselected,
            R.drawable.fp_audio_unselected};
    private int[] mIconSelectIds = {
            R.drawable.fp_video_selected, R.drawable.fp_pic_selected,
            R.drawable.fp_audio_selected};

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // 声明SharedPreferences 对象
    private SharedPreferences sp;
    // 声明editor 对象
    private SharedPreferences.Editor editor;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置顶部状态栏透明度
        StatusBar statusBar = new StatusBar(MainActivity.this);
        //设置颜色为半透明
        statusBar.setColor(R.color.transparent);
        setContentView(R.layout.activity_main);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        DLNAManager.setIsDebugMode(BuildConfig.DEBUG);

        XXPermissions.with(MainActivity.this)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .permission(Permission.RECORD_AUDIO)
                .interceptor(new PermissionInterceptor())
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            return;
                        }
                        loadEverything();
                    }
                });


        ISNav.getInstance().init(new ImageLoader() {
            @Override
            public void displayImage(Context context, String path, ImageView imageView) {
                Glide.with(context).load(path).into(imageView);
            }
        });

        screen_free = (Button) findViewById(R.id.screen_free);
        screen_free.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TouchUtil.isFastClick()) {
                    startActivity(new Intent(MainActivity.this, FreeScreenActivity.class));
                }
            }
        });

        // 获得只能被本应用程序读、写的SharedPreferences对象
        sp = MainActivity.this.getSharedPreferences("testpreferences", MODE_PRIVATE);
        // 获得Editor对象
        editor = sp.edit();
        mainHandler.post(mHandlerDlnaStatusGot);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    private void loadEverything() {
        String[] mTitles = { getString(R.string.videos), getString(R.string.photos), getString(R.string.audios)};
        mFragments2.add(new VideoFragment());
        mFragments2.add(new PhotoFolderFragment());
        mFragments2.add(new AudioFragment());
        viewPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));

        CommonTabLayout mTabLayout = findViewById(R.id.common_tab);
        ArrayList<CustomTabEntity> mTabEntities = new ArrayList<>();
        for (int i = 0; i < mTitles.length; i++) {
            mTabEntities.add(new TabEntity(mTitles[i], mIconSelectIds[i], mIconUnselectIds[i]));
        }
        mTabLayout.setTabData(mTabEntities);

        mTabLayout.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelect(int position) {
                viewPager.setCurrentItem(position);
            }

            @Override
            public void onTabReselect(int position) {
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            //只实现自己想要的回调
            @Override
            public void onPageSelected(int position) {
                mTabLayout.setCurrentTab(position);
            }
        });
    }


    private class MyPagerAdapter extends FragmentPagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mFragments2.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String[] mTitles = { getString(R.string.videos), getString(R.string.photos), getString(R.string.audios)};

            return mTitles[position];
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments2.get(position);
        }
    }

    private void getDlnaStatusCore(){
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            int status = DlnaWrapper.getInstance().getmDLNAPlayer().currentState;
            if (status != DLNAPlayer.UNKNOWN && status != DLNAPlayer.STOP && status != DLNAPlayer.ERROR) { //已连接
            }
        }
    }
    private Runnable mHandlerDlnaStatusGot = new Runnable() {
        @Override
        public void run() {
            getDlnaStatusCore();
            mainHandler.postDelayed(this, 500);
        }
    };
}

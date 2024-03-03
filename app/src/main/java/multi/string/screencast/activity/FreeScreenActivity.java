package multi.string.screencast.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.multimedia.lib.screening.DLNAManager;
import com.multimedia.lib.screening.DLNAPlayer;
import com.multimedia.lib.screening.bean.DeviceInfo;
import com.multimedia.lib.screening.bean.MediaInfo;
import com.multimedia.lib.screening.listener.DLNAControlCallback;
import com.multimedia.lib.screening.listener.DLNADeviceConnectListener;
import com.multimedia.lib.screening.listener.DLNAStateCallback;
import com.sd.lib.switchbutton.FSwitchButton;
import com.sd.lib.switchbutton.SwitchButton;
import com.wang.avi.AVLoadingIndicatorView;

import org.fourthline.cling.model.action.ActionInvocation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import multi.string.screencast.BuildConfig;
import multi.string.screencast.R;
import multi.string.screencast.adapters.DevicesAdapter;
import multi.string.screencast.dlna.DEVICEChangeListener;
import multi.string.screencast.dlna.DlnaWrapper;
import multi.string.screencast.utils.FileUtil;
import multi.string.screencast.utils.PermissionInterceptor;
import multi.string.screencast.utils.TouchUtil;
import multi.string.screencast.utils.WifiStateUtils;
import multi.string.screencast.widgets.SearchRingView;
import multi.string.screencast.widgets.StatusBar;

public class FreeScreenActivity extends AppCompatActivity implements DLNADeviceConnectListener, DEVICEChangeListener, DevicesAdapter.OnItemClickListen {
    private static final String TAG = "multistring";
    //顶部一栏
    private RelativeLayout rlback = null;
    private RelativeLayout mSearchRl = null;
    private RelativeLayout mWifiDisconnectRl = null;
    private SearchRingView mSearchRingView = null;
    private RelativeLayout mUseDesc = null;
    //中间加载中icon
    private AVLoadingIndicatorView mAvLoadingIndicatorView;

    //dlna设备相关
    private DevicesAdapter mDevicesAdapter;
    private ListView mDeviceListView;
    private TextView mClockTime = null;
    private boolean  mShowTime = false;

    private int curItemType = MediaInfo.TYPE_MIRROR;
    private String mMediaPath = "freescreen";
    private boolean inited_dlna = false;


    //4k高清相关
    private FSwitchButton sb_custom = null;
    private int has4kablility = 0;
    private int able4K = 0;
    private String castTypeName = "";

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //顶部状态栏透明度设置 https://blog.csdn.net/qq_40642784/article/details/105705658
        //设置顶部状态栏透明度
        StatusBar statusBar = new StatusBar(FreeScreenActivity.this);
        //设置颜色为半透明
        statusBar.setColor(R.color.transparent);

        setContentView(R.layout.activity_free_screen);

        sb_custom = findViewById(R.id.sb_custom);
        sb_custom.setChecked(false, false, false);
        able4K = 0;
        sb_custom.setOnCheckedChangeCallback(new SwitchButton.OnCheckedChangeCallback() {
            @Override
            public void onCheckedChanged(boolean checked, SwitchButton switchButton) {
                Log.i(TAG, "sb_custom onCheckedChanged:" + checked);
                if (checked == true && has4kablility == 0) {
                    able4K = 0;
                    sb_custom.setChecked(false, false, false);
                } else if (checked == true){
                    able4K = 1;
                } else {
                    able4K = 0;
                }
            }
        });

        rlback = (RelativeLayout) findViewById(R.id.link_back_rl);
        rlback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    finish();
                }
            }
        });


        mSearchRingView = (SearchRingView) findViewById(R.id.search_ring);
        mSearchRingView.setCountdownTime(300); //三秒转一个圈
        mSearchRingView.startCountDownTime(new SearchRingView.OnCountdownFinishListener() {
            @Override
            public void countdownFinished() {
            }
        });

        mSearchRl = (RelativeLayout) findViewById(R.id.search_rl);
        mSearchRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inited_dlna == false) {
                    return;
                }
                if (TouchUtil.isFastClick()) {
                    DLNAManager.getInstance().startBrowser();
                    mSearchRingView.setCountdownTime(300); //三秒转一个圈
                    mSearchRingView.startCountDownTime(new SearchRingView.OnCountdownFinishListener() {
                        @Override
                        public void countdownFinished() {
                        }
                    });
                }
            }
        });

        mClockTime = (TextView)findViewById(R.id.clock_time);
        mClockTime.setVisibility(View.INVISIBLE);
        mShowTime = false;
        //加载中icon
        mAvLoadingIndicatorView = (AVLoadingIndicatorView) findViewById(R.id.loadingId);

        mUseDesc = (RelativeLayout) findViewById(R.id.use_describe);
        mWifiDisconnectRl = (RelativeLayout) findViewById(R.id.wifi_disconnect_rl);

        mDevicesAdapter = new DevicesAdapter(FreeScreenActivity.this, FreeScreenActivity.this);

        DLNAManager.setIsDebugMode(BuildConfig.DEBUG);
        XXPermissions.with(FreeScreenActivity.this)
                // 适配分区存储应该这样写
//                .permission(Permission.Group.STORAGE)
                // 不适配分区存储应该这样写
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

        mDeviceListView = findViewById(R.id.deviceListView);
        mDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });
        mDeviceListView.setAdapter(mDevicesAdapter);

        //设置顶部标题栏透明度 https://blog.csdn.net/henkun/article/details/107816167
        RelativeLayout m_link_title_rl = findViewById(R.id.link_title_rl);
        m_link_title_rl.getBackground().mutate().setAlpha(0);

        //实时获取wifi的状态
        mHandlerWifi.sendEmptyMessageDelayed(0, 10);
        has4kablility = 0;
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        freshDeviceStatus();
    }


    @Override
    public void onConnect(DeviceInfo deviceInfo, int errorCode) {
        if (errorCode == CONNECT_INFO_CONNECT_SUCCESS) {
            startPlay();
        }
    }

    @Override
    public void onDisconnect(DeviceInfo deviceInfo, int type, int errorCode) {
    }

    @Override
    protected void onDestroy() {
        if (mHandlerWifi != null) {
            mHandlerWifi.removeMessages(0);
        }
        DlnaWrapper.getInstance().removeLis();

        super.onDestroy();
    }

    private void loadEverything() {

        boolean ret = DLNAManager.getInstance().init(getApplicationContext(), new DLNAStateCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "DLNAManager ,onConnected");
                inited_dlna = true;
                DlnaWrapper.getInstance().initDlna(getApplicationContext(), FreeScreenActivity.this, FreeScreenActivity.this);
                DLNAManager.getInstance().startBrowser();
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "DLNAManager ,onDisconnected");
            }
        });

        if (ret == false && inited_dlna == false) {
            inited_dlna = true;
            DlnaWrapper.getInstance().initDlna(getApplicationContext(), FreeScreenActivity.this, FreeScreenActivity.this);
            DLNAManager.getInstance().startBrowser();
        }
    }

    private void startPlay() {
        String sourceUrl = mMediaPath;
        final MediaInfo mediaInfo = new MediaInfo();
        if (!TextUtils.isEmpty(sourceUrl)) {
            mediaInfo.setMediaId(Base64.encodeToString(sourceUrl.getBytes(), Base64.NO_WRAP));
            mediaInfo.setUri(sourceUrl);
        }
        mediaInfo.setMediaType(curItemType);

        if (curItemType == MediaInfo.TYPE_VIDEO) {
            mediaInfo.setFileName(FileUtil.getVideoFileName(sourceUrl));
        } else if (curItemType == MediaInfo.TYPE_AUDIO){
            mediaInfo.setMediaName(FileUtil.getAudioFileName(sourceUrl));
        }
        DlnaWrapper.getInstance().getmDLNAPlayer().setDataSource(mediaInfo);
        showLoading();
        DlnaWrapper.getInstance().getmDLNAPlayer().is_mirror = true;
        DlnaWrapper.getInstance().getmDLNAPlayer().is_4k = able4K;//(able4K == 1?true:false);
        castTypeName = (able4K == 0?getString(R.string.screen_cast_normal):getString(R.string.screen_cast_4k));
        DlnaWrapper.getInstance().getmDLNAPlayer().start(new DLNAControlCallback() {
            @Override
            public void onSuccess(@Nullable ActionInvocation invocation) {
                if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
                    DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.CONNECTED);
                }
                closeLoading();
                if (mSearchRingView != null){
                    mSearchRingView.cancel();
                }
                mShowTime = true;
            }

            @Override
            public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {

            }

            @Override
            public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
                    DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.UNKNOWN);
                }
                closeLoading();
            }
        });
    }

    private void stopPlay() {
        DlnaWrapper.getInstance().getmDLNAPlayer().stop(new DLNAControlCallback() {
            @Override
            public void onSuccess(@Nullable ActionInvocation invocation) {
                if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
                    DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.STOP); //mDeviceInfo.setState(DLNAPlayer.STOP);
                }
            }

            @Override
            public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {
            }

            @Override
            public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
                    DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.STOP);//mDeviceInfo.setState(DLNAPlayer.STOP);
                }

            }
        });
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.UNKNOWN); //mDeviceInfo.setState(DLNAPlayer.UNKNOWN);
        }

        if (mClockTime != null) {
            mClockTime.post(new Runnable() {
                @Override
                public void run() {
                    mClockTime.setVisibility(View.INVISIBLE);
                }
            });
        }
        mShowTime = false;
    }

    //Wifi网络监测线程  然后在onCreate方法里面开启
    private final Handler mHandlerWifi = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                showTimeClock();
                NetworkInfo networkInfo = WifiStateUtils.WifiInfo(FreeScreenActivity.this);
                if (networkInfo.isConnected() == false) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (mWifiDisconnectRl != null)
                                mWifiDisconnectRl.setVisibility(View.VISIBLE);
                        }
                    });

                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (mSearchRl != null) {
                                mSearchRl.setVisibility(View.INVISIBLE);
                                mSearchRl.setClickable(false);
                            }

                            if (mDeviceListView != null) {
                                mDeviceListView.setVisibility(View.INVISIBLE);
                                mDeviceListView.setClickable(false);
                            }

                            if (mUseDesc != null) {
                                mUseDesc.setVisibility(View.VISIBLE);
                            }
                        }
                    });

                } else { //有网络
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (mWifiDisconnectRl != null) {
                                mWifiDisconnectRl.setVisibility(View.INVISIBLE);
                            }
                        }
                    });

                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (mSearchRl != null) {
                                mSearchRl.setVisibility(View.VISIBLE);
                                mSearchRl.setClickable(true);
                            }

                            if (mDeviceListView != null) {
                                mDeviceListView.setVisibility(View.VISIBLE);
                                mDeviceListView.setClickable(true);
                            }

                            if (mUseDesc != null) {
                                mUseDesc.setVisibility(View.INVISIBLE);
                            }
                        }
                    });

                }

                sendEmptyMessageDelayed(0, 1000);
            }
        }
    };

    //Wifi网络监测线程  然后在onCreate方法里面开启
    private final Handler mHandlerRefreshDeviceStatus = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            runOnUiThread(new Runnable() {
                public void run() {
                    freshDeviceStatus();
                }
            });
        }
    };


    @Override
    public void onDeviceChanged(List<DeviceInfo> deviceInfoList) {
        Log.i(TAG, "onDeviceChanged "+deviceInfoList.size());
        if (mDevicesAdapter != null) {
            mDevicesAdapter.clear();
            mDevicesAdapter.addAll(deviceInfoList);
            mDevicesAdapter.notifyDataSetChanged();
        }

        if (mHandlerRefreshDeviceStatus != null) {
            mHandlerRefreshDeviceStatus.removeMessages(0);
            mHandlerRefreshDeviceStatus.sendEmptyMessageDelayed(0, 300);
        }
    }

    @Override
    public void onClickItem(int pos) {
        if (TouchUtil.isFastClick()) {
            DeviceInfo deviceInfo = mDevicesAdapter.getItem(pos);
            if (null == deviceInfo) {
                return;
            }

            if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
                int status = DlnaWrapper.getInstance().getmDLNAPlayer().currentState;
                if (status == DLNAPlayer.UNKNOWN || status == DLNAPlayer.STOP || status == DLNAPlayer.ERROR) {
                    DlnaWrapper.getInstance().getmDLNAPlayer().connect(deviceInfo);
                } else {
                    stopPlay();
                }
            } else if (deviceInfo.getState() == DLNAPlayer.UNKNOWN || deviceInfo.getState() == DLNAPlayer.STOP) { //当前为未连接状态
                DlnaWrapper.getInstance().getmDLNAPlayer().connect(deviceInfo);
            } else { //连接状态,直接断开
                stopPlay();
            }
        }
    }

    private void freshDeviceStatus(){
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null && DlnaWrapper.getInstance().getmDLNAPlayer().mDeviceInfo != null &&
                DlnaWrapper.getInstance().getmDLNAPlayer().mDeviceInfo.getDevice() != null){

            int status = DlnaWrapper.getInstance().getmDLNAPlayer().currentState;
            if (mDevicesAdapter != null && mDevicesAdapter.getCount() > 0){
                int count = mDevicesAdapter.getCount();
                int id_connect = -1;
                for (int i=0; i<count; i++) {
                    DeviceInfo info =  (DeviceInfo)mDevicesAdapter.getItem(i);
                    if (info != null && info.getDevice() != null) {//&& info.getDevice() != null
                        if(info.getDevice().getIdentity().equals(DlnaWrapper.getInstance().getmDLNAPlayer().mDeviceInfo.getDevice().getIdentity())){
                            if (status != DLNAPlayer.UNKNOWN && status != DLNAPlayer.STOP){
                                info.setRealState(1);
                                id_connect = i;
                            } else {
                                info.setRealState(0);
                            }
                        } else {
                            info.setRealState(0);
                        }
                    } else {
                        info.setRealState(0);
                    }
                }
                //未连接的全部设置成灰色，并不可点击
                if (id_connect != -1) {
                    for (int i=0; i<count; i++) {
                        if (i == id_connect) {
                            continue;
                        }
                        DeviceInfo info = (DeviceInfo) mDevicesAdapter.getItem(i);
                        info.setRealState(2);
                    }
                }

                mDevicesAdapter.notifyDataSetChanged();
            }
        }

        if (mHandlerRefreshDeviceStatus != null) {
            mHandlerRefreshDeviceStatus.removeMessages(0);
            mHandlerRefreshDeviceStatus.sendEmptyMessageDelayed(0, 400);
        }
    }

    private void showTimeClock(){
        if (mShowTime == true) {
            if (mClockTime != null) {
                mClockTime.post(new Runnable() {
                    @Override
                    public void run() {
                        mClockTime.setVisibility(View.VISIBLE);
                        long time = System.currentTimeMillis();//long now = android.os.SystemClock.uptimeMillis();
                        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                        Date d1 = new Date(time);
                        String t1 = format.format(d1);
                        mClockTime.setText(castTypeName+"  "+t1);
                    }
                });
            }
        } else {
            if (mClockTime != null) {
                mClockTime.post(new Runnable() {
                    @Override
                    public void run() {
                        mClockTime.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }
    }

    //显示加载中
    private void showLoading(){
        if (mAvLoadingIndicatorView != null){
            mAvLoadingIndicatorView.setVisibility(View.VISIBLE);
        }

        runOnUiThread(new Runnable() {
            public void run() {
                mDeviceListView.setClickable(false);
            }
        });

        mAvLoadingIndicatorView.postDelayed(new Runnable() {
            public void run() {
                mAvLoadingIndicatorView.setVisibility(View.INVISIBLE);
                mDeviceListView.setClickable(true);
            }
        }, 3000);
    }

    //关闭加载中
    private void closeLoading(){
        if (mAvLoadingIndicatorView != null){
            mAvLoadingIndicatorView.setVisibility(View.INVISIBLE);
        }

        runOnUiThread(new Runnable() {
            public void run() {
                mDeviceListView.setClickable(true);
            }
        });
    }

}

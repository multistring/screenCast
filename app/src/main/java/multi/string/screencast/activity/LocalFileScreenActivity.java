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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

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
import com.wang.avi.AVLoadingIndicatorView;

import org.fourthline.cling.model.action.ActionInvocation;

import java.util.List;

import multi.string.screencast.BuildConfig;
import multi.string.screencast.R;
import multi.string.screencast.adapters.DevicesAdapter;
import multi.string.screencast.dlna.DEVICEChangeListener;
import multi.string.screencast.dlna.DlnaWrapper;
import multi.string.screencast.models.AlbumItem;
import multi.string.screencast.models.Media;
import multi.string.screencast.utils.FileUtil;
import multi.string.screencast.utils.PermissionInterceptor;
import multi.string.screencast.utils.StatusUtil;
import multi.string.screencast.utils.TouchUtil;
import multi.string.screencast.utils.WifiStateUtils;
import multi.string.screencast.widgets.SearchRingView;
import multi.string.screencast.widgets.StatusBar;

public class LocalFileScreenActivity extends AppCompatActivity implements DLNADeviceConnectListener, DEVICEChangeListener, DevicesAdapter.OnItemClickListen {
    private static final String TAG ="multistring";
    //顶部一栏
    private RelativeLayout rlback = null;
    private RelativeLayout mSearchRl = null;
    private RelativeLayout mWifiDisconnectRl = null;
    private SearchRingView mSearchRingView = null;
    private RelativeLayout       mUseDesc = null;
    private ImageView mHelpView = null;
    private ImageView      link_status = null; //投屏状态
    //中间加载中icon
    private AVLoadingIndicatorView mAvLoadingIndicatorView;

    //dlna设备相关
    private DevicesAdapter mDevicesAdapter;
    private ListView mDeviceListView;

    //本地文件相关
    List<Media> mMediaList = null;
    private int curItemType = MediaInfo.TYPE_UNKNOWN;
    private AlbumItem albumItem = null;
    private String mMediaPath = null;

    private boolean inited_dlna = false;

    // 声明SharedPreferences 对象
    private SharedPreferences sp;
    // 声明editor 对象
    private SharedPreferences.Editor editor;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //顶部状态栏透明度设置 https://blog.csdn.net/qq_40642784/article/details/105705658
        //设置顶部状态栏透明度
        StatusBar statusBar = new StatusBar(LocalFileScreenActivity.this);
        //设置颜色为半透明
        statusBar.setColor(R.color.translucent);

        setContentView(R.layout.activity_localfile_screen);
        rlback = (RelativeLayout) findViewById(R.id.link_back_rl);
        rlback.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    finish();
                }
            }
        });


        mSearchRingView = (SearchRingView)findViewById(R.id.search_ring);
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
                    mSearchRingView.setCountdownTime(300); //
                    mSearchRingView.startCountDownTime(new SearchRingView.OnCountdownFinishListener() {
                        @Override
                        public void countdownFinished() {
                        }
                    });
                }
            }
        });
        //加载中icon
        mAvLoadingIndicatorView = (AVLoadingIndicatorView)findViewById(R.id.loadingId);

        mUseDesc = (RelativeLayout) findViewById(R.id.use_describe);
        mWifiDisconnectRl =  (RelativeLayout) findViewById(R.id.wifi_disconnect_rl);
        link_status = (ImageView) findViewById(R.id.link_status_local);
        link_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
                        if (DlnaWrapper.getInstance().getmDLNAPlayer().is_mirror == true) {//镜像投屏
                        } else { //文件投屏
                            startActivity(new Intent(LocalFileScreenActivity.this, PlayControlActivity.class));
                        }
                    }
                }
            }
        });


        mDevicesAdapter = new DevicesAdapter(LocalFileScreenActivity.this, LocalFileScreenActivity.this);
        DLNAManager.setIsDebugMode(BuildConfig.DEBUG);
        XXPermissions.with(LocalFileScreenActivity.this)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
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

        RelativeLayout m_link_title_rl = findViewById(R.id.link_title_rl);
        m_link_title_rl.getBackground().mutate().setAlpha(0);

        //实时获取wifi的状态
        mHandlerWifi.sendEmptyMessageDelayed(0, 10);
        initData(savedInstanceState);

        // 获得只能被本应用程序读、写的SharedPreferences对象
        sp = LocalFileScreenActivity.this.getSharedPreferences("testpreferences", MODE_PRIVATE);
        // 获得Editor对象
        editor = sp.edit();
    }

    private void initData(Bundle savedInstanceState){
        if (savedInstanceState == null){
            Intent intent = getIntent();
            if (intent != null) {
                mMediaList = intent.getParcelableArrayListExtra(Media.EXTRA_RESULT);
                intent.removeExtra(Media.EXTRA_RESULT);
                if (mMediaList != null && mMediaList.size() > 0) {
                    Media media = mMediaList.get(0);
                    if (media.type == Media.VIDEO_TYPE) {
                        curItemType = MediaInfo.TYPE_VIDEO;
                    } else if (media.type == Media.AUDIO_TYPE) {
                        curItemType = MediaInfo.TYPE_AUDIO;
                    } else if (media.type == Media.PIC_TYPE) {
                        curItemType = MediaInfo.TYPE_IMAGE;
                    }
                    mMediaPath = mMediaList.get(0).path;
                }

                albumItem = intent.getParcelableExtra("AlbumItem");
                if (albumItem != null){
                    curItemType = MediaInfo.TYPE_IMAGE;
                    mMediaPath = albumItem.photos.get(0).path;
                }
                intent.removeExtra("AlbumItem");
            }
        }
    }


    private void loadEverything() {
        boolean ret = DLNAManager.getInstance().init(getApplicationContext(), new DLNAStateCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "DLNAManager ,onConnected");
                inited_dlna = true;
                DlnaWrapper.getInstance().initDlna(getApplicationContext(), LocalFileScreenActivity.this, LocalFileScreenActivity.this);
                DLNAManager.getInstance().startBrowser();
            }
            @Override
            public void onDisconnected() {
                Log.d(TAG, "DLNAManager ,onDisconnected");
            }
        });

        if (ret == false && inited_dlna == false) {
            inited_dlna = true;
            DlnaWrapper.getInstance().initDlna(getApplicationContext(), LocalFileScreenActivity.this, LocalFileScreenActivity.this);
            DLNAManager.getInstance().startBrowser();
        }
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
            if (DlnaWrapper.getInstance().getmDLNAPlayer() != null){
                DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.CONNECTED);
            }
            startPlay();
        }
    }

    @Override
    public void onDisconnect(DeviceInfo deviceInfo, int type, int errorCode) {

    }

    @Override
    protected void onDestroy() {
        if (mHandlerWifi != null){
            mHandlerWifi.removeMessages(0);
        }
        DlnaWrapper.getInstance().removeLis();
        super.onDestroy();
    }

    private void startPlay() {
        if (mMediaPath == null){
            return;
        }
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
        DlnaWrapper.getInstance().getmDLNAPlayer().is_mirror = false;
        DlnaWrapper.getInstance().getmDLNAPlayer().start(new DLNAControlCallback() {
            @Override
            public void onSuccess(@Nullable ActionInvocation invocation) {
                if (DlnaWrapper.getInstance().getmDLNAPlayer() != null){
                    DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.PLAY);
                }
                closeLoading();
                if (curItemType != MediaInfo.TYPE_IMAGE) {
                    Intent intent = new Intent(LocalFileScreenActivity.this, PlayControlActivity.class);
                    LocalFileScreenActivity.this.startActivityForResult(intent, StatusUtil.REQUEST_CODE_VIDEOSCREEN);
                } else {
                    Intent intent = new Intent(LocalFileScreenActivity.this, GalleryActivity.class);
                    intent.putExtra("AlbumItem", albumItem);
                    startActivity(intent);
                }
            }

            @Override
            public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {

            }

            @Override
            public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                if (DlnaWrapper.getInstance().getmDLNAPlayer() != null){
                    DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.UNKNOWN);
                }
                closeLoading();
            }
        });
    }

    private void stopPlay(){
        DlnaWrapper.getInstance().getmDLNAPlayer().stop(new DLNAControlCallback() {
            @Override
            public void onSuccess(@Nullable ActionInvocation invocation) {
                if (DlnaWrapper.getInstance().getmDLNAPlayer() != null){
                    DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.STOP);
                }
                PlayControlActivity.getStatus = false;
            }

            @Override
            public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {

            }

            @Override
            public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                if (DlnaWrapper.getInstance().getmDLNAPlayer() != null){
                    DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.STOP);
                }

            }
        });
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null){
            DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.UNKNOWN);
        }
    }

    //Wifi网络监测线程  然后在onCreate方法里面开启
    private final Handler mHandlerWifi = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                NetworkInfo networkInfo = WifiStateUtils.WifiInfo(LocalFileScreenActivity.this);
                if (networkInfo.isConnected() == false){
                    runOnUiThread(new Runnable()  {
                        public void run()  {
                            if(mWifiDisconnectRl != null)
                                mWifiDisconnectRl.setVisibility(View.VISIBLE);
                        }
                    });

                    runOnUiThread(new Runnable()  {
                        public void run()  {
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
                    runOnUiThread(new Runnable()  {
                        public void run()  {
                            if (mWifiDisconnectRl != null) {
                                mWifiDisconnectRl.setVisibility(View.INVISIBLE);
                            }
                        }
                    });

                    runOnUiThread(new Runnable()  {
                        public void run()  {
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
                sendEmptyMessageDelayed(0, 400);
            }
        }
    };

    @Override
    public void onDeviceChanged(List<DeviceInfo> deviceInfoList) {
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
            int status = DlnaWrapper.getInstance().getmDLNAPlayer().currentState; //mDeviceInfo.getState();
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

                    if (DlnaWrapper.getInstance().getmDLNAPlayer().is_mirror == false) {
                        //右上角的连接状态按钮 连接状态
                        link_status.post(new Runnable() {
                            @Override
                            public void run() {
                                link_status.setImageDrawable(getApplicationContext().getResources().getDrawable(R.drawable.cast_online));
                                link_status.setClickable(true);
                            }
                        });
                    }
                } else {
                    link_status.post(new Runnable() {
                        @Override
                        public void run() {
                            link_status.setImageDrawable(getApplicationContext().getResources().getDrawable(R.drawable.cast_offline));
                            link_status.setClickable(false);
                        }
                    });
                }
                mDevicesAdapter.notifyDataSetChanged();
            }
        }

        if (mHandlerRefreshDeviceStatus != null) {
            mHandlerRefreshDeviceStatus.removeMessages(0);
            mHandlerRefreshDeviceStatus.sendEmptyMessageDelayed(0, 1000);
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
}


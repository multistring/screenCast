package multi.string.screencast.dlna;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.multimedia.lib.screening.DLNAManager;
import com.multimedia.lib.screening.DLNAPlayer;
import com.multimedia.lib.screening.bean.DeviceInfo;
import com.multimedia.lib.screening.listener.DLNAControlCallback;
import com.multimedia.lib.screening.listener.DLNADeviceConnectListener;
import com.multimedia.lib.screening.listener.DLNARegistryListener;

import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.support.model.PositionInfo;

import java.util.List;

public class DlnaWrapper {
    private static DlnaWrapper manager = null;

    private DLNAPlayer mDLNAPlayer = null;
    private DLNARegistryListener mDLNARegistryListener;
    private List<DeviceInfo> mDeviceInfoList = null;
    private DEVICEChangeListener mDeviceChangeLis = null;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private long mduration = 0;
    private long mcurrenttime =-1;
    public static DlnaWrapper getInstance(){
        if (manager == null){
            manager = new DlnaWrapper();
        }

        return manager;
    }

    public void initDlna(Context context, DLNADeviceConnectListener listener, DEVICEChangeListener listener1){
        if (mDLNAPlayer == null) {
            mDLNAPlayer = new DLNAPlayer(context);
        }
        mDLNAPlayer.setConnectListener(listener);
        mDeviceChangeLis = listener1;
        mDLNARegistryListener = new DLNARegistryListener() {
            @Override
            public void onDeviceChanged(List<DeviceInfo> deviceInfoList) {
                mDeviceInfoList = deviceInfoList;
                if (mDeviceChangeLis != null){
                    mDeviceChangeLis.onDeviceChanged(deviceInfoList);
                }
            }
        };

        DLNAManager.getInstance().registerListener(mDLNARegistryListener);
    }

    public void getposition(){
        DlnaWrapper.getInstance().getmDLNAPlayer().getPositionInfo(new DLNAControlCallback() {
            @Override
            public void onSuccess(@Nullable ActionInvocation invocation) {
            }
            @Override
            public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {
                mduration = ((PositionInfo)extra[0]).getTrackDurationSeconds();//视频总时长 单位s
                mcurrenttime = ((PositionInfo)extra[0]).getTrackElapsedSeconds();//视频当前进度 单位s
            }

            @Override
            public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
            }
        });
        return;
    }

    public static void updateEnv(){

    }

    public  void removeLis(){
        mDeviceChangeLis = null;
        if (mDLNAPlayer != null) {
            mDLNAPlayer.setConnectListener(null);
        }
    }

    public DLNAPlayer getmDLNAPlayer(){
        return mDLNAPlayer;
    }

    public Handler getMainHandler(){
        return mainHandler;
    }
}


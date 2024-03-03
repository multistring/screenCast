package com.multimedia.lib.screening;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.multimedia.lib.muxer.IMuxer;
import com.multimedia.lib.muxer.RTMPStreamMuxer;
import com.multimedia.lib.publisher.StreamPublisher;
//import com.multimedia.nginx.NginxHelper;
import com.multimedia.lib.screening.bean.DeviceInfo;
import com.multimedia.lib.screening.bean.MediaInfo;
import com.multimedia.lib.screening.listener.DLNAControlCallback;
import com.multimedia.lib.screening.listener.DLNADeviceConnectListener;
import com.multimedia.lib.screening.listener.OnRequestMediaProjectionResultCallback;
import com.multimedia.lib.screenrecorder.IRecorderCallback;
import com.multimedia.lib.screenrecorder.IScreenRecorderService;
import com.multimedia.lib.screenrecorder.Notifications;
import com.multimedia.lib.screenrecorder.ScreenRecorderServiceImpl;
import com.multimedia.lib.screenrecorder.VideoEncodeConfig;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.avtransport.callback.GetTransportInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Seek;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.VideoItem;
import org.fourthline.cling.support.renderingcontrol.callback.GetVolume;
import org.fourthline.cling.support.renderingcontrol.callback.SetMute;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;
import org.seamless.util.MimeType;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Description：真正投屏的管理者
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-09
 */
public class DLNAPlayer {

    private static final String DIDL_LITE_FOOTER = "</DIDL-Lite>";
    private static final String DIDL_LITE_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>"
            + "<DIDL-Lite "
            + "xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" "
            + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
            + "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" "
            + "xmlns:dlna=\"urn:schemas-dlna-org:metadata-1-0/\">";

    /**
     * 未知状态
     */
    public static final int UNKNOWN = -1;

    /**
     * 已连接状态
     */
    public static final int CONNECTED = 0;

    /**
     * 播放状态
     */
    public static final int PLAY = 1;
    /**
     * 暂停状态
     */
    public static final int PAUSE = 2;
    /**
     * 停止状态
     */
    public static final int STOP = 3;
    /**
     * 转菊花状态
     */
    public static final int BUFFER = 4;
    /**
     * 投放失败
     */
    public static final int ERROR = 5;

    /**
     * 已断开状态
     */
    public static final int DISCONNECTED = 6;

    public int currentState = UNKNOWN;
    public DeviceInfo mDeviceInfo;
    private Device mDevice;
    private MediaInfo mMediaInfo;
    private Context mContext;//鉴权预留
    private ServiceConnection mServiceConnection;
    private AndroidUpnpService mUpnpService;
    private DLNADeviceConnectListener connectListener;
    /**
     * 连接、控制服务
     */
    private ServiceType AV_TRANSPORT_SERVICE;
    private ServiceType RENDERING_CONTROL_SERVICE;

    public boolean is_mirror = false;
    public int is_4k = 0;
    public String  curFilePath = ""; //如果是非镜像，当前播放音视频文件的路径

    public DLNAPlayer(@NonNull Context context) {
        is_mirror = false;
        mContext = context;
        AV_TRANSPORT_SERVICE = new UDAServiceType("AVTransport");
        RENDERING_CONTROL_SERVICE = new UDAServiceType("RenderingControl");
        initConnection();
    }

    public void setConnectListener(DLNADeviceConnectListener connectListener) {
        this.connectListener = connectListener;
    }

    private void initConnection() {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mUpnpService = (AndroidUpnpService) service;
                currentState = CONNECTED;
                if (null != mDeviceInfo) {
                    mDeviceInfo.setState(CONNECTED);
                    mDeviceInfo.setConnected(true);
                }
                if (null != connectListener) {
                    connectListener.onConnect(mDeviceInfo, DLNADeviceConnectListener.CONNECT_INFO_CONNECT_SUCCESS);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                currentState = DISCONNECTED;
                if (null != mDeviceInfo) {
                    mDeviceInfo.setState(DISCONNECTED);
                    mDeviceInfo.setConnected(false);
                }
                if (null != connectListener) {
                    connectListener.onDisconnect(mDeviceInfo, DLNADeviceConnectListener.TYPE_DLNA,
                            DLNADeviceConnectListener.CONNECT_INFO_DISCONNECT_SUCCESS);
                }
                mUpnpService = null;
                connectListener = null;
                mDeviceInfo = null;
                mDevice = null;
                mMediaInfo = null;
                AV_TRANSPORT_SERVICE = null;
                RENDERING_CONTROL_SERVICE = null;
                mServiceConnection = null;
                mContext = null;
            }
        };
    }

    public void connect(@NonNull DeviceInfo deviceInfo) {
        checkConfig();
        mDeviceInfo = deviceInfo;
        mDevice = mDeviceInfo.getDevice();
        if (null != mUpnpService) {
            currentState = CONNECTED;
            if (null != connectListener) {
                connectListener.onConnect(mDeviceInfo, DLNADeviceConnectListener.CONNECT_INFO_CONNECT_SUCCESS);
            }
            return;
        }
        mContext.bindService(new Intent(mContext, DLNABrowserService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE);
    }


    public void disconnect() {
        checkConfig();
        try {
            if (null != mUpnpService && null != mServiceConnection) {
                mContext.unbindService(mServiceConnection);
            }
        } catch (Exception e) {
            DLNAManager.logE("DLNAPlayer disconnect UPnpService error.", e);
        }

    }

    private void checkPrepared() {
        if (null == mUpnpService) {
            throw new IllegalStateException("Invalid AndroidUPnpService");
        }
    }

    private void checkConfig() {
        if (null == mContext) {
            throw new IllegalStateException("Invalid context");
        }
    }

    private void execute(@NonNull ActionCallback actionCallback) {
        checkPrepared();
        mUpnpService.getControlPoint().execute(actionCallback);

    }

    private void execute(@NonNull SubscriptionCallback subscriptionCallback) {
        checkPrepared();
        mUpnpService.getControlPoint().execute(subscriptionCallback);
    }

    public void play(@NonNull DLNAControlCallback callback) {
        if (mDevice == null) {
            return;
        }
        if (is_mirror == true) {
            callback.onSuccess(null);
            return;
        }

        final Service avtService = mDevice.findService(AV_TRANSPORT_SERVICE);
        if (checkErrorBeforeExecute(PLAY, avtService, callback)) {
            return;
        }
        execute(new Play(avtService) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                currentState = PLAY;
                callback.onSuccess(invocation);
                mDeviceInfo.setState(PLAY);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                //currentState = ERROR;
                callback.onFailure(invocation, DLNAControlCallback.ERROR_CODE_DLNA_ERROR, defaultMsg);
                mDeviceInfo.setState(ERROR);
            }
        });
    }

    public void pause(@NonNull DLNAControlCallback callback) {
        if (mDevice == null) {
            return;
        }
        if (is_mirror == true) {
            return;
        }


        final Service avtService = mDevice.findService(AV_TRANSPORT_SERVICE);
        if (checkErrorBeforeExecute(PAUSE, avtService, callback)) {
            return;
        }

        execute(new Pause(avtService) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                currentState = PAUSE;
                callback.onSuccess(invocation);
                mDeviceInfo.setState(PAUSE);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                //currentState = ERROR;
                callback.onFailure(invocation, DLNAControlCallback.ERROR_CODE_DLNA_ERROR, defaultMsg);
                mDeviceInfo.setState(ERROR);
            }
        });
    }

    public void stop(@NonNull DLNAControlCallback callback) {
        if (mDevice == null){
            return;
        }

        final Service avtService = mDevice.findService(AV_TRANSPORT_SERVICE);

        execute(new Stop(avtService) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                currentState = STOP;
                callback.onSuccess(invocation);
                mDeviceInfo.setState(STOP);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                callback.onFailure(invocation, DLNAControlCallback.ERROR_CODE_DLNA_ERROR, defaultMsg);
                mDeviceInfo.setState(ERROR);
            }
        });

        if (is_mirror == true) {
            stopMirror();
        }
    }

    public void seekTo(String time, @NonNull DLNAControlCallback callback) {
        final Service avtService = mDevice.findService(AV_TRANSPORT_SERVICE);
        if (checkErrorBeforeExecute(avtService, callback)) {
            return;
        }

        if (is_mirror == true) {
            return;
        }
        execute(new Seek(avtService, time) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                callback.onSuccess(invocation);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                callback.onFailure(invocation, DLNAControlCallback.ERROR_CODE_DLNA_ERROR, defaultMsg);
                mDeviceInfo.setState(ERROR);
            }
        });
    }

    public void setVolume(long volume, @NonNull DLNAControlCallback callback) {
        if (mDevice == null) {
            return;
        }
        final Service avtService = mDevice.findService(RENDERING_CONTROL_SERVICE);
        if (checkErrorBeforeExecute(avtService, callback)) {
            return;
        }

        execute(new SetVolume(avtService, volume) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                callback.onSuccess(invocation);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                callback.onFailure(invocation, DLNAControlCallback.ERROR_CODE_DLNA_ERROR, defaultMsg);
                mDeviceInfo.setState(ERROR);
            }
        });
    }

    public void mute(boolean desiredMute, @NonNull DLNAControlCallback callback) {
        final Service avtService = mDevice.findService(RENDERING_CONTROL_SERVICE);
        if (checkErrorBeforeExecute(avtService, callback)) {
            return;
        }
        execute(new SetMute(avtService, desiredMute) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                callback.onSuccess(invocation);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                callback.onFailure(invocation, DLNAControlCallback.ERROR_CODE_DLNA_ERROR, defaultMsg);
                mDeviceInfo.setState(ERROR);
            }
        });
    }

    public void getTransportInfo(@NonNull DLNAControlCallback callback) {
        if (mDevice == null) {
            return;
        }
        final Service avtService = mDevice.findService(AV_TRANSPORT_SERVICE);
        if (checkErrorBeforeExecute(avtService, callback)) {
            return;
        }

        final GetTransportInfo getTransportInfo = new GetTransportInfo(avtService) {
            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                callback.onFailure(invocation, DLNAControlCallback.ERROR_CODE_DLNA_ERROR, defaultMsg);
            }

            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                callback.onSuccess(invocation);
            }

            @Override
            public void received(ActionInvocation invocation, TransportInfo transportInfo) {
                callback.onReceived(invocation, transportInfo);
            }
        };

        execute(getTransportInfo);
    }


    public void getPositionInfo(@NonNull DLNAControlCallback callback) {
        if (mDevice == null) {
            return;
        }
        final Service avtService = mDevice.findService(AV_TRANSPORT_SERVICE);
        if (checkErrorBeforeExecute(avtService, callback)) {
            return;
        }

        final GetPositionInfo getPositionInfo = new GetPositionInfo(avtService) {
            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                callback.onFailure(invocation, DLNAControlCallback.ERROR_CODE_DLNA_ERROR, defaultMsg);
                mDeviceInfo.setState(ERROR);
            }

            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                callback.onSuccess(invocation);
            }

            @Override
            public void received(ActionInvocation invocation, PositionInfo info) {
                callback.onReceived(invocation, info);
            }
        };

        execute(getPositionInfo);
    }


    public void getVolume(@NonNull DLNAControlCallback callback) {
        if (mDevice == null) {
            return;
        }
        final Service avtService = mDevice.findService(RENDERING_CONTROL_SERVICE);
        if (checkErrorBeforeExecute(avtService, callback)) {
            return;
        }

        final GetVolume getVolume = new GetVolume(avtService) {

            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                callback.onSuccess(invocation);
            }

            @Override
            public void received(ActionInvocation invocation, int currentVolume) {
                callback.onReceived(invocation, currentVolume);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                //currentState = ERROR;
                callback.onFailure(invocation, DLNAControlCallback.ERROR_CODE_DLNA_ERROR, defaultMsg);
                mDeviceInfo.setState(ERROR);
            }
        };
        execute(getVolume);
    }

    public void setDataSource(@NonNull MediaInfo mediaInfo) {
        mMediaInfo = mediaInfo;
        curFilePath = mediaInfo.getUri();
        //尝试变换本地播放地址
        mMediaInfo.setUri(DLNAManager.tryTransformLocalMediaAddressToLocalHttpServerAddress(mContext,
                mMediaInfo.getUri()));
    }

    public void start(final @NonNull DLNAControlCallback callback) {
        if (mMediaInfo.getMediaType() == MediaInfo.TYPE_MIRROR) {
            startMirror(callback);
            is_mirror = true;
            return;
        }
        mDeviceInfo.setMediaID(mMediaInfo.getMediaId());
        String metadata = pushMediaToRender(mMediaInfo);
        final Service avtService = mDevice.findService(AV_TRANSPORT_SERVICE);
        if (null == avtService) {
            callback.onFailure(null, DLNAControlCallback.ERROR_CODE_SERVICE_ERROR, "接收端不支持AV_TRANSPORT_SERVICE");
            return;
        }
        execute(new SetAVTransportURI(avtService, mMediaInfo.getUri(), metadata) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                play(callback);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                DLNAManager.logE("play error:" + defaultMsg);
                //currentState = ERROR;
                mDeviceInfo.setState(ERROR);
                callback.onFailure(invocation, DLNAControlCallback.ERROR_CODE_DLNA_ERROR, defaultMsg);
            }
        });
    }

    public void quickTime(){
       if (mScreenRecorderService != null){
           mScreenRecorderService.quickTime();
       }
    }

    private String pushMediaToRender(@NonNull MediaInfo mediaInfo) {
        return pushMediaToRender(mediaInfo.getUri(), mediaInfo.getMediaId(), mediaInfo.getMediaName(),
                mediaInfo.getMediaType());
    }

    private String pushMediaToRender(String url, String id, String name, int ItemType) {
        final long size = 0;
        final Res res = new Res(new MimeType(ProtocolInfo.WILDCARD, ProtocolInfo.WILDCARD), size, url);
        final String creator = "unknow";
        final String parentId = "0";
        final String metadata;

        switch (ItemType) {
            case MediaInfo.TYPE_IMAGE:
                ImageItem imageItem = new ImageItem(id, parentId, name, creator, res);
                metadata = createItemMetadata(imageItem);
                break;
            case MediaInfo.TYPE_VIDEO:
                VideoItem videoItem = new VideoItem(id, parentId, name, creator, res);
                metadata = createItemMetadata(videoItem);
                break;
            case MediaInfo.TYPE_AUDIO:
                AudioItem audioItem = new AudioItem(id, parentId, name, creator, res);
                metadata = createItemMetadata(audioItem);
                break;
            default:
                throw new IllegalArgumentException("UNKNOWN MEDIA TYPE");
        }

        DLNAManager.logE("metadata: " + metadata);
        return metadata;
    }

    /**
     * 创建投屏的参数
     *
     * @param item
     * @return
     */
    private String createItemMetadata(DIDLObject item) {
        StringBuilder metadata = new StringBuilder();
        metadata.append(DIDL_LITE_HEADER);

        metadata.append(String.format("<item id=\"%s\" parentID=\"%s\" restricted=\"%s\">", item.getId(), item.getParentID(), item.isRestricted() ? "1" : "0"));

        metadata.append(String.format("<dc:title>%s</dc:title>", item.getTitle()));
        String creator = item.getCreator();
        if (creator != null) {
            creator = creator.replaceAll("<", "_");
            creator = creator.replaceAll(">", "_");
        }
        metadata.append(String.format("<upnp:artist>%s</upnp:artist>", creator));
        metadata.append(String.format("<upnp:class>%s</upnp:class>", item.getClazz().getValue()));

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date now = new Date();
        String time = sdf.format(now);
        metadata.append(String.format("<dc:date>%s</dc:date>", time));

        Res res = item.getFirstResource();
        if (res != null) {
            // protocol info
            String protocolinfo = "";
            ProtocolInfo pi = res.getProtocolInfo();
            if (pi != null) {
                protocolinfo = String.format("protocolInfo=\"%s:%s:%s:%s\"", pi.getProtocol(), pi.getNetwork(), pi.getContentFormatMimeType(), pi
                        .getAdditionalInfo());
            }
            DLNAManager.logE("protocolinfo: " + protocolinfo);

            // resolution, extra info, not adding yet
            String resolution = "";
            if (res.getResolution() != null && res.getResolution().length() > 0) {
                resolution = String.format("resolution=\"%s\"", res.getResolution());
            }

            // duration
            String duration = "";
            if (res.getDuration() != null && res.getDuration().length() > 0) {
                duration = String.format("duration=\"%s\"", res.getDuration());
            }

            // res begin
            //            metadata.append(String.format("<res %s>", protocolinfo)); // no resolution & duration yet
            metadata.append(String.format("<res %s %s %s>", protocolinfo, resolution, duration));

            // url
            String url = res.getValue();
            metadata.append(url);

            // res end
            metadata.append("</res>");
        }
        metadata.append("</item>");

        metadata.append(DIDL_LITE_FOOTER);

        return metadata.toString();
    }

    private boolean checkErrorBeforeExecute(int expectState, Service avtService, @NonNull DLNAControlCallback callback) {
        if (currentState == expectState) {
            callback.onSuccess(null);
            return true;
        }

        return checkErrorBeforeExecute(avtService, callback);
    }

    private boolean checkErrorBeforeExecute(Service avtService, @NonNull DLNAControlCallback callback) {
        if (currentState == UNKNOWN) {
            callback.onFailure(null, DLNAControlCallback.ERROR_CODE_NOT_READY, "当前设备链接还未准备好");
            return true;
        }

        if (null == avtService) {
            callback.onFailure(null, DLNAControlCallback.ERROR_CODE_SERVICE_ERROR, "当前设备不支持的服务类型");
            return true;
        }

        return false;
    }

    public void setStatus(int status){
        currentState = status;
    }

    public void destroy() {
        checkConfig();
        stopMirror();
        try {
            if (null != mScreenRecorderService && null != mScreenRecorderServiceConnection) {
                mContext.unbindService(mScreenRecorderServiceConnection);
            }
        } catch (Exception e) {
            DLNAManager.logE("DLNAPlayer disconnect RecorderService error.", e);
        }
        disconnect();
    }

    //-------------------------------------------mirror-------------------------------------------------

    private IScreenRecorderService mScreenRecorderService;
    private DLNAControlCallback mMirrorControlCallback;
    private IMuxer mStreamMuxer = null;
    private Notifications mNotifications;
    private long mNotificationStartTime = 0;

    private ServiceConnection mScreenRecorderServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mScreenRecorderService = (IScreenRecorderService) service;
            prepareMediaProjection();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mScreenRecorderService = null;
            mScreenRecorderServiceConnection = null;
        }
    };

    private void prepareMediaProjection() {
        if (null != mScreenRecorderService) {
            mScreenRecorderService.registerRecorderCallback(mRecorderCallback);
            if (mScreenRecorderService.hasPrepared()) {
                mScreenRecorderService.startRecorder(is_4k);
                return;
            }
            RequestMediaProjectionActivity.resultCallback = mRequestMediaProjectionResultCallback;
            RequestMediaProjectionActivity.start(mContext);
        }
    }

    private IRecorderCallback mRecorderCallback = new IRecorderCallback() {
        @Override
        public void onPrepareRecord() {
            mStreamMuxer = new RTMPStreamMuxer();
            StreamPublisher.StreamPublisherParam videoStreamPublisherParam =
                    new StreamPublisher.StreamPublisherParam.Builder()
                            .setHeight(mScreenRecorderService.getVideoEncodeConfig().getHeight())
                            .setWidth(mScreenRecorderService.getVideoEncodeConfig().getWidth())
                            .createStreamPublisherParam();
            String[] pathArray = mScreenRecorderService.getSavingFilePath().split("\\.");
            videoStreamPublisherParam.outputFilePath = mScreenRecorderService.getSavingFilePath()
                    .replace(pathArray[pathArray.length - 1], "flv");
//            videoStreamPublisherParam.outputUrl = "rtmp://" + DLNAManager.getLocalIpStr(mContext) +
//                    NginxHelper.getRtmpLiveServerConfig() + "mirror";

            mStreamMuxer.open(videoStreamPublisherParam);
        }

        @Override
        public void onStartRecord() {
            mContext.registerReceiver(mStopActionReceiver, new IntentFilter(Notifications.ACTION_STOP));
            //ZLMediaKit
//            String sourceUrl = rtmp://192.168.8.108/live/0
//            http://192.168.8.108/live/0.live.ts
            String sourceUrl = "http://" + DLNAManager.getLocalIpStr(mContext) + ":8081/live/0.live.ts";

            final MediaInfo mediaInfo = new MediaInfo();
            mediaInfo.setMediaId(Base64.encodeToString(sourceUrl.getBytes(), Base64.NO_WRAP));
            //int oldtype = mMediaInfo.getMediaType();
            mediaInfo.setMediaType(MediaInfo.TYPE_VIDEO);
            mediaInfo.setUri(sourceUrl);

            setDataSource(mediaInfo);
            start(mMirrorControlCallback);

            if (null == mNotifications) {
                mNotifications = new Notifications(mContext);
            }
            mNotifications.recording(0);
        }

        @Override
        public void onRecording(long presentationTimeUs) {
            if (mNotificationStartTime <= 0) {
                mNotificationStartTime = presentationTimeUs;
            }
            long time = (presentationTimeUs - mNotificationStartTime) / 1000;
            mNotifications.recording(time);
        }

        @Override
        public void onStopRecord(Throwable error) {
            mNotificationStartTime = 0;
            if(mNotifications != null)
                mNotifications.clear();
        }

        @Override
        public void onDestroyRecord() {
            mNotifications = null;
        }

        @Override
        public void onConfigChanged(byte[] buffer, int length) {
            if (null != mStreamMuxer) {
                mStreamMuxer.writeHeadInfo(buffer, length);
            }
        }

        @Override
        public void onMuxAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
            if (null != mStreamMuxer) {
                mStreamMuxer.writeAudio(buffer, offset, length, bufferInfo);
            }
        }

        @Override
        public void onMuxVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
            if (null != mStreamMuxer) {
                mStreamMuxer.writeVideo(buffer, offset, length, bufferInfo);
            }
        }
    };

    private OnRequestMediaProjectionResultCallback mRequestMediaProjectionResultCallback = new OnRequestMediaProjectionResultCallback() {
        @Override
        public void onMediaProjectionResult(MediaProjection mediaProjection) {
            if (null != mScreenRecorderService) {
                mScreenRecorderService.prepareAndStartRecorder(mediaProjection, null, null, is_4k);
            } else {
                if (null != mMirrorControlCallback) {
                    mMirrorControlCallback.onFailure(null,
                            DLNAControlCallback.ERROR_CODE_BIND_SCREEN_RECORDER_SERVICE_ERROR, "");
                }
            }
        }
    };

    private BroadcastReceiver mStopActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Notifications.ACTION_STOP.equals(intent.getAction())) {
                stopMirror();
            }
        }
    };

    private void startMirror(final @NonNull DLNAControlCallback callback) {
        checkConfig();
        mMirrorControlCallback = callback;
        if (null == mScreenRecorderService) {
            mContext.bindService(new Intent(mContext, ScreenRecorderServiceImpl.class), mScreenRecorderServiceConnection,
                    Context.BIND_AUTO_CREATE);
        } else {
            prepareMediaProjection();
        }
    }

    private void stopMirror() {
        mMirrorControlCallback = null;
        try {
            mContext.unregisterReceiver(mStopActionReceiver);
        } catch (Exception e) {
            //ignored
        }
        if (null != mScreenRecorderService) {
            mScreenRecorderService.stopRecorder();
        }
        if (null != mStreamMuxer) {
            mStreamMuxer.close();
            mStreamMuxer = null;
        }

        is_mirror = false;
    }

    public String getCurFilePath(){
        return curFilePath;
    }

    public String getCurFileName(){
        if (mMediaInfo != null){
            return mMediaInfo.getFileName();
        } else {
            return "";
        }
    }
}

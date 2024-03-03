package multi.string.screencast.utils;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.multimedia.lib.screening.DLNAPlayer;
import com.multimedia.lib.screening.bean.MediaInfo;
import com.multimedia.lib.screening.listener.DLNAControlCallback;

import org.fourthline.cling.model.action.ActionInvocation;

import java.util.ArrayList;

import multi.string.screencast.activity.GalleryActivity;
import multi.string.screencast.activity.LocalFileScreenActivity;
import multi.string.screencast.activity.PlayControlActivity;
import multi.string.screencast.dlna.DlnaWrapper;
import multi.string.screencast.models.AlbumItem;
import multi.string.screencast.models.Media;

public class StatusUtil {
    public static int REQUEST_CODE_VIDEOSCREEN = 12;
    private static Activity mActivity = null;
    public static void GotoLocalFileCast(Activity activity, ArrayList<Media> paths, boolean jumpActivity){
        if (activity == null) {
            return;
        }

        if (paths == null || paths.size() <= 0){
            return;
        }

        mActivity = activity;
        Media media = paths.get(0);

        if (media.type == Media.VIDEO_TYPE || media.type == Media.AUDIO_TYPE || media.type == Media.PIC_TYPE) { //针对音视频文件的逻辑
            if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
                int status = DlnaWrapper.getInstance().getmDLNAPlayer().currentState;
                if (status != DLNAPlayer.UNKNOWN && status != DLNAPlayer.STOP && DlnaWrapper.getInstance().getmDLNAPlayer().is_mirror == false) { //处于连接状态，跳到文件播放控制页面
                    String curPath = DlnaWrapper.getInstance().getmDLNAPlayer().getCurFilePath();
                    int type = media.type == Media.VIDEO_TYPE? MediaInfo.TYPE_VIDEO:MediaInfo.TYPE_AUDIO;
                    castFileToTv(paths.get(0).path, type, jumpActivity, null);
                } else { //处于未连接状态，跳转到投屏配置页面
                    if (jumpActivity == true) {
                        Intent intent = new Intent(activity, LocalFileScreenActivity.class);
                        intent.putParcelableArrayListExtra(Media.EXTRA_RESULT, paths);
                        activity.startActivityForResult(intent, REQUEST_CODE_VIDEOSCREEN);
                    }
                }
            } else { //第一次进入投屏界面
                if (jumpActivity == true) {
                    Intent intent = new Intent(activity, LocalFileScreenActivity.class);
                    intent.putParcelableArrayListExtra(Media.EXTRA_RESULT, paths);
                    activity.startActivityForResult(intent, REQUEST_CODE_VIDEOSCREEN);
                }
            }
        }
    }

    //只针对图片的逻辑
    public static void GotoPhotoCast(Activity activity, AlbumItem albumItem, boolean jumpActivity){
        if (activity == null || albumItem == null) {
            return;
        }
        mActivity = activity;
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            int status = DlnaWrapper.getInstance().getmDLNAPlayer().currentState;
            if (status != DLNAPlayer.UNKNOWN && status != DLNAPlayer.STOP && DlnaWrapper.getInstance().getmDLNAPlayer().is_mirror == false) { //处于连接状态，跳到文件播放控制页面
                castFileToTv(albumItem.photos.get(0).path, MediaInfo.TYPE_IMAGE, jumpActivity, albumItem);
            } else { //处于未连接状态，跳转到投屏配置页面
                Intent intent = new Intent(activity, LocalFileScreenActivity.class);
                intent.putExtra("AlbumItem", albumItem);
                activity.startActivityForResult(intent, REQUEST_CODE_VIDEOSCREEN);
            }
        } else {
            Intent intent = new Intent(activity, LocalFileScreenActivity.class);
            intent.putExtra("AlbumItem", albumItem);
            activity.startActivityForResult(intent, REQUEST_CODE_VIDEOSCREEN);
        }
    }

    public static void castFileToTv(String path, int type, boolean jumpActivity, AlbumItem albumItem){
        String sourceUrl = path;//mMediaList.get(0).path;//mMediaPath;
        final MediaInfo mediaInfo = new MediaInfo();
        if (!TextUtils.isEmpty(sourceUrl)) {
            mediaInfo.setMediaId(Base64.encodeToString(sourceUrl.getBytes(), Base64.NO_WRAP));
            mediaInfo.setUri(sourceUrl);
        }
        mediaInfo.setMediaType(type);
        if (type == MediaInfo.TYPE_VIDEO) {
            mediaInfo.setFileName(FileUtil.getVideoFileName(sourceUrl));
        } else if (type == MediaInfo.TYPE_AUDIO){
            mediaInfo.setMediaName(FileUtil.getAudioFileName(sourceUrl));
        }
        DlnaWrapper.getInstance().getmDLNAPlayer().setDataSource(mediaInfo);
        DlnaWrapper.getInstance().getmDLNAPlayer().is_mirror = false;
        DlnaWrapper.getInstance().getmDLNAPlayer().start(new DLNAControlCallback() {
            @Override
            public void onSuccess(@Nullable ActionInvocation invocation) {
                if (DlnaWrapper.getInstance().getmDLNAPlayer() != null){
                    DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.PLAY);
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
            }
        });

        if (type == MediaInfo.TYPE_IMAGE) {
            if (jumpActivity == true) {
                Intent intent = new Intent(mActivity, GalleryActivity.class);
                intent.putExtra("AlbumItem", albumItem);
                mActivity.startActivityForResult(intent, REQUEST_CODE_VIDEOSCREEN);
            }
        } else {
            if (jumpActivity == true) {
                Intent intent = new Intent(mActivity, PlayControlActivity.class);
                mActivity.startActivityForResult(intent, REQUEST_CODE_VIDEOSCREEN);
            }
        }
    }
}


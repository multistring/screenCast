package multi.string.screencast.activity;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.multimedia.lib.screening.DLNAPlayer;
import com.multimedia.lib.screening.listener.DLNAControlCallback;

import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportState;

import java.util.ArrayList;
import java.util.List;

import multi.string.screencast.R;
import multi.string.screencast.adapters.VideoQueueAdapter;
import multi.string.screencast.dlna.DlnaWrapper;
import multi.string.screencast.models.Media;
import multi.string.screencast.models.MediaListItem;
import multi.string.screencast.utils.BottomDialog;
import multi.string.screencast.utils.DensityUtil;
import multi.string.screencast.utils.StatusUtil;
import multi.string.screencast.utils.TimeUtil;
import multi.string.screencast.utils.TouchUtil;
import multi.string.screencast.widgets.GradientTextView;

public class PlayControlActivity extends BaseActivity implements VideoQueueAdapter.OnItemClickListen{
    private  int CHANGE_VOLUME_UP = 1;
    private  int CHANGE_VOLUME_DOWN = 2;

    private LinearLayout backll = null; //回退键
    private LinearLayout menull = null; //视频列表键
    private LinearLayout stopTv = null; //断开电视
    private LinearLayout volumeAddll = null; //音量增加键
    private LinearLayout volumeDelll = null;//音量降低
    private SeekBar progressBar = null; //进度条
    private TextView curTimeTv   = null; //当前时间
    private TextView     totalTimeTv = null; //总时间
    private TextView     videoTitle = null; //播放标题
    private AppCompatImageView lastMedia = null; //上一个视频
    private AppCompatImageView  nextMedia = null; //下一个视频
    private AppCompatImageView  playMedia = null; //播放暂停键
    private GradientTextView loadingTextView = null; //底部加载view

    private long mduration = 0;
    private long mcurrenttime = 0;
    private int  mprogress = 0; //0-100
    private String m_transport_state = "";
    private int current_volume;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private int needSetStatusCount = 0;

    //队列存取相关
    // 声明SharedPreferences 对象
    private SharedPreferences sp;
    // 声明editor 对象
    private SharedPreferences.Editor editor;

    //底部播放列表
    private VideoQueueAdapter adapter = null;
    RecyclerView recyclerView = null;
    public static boolean getStatus = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_control);
        init();
    }

    private void init() {
        immersionBar(true, true);
        initView();
    }

    private void initView() {
        View statusView = findViewById(R.id.headView);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) statusView.getLayoutParams();
        params.topMargin = DensityUtil.getStatusHeightByDensity(mContext);
        statusView.setLayoutParams(params);

        backll = findViewById(R.id.backLinear);
        backll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TouchUtil.isFastClick()) {
                    finish();
                }
            }
        });

        menull = findViewById(R.id.menuLinear); //视频列表键
        stopTv = findViewById(R.id.stopTvLinear); //断开电视
        volumeAddll = findViewById(R.id.addLinear); //音量增加键
        volumeDelll = findViewById(R.id.delLinear);//音量降低
        progressBar = findViewById(R.id.progressBar); //进度条
        curTimeTv   = findViewById(R.id.currentTimeTv); //当前时间
        totalTimeTv = findViewById(R.id.totalTimeTv); //总时间
        lastMedia = findViewById(R.id.lastImg); //上一个视频
        nextMedia = findViewById(R.id.nextImg);  //下一个视频
        playMedia = findViewById(R.id.playImg); //播放暂停键
        videoTitle = findViewById(R.id.titleTv);
        loadingTextView = findViewById(R.id.video_loading);

        menull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    DisplayMetrics mDisplayMetrics = getResources().getDisplayMetrics();
                    int width = mDisplayMetrics.widthPixels;
                    int height = mDisplayMetrics.heightPixels;
                    float density = mDisplayMetrics.density;
                    int densityDpi = mDisplayMetrics.densityDpi;
                    List<MediaListItem> list = new ArrayList<>();
                    String mediaplaylist = sp.getString("mediaplaylist", null);
                    if (mediaplaylist != null && !mediaplaylist.equals("")) {
                        Gson gson = new Gson();
                        List<MediaListItem> list1 = gson.fromJson(mediaplaylist, new TypeToken<List<MediaListItem>>() {
                        }.getType());
                        if (list1 != null && list1.size() > 0) {
                            list = list1;
                        }
                    }

                    if (list.size() <= 0) {
                        Toast.makeText(PlayControlActivity.this, getString(R.string.list_empty), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    BottomDialog dialog = new BottomDialog(mContext, R.layout.video_list_dialog, height / 3, R.style.dialog_more_style, Gravity.BOTTOM, true);
                    dialog.show();
                    recyclerView = dialog.findViewById(R.id.recyclerView);
                    adapter = new VideoQueueAdapter(PlayControlActivity.this, PlayControlActivity.this);
                    recyclerView.setLayoutManager(new LinearLayoutManager(PlayControlActivity.this));
                    recyclerView.setAdapter(adapter);
                    adapter.setNewInstance(list);
                }
            }
        });

        lastMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    playNextOrBefore(0);
                }
            }
        });

        nextMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    playNextOrBefore(1);
                }
            }
        });

        stopTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    stopPlay(2);
                    finish();
                }
            }
        });
        stopTv.setClickable(false);

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 当拖动条的滑块位置发生改变时触发该方法,在这里直接使用参数progress，即当前滑块代表的进度值
                mprogress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mduration > 0 && mprogress>=0 && mprogress<=100){
                    int curtime = (int)(mduration*((float)mprogress/100));
                    curtime = (int)(curtime<0?0:(curtime>mduration?mduration:curtime));
                    seekto(curtime);
                }
            }
        });

        //播放 暂停控制
        playMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TouchUtil.isFastClick()) {
                    if (m_transport_state == TransportState.PLAYING.getValue()) {
                        dlnapause();
                    } else if (m_transport_state == TransportState.STOPPED.getValue()) {
                    } else if (m_transport_state == TransportState.PAUSED_PLAYBACK.getValue()) {
                        dlnaplay();
                    }
                    getStatus = true;
                }
            }
        });
        getStatus = true;

        //音量控制
        volumeAddll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TouchUtil.isFastClickFor200()) {
                    changevolume(CHANGE_VOLUME_UP);
                }
            }
        });
        volumeDelll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TouchUtil.isFastClickFor200()) {
                    changevolume(CHANGE_VOLUME_DOWN);
                }
            }
        });
        volumeAddll.setClickable(false);
        volumeDelll.setClickable(false);

        if (DlnaWrapper.getInstance().getMainHandler() != null) {
            DlnaWrapper.getInstance().getMainHandler().removeCallbacks(mHandlerGetPosAndStatusRunnable);
            DlnaWrapper.getInstance().getMainHandler().post(mHandlerGetPosAndStatusRunnable);
        }
        // 获得只能被本应用程序读、写的SharedPreferences对象
        sp = mContext.getSharedPreferences("testpreferences", MODE_PRIVATE);
        // 获得Editor对象
        editor = sp.edit();
        needSetStatusCount = 12;
    }


    private void dlnaplay(){
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            DlnaWrapper.getInstance().getmDLNAPlayer().play(new DLNAControlCallback() {
                @Override
                public void onSuccess(@Nullable ActionInvocation invocation) {
                }

                @Override
                public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {
                }

                @Override
                public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                }
            });
        }
        return;
    }

    private void dlnapause(){
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            DlnaWrapper.getInstance().getmDLNAPlayer().pause(new DLNAControlCallback() {
                @Override
                public void onSuccess(@Nullable ActionInvocation invocation) {
                }
                @Override
                public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {

                }
                @Override
                public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                }
            });
        }
    }

    private void stopPlay(final int stopNum){
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            DlnaWrapper.getInstance().getmDLNAPlayer().stop(new DLNAControlCallback() {
                @Override
                public void onSuccess(@Nullable ActionInvocation invocation) {
                    if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
                        DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.STOP);
                    }

                    getStatus = false;
                }

                @Override
                public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {
                }

                @Override
                public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                    if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
                        DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.STOP);
                    }
                    if (stopNum > 0) {
                        int newNum = stopNum-1;
                        stopPlay(newNum);
                    }
                }
            });
        }
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null){
            DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.UNKNOWN);
        }
        needSetStatusCount = 12;
    }

    private void gettransportinfo(){
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            DlnaWrapper.getInstance().getmDLNAPlayer().getTransportInfo(new DLNAControlCallback() {
                @Override
                public void onSuccess(@Nullable ActionInvocation invocation) {
                }

                @Override
                public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {
                    m_transport_state = ((TransportInfo) extra[0]).getCurrentTransportState().getValue();
                    if (m_transport_state == TransportState.PLAYING.getValue()) {
                        if (needSetStatusCount > 0){
                            needSetStatusCount--;
                            DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.PLAY);
                        }
                    } else if (m_transport_state == TransportState.STOPPED.getValue()) {
                        if (needSetStatusCount > 0){
                            needSetStatusCount--;
                            DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.STOP);
                        }
                    } else if (m_transport_state == TransportState.PAUSED_PLAYBACK.getValue()) {
                        if (needSetStatusCount > 0){
                            needSetStatusCount--;
                            DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.PAUSE);
                        }
                    }
                    freshUIIcon();
                }

                @Override
                public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                    needSetStatusCount = 2;
                }
            });
        }
        return;
    }

    private int changevolume(final int type){
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            DlnaWrapper.getInstance().getmDLNAPlayer().getVolume(new DLNAControlCallback() {
                @Override
                public void onSuccess(@Nullable ActionInvocation invocation) {
                }

                @Override
                public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {
                    current_volume = ((int) extra[0]);
                    current_volume = current_volume / 2;
                    if (type == CHANGE_VOLUME_UP) {
                        if (current_volume >= 0) {
                            setvolume((current_volume + 1) * 2);
                        }
                    } else {
                        if (current_volume > 0) {
                            setvolume((current_volume - 1) * 2);
                        }
                    }
                }

                @Override
                public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                    current_volume = -1;
                }
            });
        }
        return current_volume;
    }

    private void setvolume(int volume){
        DlnaWrapper.getInstance().getmDLNAPlayer().setVolume(volume, new DLNAControlCallback() {
            @Override
            public void onSuccess(@Nullable ActionInvocation invocation) {
            }

            @Override
            public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {
            }

            @Override
            public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
            }
        });
    }

    private void seekto(long time){
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            DlnaWrapper.getInstance().getmDLNAPlayer().seekTo(ModelUtil.toTimeString(time), new DLNAControlCallback() {
                @Override
                public void onSuccess(@Nullable ActionInvocation invocation) {
                    needSetStatusCount = 12;
                    gettransportinfo();
                }

                @Override
                public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {
                }

                @Override
                public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                }
            });
        }
        return;
    }

    private void getposition(){
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            DlnaWrapper.getInstance().getmDLNAPlayer().getPositionInfo(new DLNAControlCallback() {
                @Override
                public void onSuccess(@Nullable ActionInvocation invocation) {
                }

                @Override
                public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {
                    mduration = ((PositionInfo) extra[0]).getTrackDurationSeconds();//视频总时长 单位s
                    mcurrenttime = ((PositionInfo) extra[0]).getTrackElapsedSeconds();//视频当前进度 单位s
                }

                @Override
                public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                }
            });
        }
        return;
    }

    private void freshUIIcon(){
        //时间相关的icon
        if (mcurrenttime <=0){
            mcurrenttime = 0;
        }
        if (mduration < 0){
            mduration = 0;
            mcurrenttime = 0;
        }
        if (mduration>0) {
            int progress = (int)(((float)mcurrenttime/mduration)*100);
            progress = progress<0?0:progress>100?100:progress;
            progressBar.setProgress(progress);
        } else {
            progressBar.setProgress(0);
        }
        if (mduration > 0) {
            runOnUiThread(new Runnable() {
                public void run() {
                    curTimeTv.setText(TimeUtil.sumSecondToTime((int) mcurrenttime));
                    totalTimeTv.setText(TimeUtil.sumSecondToTime((int) mduration));
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                public void run() {
                    curTimeTv.setText(TimeUtil.sumSecondToTime((int) 0));
                    totalTimeTv.setText(TimeUtil.sumSecondToTime((int) 0));
                }
            });
        }

        //播放按钮相关的icon
        runOnUiThread(new Runnable() {
            public void run() {
                if (m_transport_state == TransportState.PLAYING.getValue() && mcurrenttime > 0) { //播放状态
                    playMedia.setImageDrawable(getApplicationContext().getResources().getDrawable(R.drawable.pause_control_ok));
                    playMedia.setClickable(true);
                    volumeAddll.setClickable(true);
                    volumeDelll.setClickable(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        volumeAddll.setTransitionAlpha(1.0f);
                        volumeDelll.setTransitionAlpha(1.0f);
                        stopTv.setTransitionAlpha(1.0f);
                    }
                    progressBar.setClickable(true);
                    stopTv.setClickable(true);
                    loadingTextView.setVisibility(View.INVISIBLE);
                } else if (m_transport_state == TransportState.PAUSED_PLAYBACK.getValue()) { //暂停状态
                    playMedia.setImageDrawable(getApplicationContext().getResources().getDrawable(R.drawable.play_control_ok));
                    playMedia.setClickable(true);
                    volumeAddll.setClickable(true);
                    volumeDelll.setClickable(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        volumeAddll.setTransitionAlpha(1.0f);
                        volumeDelll.setTransitionAlpha(1.0f);
                        stopTv.setTransitionAlpha(1.0f);
                    }
                    progressBar.setClickable(true);
                    stopTv.setClickable(true);
                    loadingTextView.setVisibility(View.INVISIBLE);
                } else { //别的状态一律不可点击
                    playMedia.setImageDrawable(getApplicationContext().getResources().getDrawable(R.drawable.play_control_gray));
                    playMedia.setClickable(false);
                    volumeAddll.setClickable(false);
                    volumeDelll.setClickable(false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        volumeAddll.setTransitionAlpha(0.3f);
                        volumeDelll.setTransitionAlpha(0.3f);
                        stopTv.setTransitionAlpha(0.3f);
                    }
                    progressBar.setClickable(false);
                    loadingTextView.setVisibility(View.VISIBLE);
                }

                if (DlnaWrapper.getInstance().getmDLNAPlayer()!= null && DlnaWrapper.getInstance().getmDLNAPlayer().mDeviceInfo !=null
                        && DlnaWrapper.getInstance().getmDLNAPlayer().mDeviceInfo.getState() == DLNAPlayer.ERROR) {
                    stopTv.setClickable(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        stopTv.setTransitionAlpha(1.0f);
                    }
                }

                if (videoTitle != null && DlnaWrapper.getInstance().getmDLNAPlayer()!=null &&
                        DlnaWrapper.getInstance().getmDLNAPlayer().curFilePath!=null) {
                    String fileName = DlnaWrapper.getInstance().getmDLNAPlayer().getCurFileName();
                    if (fileName!=null && fileName.length() > 12) {
                        fileName = fileName.substring(0, 12) + "..";
                    }
                    videoTitle.setText(fileName);
                }
            }
        });
    }

    private Runnable mHandlerGetPosAndStatusRunnable = new Runnable() {
        @Override
        public void run() {
            if (getStatus == true) {
                getposition();
                gettransportinfo();
            }
            mainHandler.postDelayed(this, 400);
        }
    };

    @Override
    public void onUpItem(String path) {
        String mediaplaylist = sp.getString("mediaplaylist", null);
        if (mediaplaylist != null && !mediaplaylist.equals("")) {
            Gson gson = new Gson();
            List<MediaListItem> list = gson.fromJson(mediaplaylist, new TypeToken<List<MediaListItem>>() {
            }.getType());

            if (list != null && adapter != null) {
                int pos = 0;
                for (MediaListItem item:list) {
                    if (item.getPath().equals(path)) {
                        break;
                    }
                    pos++;
                }
                if (pos < list.size()){
                    MediaListItem removeItem = list.remove(pos);
                    list.add(0, removeItem);
                    saveListData(list);
                    adapter.setNewInstance(list);
                    recyclerView.scrollToPosition(0);
                }
            }
        }
    }

    @Override
    public void onDelItem(String path) {
        String mediaplaylist = sp.getString("mediaplaylist", null);
        if (mediaplaylist != null && !mediaplaylist.equals("")) {
            Gson gson = new Gson();
            List<MediaListItem> list = gson.fromJson(mediaplaylist, new TypeToken<List<MediaListItem>>() {
            }.getType());

            if (list != null && adapter != null) {
                int pos = 0;
                for (MediaListItem item:list) {
                    if (item.getPath().equals(path)) {
                        break;
                    }
                    pos++;
                }
                if (pos < list.size()){
                    list.remove(pos);
                    saveListData(list);
                    adapter.setNewInstance(list);
                }
            }
        }
    }

    @Override
    public void onPlayItem(MediaListItem item1, boolean needFresh) {
        String mediaplaylist = sp.getString("mediaplaylist", null);
        if (mediaplaylist != null && !mediaplaylist.equals("")) {
            Gson gson = new Gson();
            List<MediaListItem> list = gson.fromJson(mediaplaylist, new TypeToken<List<MediaListItem>>() {
            }.getType());

            if (list != null && adapter != null) {
                int pos = 0;
                for (MediaListItem item:list) {
                    if (item.getPath().equals(item1.getPath())) {
                        break;
                    }
                    pos++;
                }
                if (pos < list.size()){
                    MediaListItem removeItem = list.remove(pos);
                    removeItem.setBePlayed(true);
                    list.add(0, removeItem);
                    saveListData(list);
                    if (needFresh == true) {
                        adapter.setNewInstance(list);
                        recyclerView.scrollToPosition(0);
                    }
                }
            }
        }

        ArrayList<Media> list = new ArrayList<>();
        Media media = new Media(item1.getPath(), item1.getName(), item1.getType());
        list.add(media);
        StatusUtil.GotoLocalFileCast(PlayControlActivity.this, list, false);
        needSetStatusCount = 12;
    }

    private void playNextOrBefore(int dir){
        if (DlnaWrapper.getInstance().getmDLNAPlayer() == null) {
            return;
        }

        MediaListItem targetItem = null;
        String mediaplaylist = sp.getString("mediaplaylist", null);
        if (mediaplaylist != null && !mediaplaylist.equals("")) {
            Gson gson = new Gson();
            List<MediaListItem> list = gson.fromJson(mediaplaylist, new TypeToken<List<MediaListItem>>() {
            }.getType());

            if (list != null && list.size() > 0){
                int pos = 0;
                for (MediaListItem item:list) {
                    if (item.getPath().equals(DlnaWrapper.getInstance().getmDLNAPlayer().curFilePath)) {
                        //curPlayVideoInList = true;
                        break;
                    }
                    pos++;
                }
                if(pos>=0 && pos<list.size()) {
                    if (dir == 0) { //向前播放
                        int targetpos = pos-1;
                        if(targetpos>=0 && targetpos<list.size()){
                            onPlayItem(list.get(targetpos), false);
                        } else {
                            //Toast.makeText(mContext, " 无可播放的视频", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        int targetpos = pos+1;
                        if(targetpos>=0 && targetpos<list.size()){
                            onPlayItem(list.get(targetpos), false);
                        } else {
                            //Toast.makeText(mContext, " 无可播放的视频", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    if (dir != 0){ //只有向后播放
                        onPlayItem(list.get(0), false);
                    } else {
                        //Toast.makeText(mContext, " 无可播放的视频", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        return;
    }

    private void saveListData(List<MediaListItem> list){
        if (list != null) {
            Gson gson = new Gson();
            String jsonList = gson.toJson(list);
            editor.putString("mediaplaylist", jsonList);
            editor.commit();
        }
    }
}
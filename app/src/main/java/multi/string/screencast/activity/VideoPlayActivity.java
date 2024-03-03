package multi.string.screencast.activity;

import static sdk.android.zplayer.playercore.InnoPlayerContants.AR_ASPECT_FIT_PARENT;
import static sdk.android.zplayer.playercore.InnoPlayerContants.ERROR_TYPE_NETWORK_CONNECT_TIMEOUT;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_BUFFERING_UPDATE;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_COMPLETED;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_ERROR;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_FIRST_FRAME_DISPLAY;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_NET_SPEED;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_PREPARED;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_PREPARING;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_PROGRESS;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_SEEK_BEGIN;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_SEEK_END;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_VIDEO_FPS;
import static sdk.android.zplayer.playercore.InnoPlayerContants.EVT_PLAY_VIDEO_SIZE;
import static sdk.android.zplayer.playercore.InnoPlayerContants.PLAY_MODE_VOD;
import static sdk.android.zplayer.playercore.InnoPlayerContants.SEEK_MODE_ACCURACY;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import multi.string.screencast.R;
import multi.string.screencast.utils.TouchUtil;
import sdk.android.zplayer.playercore.InnoPlayerCore;
import sdk.android.zplayer.playercore.InnoPlayerListener;
import sdk.android.zplayer.playercore.ZMediaPlayer;
import sdk.android.zplayer.playercore.view.InnoVideoView;

public class VideoPlayActivity extends AppCompatActivity {

    private static final String TAG = "multistring";
    public static final String PATH = "path";
    //本地视频文件
    private String filename = null;//"/storage/emulated/0/SHAREit/download/videos/Tutorial_Joget__Viral_Terbaru_2022.dsv";//"/sdcard/DCIM/Camera/VID_20220503_153932.mp4";// "/storage/emulated/0/ttf/akk.mp4";
    private String mVideoPath;

    private InnoVideoView mVideoView;
    private ZMediaPlayer mMediaPlayer;

    private Button mBtnPlay = null;
    private Button mBtnCancel = null;

    private SeekBar mSeekBar;
    private boolean          mStartSeek = false;
    private boolean          mVideoPlay;
    private boolean          mIsStarted = false;
    private int             mRenderMode = AR_ASPECT_FIT_PARENT;

    private long           mStartPlayTime = 0;
    private int mViewType = InnoVideoView.RENDER_SURFACE_VIEW;
    private static final String KEY_URL = "url";

    public static void startPlayActivity(Context context, String url) {
        Intent intent = new Intent(context, VideoPlayActivity.class);
        intent.putExtra(KEY_URL, url);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        filename = intent.getStringExtra(KEY_URL);
        if (TextUtils.isEmpty(filename)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_play_video);
        mVideoView = (InnoVideoView)findViewById(R.id.video_view);
        mVideoView.setRenderMode(mRenderMode);

        boolean usingTexture = getSharedPreferences("paul", Context.MODE_PRIVATE)
                .getBoolean("using_texture", false);

        mVideoView.setRender(InnoVideoView.RENDER_TEXTURE_VIEW);
        boolean usingHwDecode = getSharedPreferences("paul", Context.MODE_PRIVATE)
                .getBoolean("using_hwdecode", false);

        mMediaPlayer = new ZMediaPlayer(getApplicationContext());
        mMediaPlayer.setPlayerView(mVideoView);
        mMediaPlayer.setPlayListener(mPlayerListener);
        mMediaPlayer.enableHardwareDecode(usingHwDecode);
        mMediaPlayer.enableWzDecode(false);
        InnoPlayerCore.loadLibrariesOnce(null);

        Log.i(TAG, "usingHwDecode is "+usingHwDecode);
        if (filename != null) {
            startplay(filename);
        }

        mBtnCancel = (Button)findViewById(R.id.cancel_btn);
        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    finish();
                }
            }
        });


        mBtnPlay = (Button) findViewById(R.id.btnPlay);
        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TouchUtil.isFastClick()) {
                    if (mVideoPlay) {
                        pausePlay();
                    } else {
                        if (!mIsStarted)
                            startPlay();
                        else
                            resumePlay();
                    }
                }
            }
        });

        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean bFromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                pausePlay();
                mStartSeek = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if ( mMediaPlayer != null) {
                    mMediaPlayer.seekTo(seekBar.getProgress(), SEEK_MODE_ACCURACY);
                }
                resumePlay();
                mStartSeek = false;
            }
        });

        mMediaPlayer.setLooping(false);
        mMediaPlayer.setIsPreload(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 100 || data ==null || data.getExtras() == null || TextUtils.isEmpty(data.getExtras().getString("result"))) {
            return;
        }
        String result = data.getExtras().getString("result");
        startplay(result);
    }

    private void startplay(String result){
        mMediaPlayer.setPlayMode(PLAY_MODE_VOD);
        mVideoPath = result;
        mMediaPlayer.setVideoPath(mVideoPath);
        mMediaPlayer.prepareForPlay();
    }

    private void startPlay(){
        if(mVideoPath == null)
            return;

        mBtnPlay.setBackgroundResource(R.drawable.pause_control_ok);
        if(mMediaPlayer != null){
            mStartPlayTime = System.nanoTime();
            mMediaPlayer.startPlay();
        }

        mVideoPlay = true;
        mIsStarted = true;
    }

    private void stopPlay(){
        mBtnPlay.setBackgroundResource(R.drawable.play_control_ok);
        if(mMediaPlayer != null){
            mMediaPlayer.stopPlay();
        }

        mVideoPlay = false;
        mIsStarted = false;
    }

    private void pausePlay(){
        mBtnPlay.setBackgroundResource(R.drawable.play_control_ok);
        if(mMediaPlayer != null){
            mMediaPlayer.pause();
        }
        mVideoPlay = false;
    }

    private void resumePlay(){
        mBtnPlay.setBackgroundResource(R.drawable.pause_control_ok);
        if(mMediaPlayer != null){
            mMediaPlayer.resume();
        }
        mVideoPlay = true;
    }

    private void backResumePlay(){
        mBtnPlay.setBackgroundResource(R.drawable.pause_control_ok);
        if(mMediaPlayer != null){
            mMediaPlayer.backResume();
        }
        mVideoPlay = true;
    }


    private void handlePlayerError(int errorCode){
        if(errorCode == ERROR_TYPE_NETWORK_CONNECT_TIMEOUT){
            Log.i(TAG, "网络连接超时，请检查网络后重试");
        }
        mVideoPlay = false;
        if (mSeekBar != null) {
            mSeekBar.setProgress(0);
        }
    }

    private InnoPlayerListener mPlayerListener = new InnoPlayerListener() {
        @Override
        public void onPlayerEvent(int msg, int arg0, int arg1, float arg2) {
            switch(msg){
                case EVT_PLAY_PREPARING:
                    long  preparingTime = System.nanoTime();
                    Log.i(TAG, "player begin preparing! cost time from start:" + (preparingTime - mStartPlayTime) / 1000000L + "ms");
                    break;

                case EVT_PLAY_PREPARED:
                    Log.i(TAG,"player prepared! cost time from start:" + (System.nanoTime() - mStartPlayTime) /1000000L + "ms");
                    break;

                case EVT_PLAY_VIDEO_SIZE:
                    break;

                case EVT_PLAY_PROGRESS:
                    if(mSeekBar != null){
                        mSeekBar.setProgress(arg0);
                    }
                    if (mSeekBar != null) {
                        mSeekBar.setMax(arg1);
                    }
                    break;
                case EVT_PLAY_COMPLETED:
                    mVideoPlay = false;
                    pausePlay();
                    break;

                case EVT_PLAY_NET_SPEED:
                    break;

                case EVT_PLAY_VIDEO_FPS:
                    break;

                case EVT_PLAY_BUFFERING_UPDATE:
                    break;

                case EVT_PLAY_SEEK_BEGIN:
                    Log.i(TAG,"seek begin!");
                    break;

                case EVT_PLAY_SEEK_END:
                    Log.i(TAG,"seek end!");
                    break;

                case EVT_PLAY_FIRST_FRAME_DISPLAY:
                    //Toast.makeText(getApplicationContext(),"receive first frame from start cost:" + (System.nanoTime() - mStartPlayTime)/1000000L + "ms", Toast.LENGTH_LONG).show();
                    break;

                case EVT_PLAY_ERROR:
                    handlePlayerError(arg0);
                    break;

                default:
                    break;
            }
        }


        @Override
        public void onPlayerDataCallBack(int msg, JSONObject Data){
        }

        @Override
        public void onTextUpdate(int msg, String txt, int start, int end, int index) {
        }

        @Override
        public void onTextMeta(int msg, String meta) {
        }

        @Override
        public void onTextIndex(int index) {

        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mMediaPlayer != null){
            stopPlay();
            mMediaPlayer.setPlayListener(null);
            mMediaPlayer.destroy();
        }

        if(mVideoView != null){
            mVideoView = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mMediaPlayer != null){
            pausePlay();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(mMediaPlayer != null){
            pausePlay();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(mIsStarted){
            backResumePlay();
        }else{
            startPlay();
        }
    }
}


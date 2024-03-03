package multi.string.screencast.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.multimedia.lib.screening.DLNAPlayer;
import com.multimedia.lib.screening.bean.MediaInfo;
import com.multimedia.lib.screening.listener.DLNAControlCallback;

import org.fourthline.cling.model.action.ActionInvocation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import multi.string.screencast.R;
import multi.string.screencast.adapters.GalleryAdapter;
import multi.string.screencast.dlna.DlnaWrapper;
import multi.string.screencast.models.AlbumItem;
import multi.string.screencast.models.Photo;
import multi.string.screencast.utils.BitmapUtil;
import multi.string.screencast.utils.DensityUtil;
import multi.string.screencast.utils.FileUtil;
import multi.string.screencast.utils.StatusUtil;
import multi.string.screencast.utils.TouchUtil;

public class GalleryActivity extends BaseActivity implements View.OnClickListener {
    private AlbumItem albumItem;
    private GalleryAdapter mAdapter;
    private ImageView maxImg, playImg, rotationImg, backImg;
    private ImageView lastImg, nextImg;
    private RecyclerView recyclerView;
    private boolean isPlay = false;
    private int rotation;
    private Photo mCurPhoto = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        init();
    }

    private void init() {
        immersionBar(true, true);
        albumItem = getIntent().getParcelableExtra("AlbumItem");
        initView();

        initRecycler();
        initListener();
    }

    private void initView() {
        View statusView = findViewById(R.id.headView);
        maxImg = findViewById(R.id.maxImg);
        playImg = findViewById(R.id.playImg);
        backImg = findViewById(R.id.backImg);
        lastImg = findViewById(R.id.lastImg);
        nextImg = findViewById(R.id.nextImg);
        rotationImg = findViewById(R.id.rotationImg);
        recyclerView = findViewById(R.id.recyclerView);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) statusView.getLayoutParams();
        params.topMargin = DensityUtil.getStatusHeightByDensity(mContext);
        statusView.setLayoutParams(params);
    }

    private void initRecycler() {
        mAdapter = new GalleryAdapter(mContext);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext, RecyclerView.HORIZONTAL, false));
        recyclerView.setAdapter(mAdapter);
        if (albumItem.photos != null && albumItem.photos.size() > 0) {
            Photo photo = albumItem.photos.get(0);
            photo.selected = true;
            Glide.with(mContext).load(photo.path).into(maxImg);
            mCurPhoto = photo;
        }
        mAdapter.setNewInstance(albumItem.photos);

        mAdapter.setOnItemClickListener(new GalleryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Photo photo) {
                if (TouchUtil.isFastClick()) {
                    Glide.with(mContext).load(photo.path).into(maxImg);
                    mCurPhoto = photo;

                    playImg.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.photo_play_img));
                    recyclerView.setClickable(true);
                    rotationImg.setClickable(true);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        rotationImg.setTransitionAlpha(1.0f);
                    }
                    StatusUtil.castFileToTv(mCurPhoto.path, MediaInfo.TYPE_IMAGE, false, null);
                    isPlay = false;
                }
            }
        });
    }
    private void initListener(){
        playImg.setOnClickListener(this);
        rotationImg.setOnClickListener(this);
        backImg.setOnClickListener(this);
        lastImg.setOnClickListener(this);
        nextImg.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (TouchUtil.isFastClick()) {
            if (v.getId() == R.id.playImg) {
                if (!isPlay) {
                    playImg.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.photo_pause_img));
                    mHandlerPlayPhoto.sendEmptyMessageDelayed(0, 2000);
                    recyclerView.setClickable(false);
                    rotationImg.setClickable(false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        rotationImg.setTransitionAlpha(0.3f);
                    }
                } else {
                    playImg.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.photo_play_img));
                    recyclerView.setClickable(true);
                    rotationImg.setClickable(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        rotationImg.setTransitionAlpha(1.0f);
                    }
                }
                isPlay = !isPlay;
            } else if (v.getId() == R.id.rotationImg) {
                mHandlerRotate.removeMessages(rotation);
                rotation += 90;
                if (rotation == 360) {
                    rotation = 0;
                }
                mHandlerRotate.sendEmptyMessage(rotation);
                rotationImg.animate().rotation(rotation);
            } else if (v.getId() == R.id.backImg) {
                stopPlay();
                mHandlerPlayPhoto.removeMessages(0);
                mHandlerPlayPhoto.removeMessages(1);
                finish();
            } else if (v.getId() == R.id.lastImg) {
                playOneImg(0);
            } else if (v.getId() == R.id.nextImg) {
                playOneImg(1);
            }
        }
    }

    /**
     * 判断Activity是否Destroy
     * @param
     * @return
     */
    public static boolean isDestroy(Activity mActivity) {
        if (mActivity== null || mActivity.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && mActivity.isDestroyed())) {
            return true;
        } else {
            return false;
        }
    }


    //Wifi网络监测线程  然后在onCreate方法里面开启
    private final Handler mHandlerPlayPhoto = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                if (isPlay == true && mAdapter!= null){
                    if(!isDestroy(GalleryActivity.this)) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                playOneImg(1);
                            }
                        });
                    }
                    sendEmptyMessageDelayed(0,6000); //四秒
                }
            } else if (msg.what == 1){ //停止

            }
        }
    };

    private final Handler mHandlerRotate = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mCurPhoto != null && mCurPhoto.path != null) {
                //File outputDir = mContext.getCacheDir();
//                File outputDir = mContext.getCacheDir();
//                String adsDir  = outputDir.getAbsolutePath()+"/frontPic";
//                String absPath = outputDir.getAbsolutePath()+"/frontPic/"+itemHolders[0].videoId+".jpg";
//                String outputDir = Environment.getExternalStoragePublicDirectory("").toString();
                String outputDir = mContext.getExternalFilesDir("PicCacheZCast").getAbsolutePath();

                String adsDir  = outputDir;//outputDir+"/PicCacheZCast";
                String absPath = outputDir+"/"+msg.what+".jpg";
                boolean ret = FileUtil.createDir(adsDir);

                Bitmap b = FileUtil.openImage(mCurPhoto.path);
                if (ret == true && b != null){
                    Bitmap copyBitmap = BitmapUtil.adjustPhotoRotation(b, msg.what);

                    if (copyBitmap != null){
                        try {
                            FileOutputStream fos=new FileOutputStream(absPath);
                            boolean isok = copyBitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
                            fos.close();
                            if (isok == true){
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        if(mContext != null && maxImg != null) {
                                            Glide.with(mContext).load(absPath).into(maxImg);
                                        }
                                    }
                                });
                                StatusUtil.castFileToTv(absPath, MediaInfo.TYPE_IMAGE, false, null);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };

    private void playOneImg(int direction){
        if (direction == 0) {//向前
            int curPos = 0;
            for (int i = 0; i < albumItem.photos.size(); i++) {
                albumItem.photos.get(i).selected=false;
                if (mCurPhoto.path.equals(albumItem.photos.get(i).path)){
                    curPos = i;
                }
            }

            if (0<(albumItem.photos.size())){
                mCurPhoto = albumItem.photos.get((curPos-1+albumItem.photos.size())%albumItem.photos.size());
                mCurPhoto.selected = true;
                mAdapter.notifyDataSetChanged();
                Glide.with(mContext).load(mCurPhoto.path).into(maxImg);
                recyclerView.scrollToPosition((curPos-1+albumItem.photos.size())%albumItem.photos.size());
                StatusUtil.castFileToTv(mCurPhoto.path, MediaInfo.TYPE_IMAGE, false, null);
            }
        } else { //向后
            int curPos = 0;
            for (int i = 0; i < albumItem.photos.size(); i++) {
                albumItem.photos.get(i).selected=false;
                if (mCurPhoto.path.equals(albumItem.photos.get(i).path)){
                    curPos = i;
                }
            }

            if (0<(albumItem.photos.size())){
                mCurPhoto = albumItem.photos.get((curPos+1)%albumItem.photos.size());
                mCurPhoto.selected = true;
                mAdapter.notifyDataSetChanged();
                Glide.with(mContext).load(mCurPhoto.path).into(maxImg);
                recyclerView.scrollToPosition((curPos+1)%albumItem.photos.size());
                StatusUtil.castFileToTv(mCurPhoto.path, MediaInfo.TYPE_IMAGE, false, null);
            }
        }
    }

    private void stopPlay(){
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            DlnaWrapper.getInstance().getmDLNAPlayer().stop(new DLNAControlCallback() {
                @Override
                public void onSuccess(@Nullable ActionInvocation invocation) {
                    if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
                        DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.STOP);
                    }
                }

                @Override
                public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {
                }

                @Override
                public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                    if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
                        DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.STOP);
                    }
                }
            });
        }
        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null){
            DlnaWrapper.getInstance().getmDLNAPlayer().setStatus(DLNAPlayer.UNKNOWN);
        }
    }
}

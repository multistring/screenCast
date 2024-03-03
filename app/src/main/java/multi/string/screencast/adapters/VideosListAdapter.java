package multi.string.screencast.adapters;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import multi.string.screencast.R;
import multi.string.screencast.activity.VideoPlayActivity;
import multi.string.screencast.models.Media;
import multi.string.screencast.models.MediaListItem;
import multi.string.screencast.models.Video;
import multi.string.screencast.utils.BitmapUtil;
import multi.string.screencast.utils.FileUtil;
import multi.string.screencast.utils.StatusUtil;
import multi.string.screencast.utils.TimeUtil;
import multi.string.screencast.utils.TouchUtil;
import sdk.android.zplayer.playercore.InnoNativeParser;

public class VideosListAdapter extends BaseMediaAdapter<VideosListAdapter.ItemHolder>{
    private List<Video> arraylist;
    private FragmentActivity mContext;
    private final InnoNativeParser mParser;

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    public VideosListAdapter(FragmentActivity context, List<Video> arraylist) {
        this.arraylist = arraylist;
        mContext = context;
        mParser = new InnoNativeParser();
        // 获得只能被本应用程序读、写的SharedPreferences对象
        sp = context.getSharedPreferences("testpreferences", MODE_PRIVATE);
        // 获得Editor对象
        editor = sp.edit();
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_video, viewGroup, false);
        ItemHolder ml = new ItemHolder(v);
        return ml;
    }

    @Override
    public void onBindViewHolder(ItemHolder itemHolder, int i) {
        Video localItem = arraylist.get(i);

        String simple_title = localItem.title;
        if (localItem.title!=null && localItem.title.length() > 11) {
            simple_title = localItem.title.substring(0, 11) + "...";
        }
        itemHolder.title.setText(simple_title!=null?simple_title:"demo");
        itemHolder.resolution.setText(Integer.valueOf(localItem.width).toString() + "X" + Integer.valueOf(localItem.height).toString());
        itemHolder.title.setTextColor(Color.WHITE);
        itemHolder.resolution.setTextColor(Color.LTGRAY);
        itemHolder.durtion.setTextColor(Color.LTGRAY);

        itemHolder.setVideoId(localItem.id);
        itemHolder.setVideoIndex(i);
        itemHolder.setVideoPath(localItem.path);
        itemHolder.setVideoName(simple_title);

        localItem.setIs_show(true);
        new videoFrontPicTask().execute(itemHolder);
        setOnPopupMenuListener(itemHolder, i);
    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    @Override
    public void updateDataSet(List<Video> arraylist) {
        this.arraylist = arraylist;
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected TextView title;
        protected TextView resolution;
        protected TextView durtion;
        protected ImageView frontPicView;
        protected ImageView popupMenu; //右边的三点操作按钮
        public int videoId; //视频的唯一标志符
        public int videoIndex; //视频在list中的index
        public String videoPath;
        public String videoName;
        private int  width=0;
        private int  height=0;
        private int  duration=0;

        public ItemHolder(@NonNull View view) {
            super(view);
            this.title = (TextView) view.findViewById(R.id.video_title);
            this.resolution = (TextView) view.findViewById(R.id.video_resolution);
            this.durtion = (TextView) view.findViewById(R.id.video_duration);
            this.frontPicView = (ImageView) view.findViewById(R.id.FrontPic);
            this.popupMenu = (ImageView) view.findViewById(R.id.popup_menu);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (TouchUtil.isFastClick()) {
                gotoLocalFileCast();
            }
        }

        //判断当前的状态，根据状态跳转到不同的页面
        public void gotoLocalFileCast(){
            ArrayList<Media> list = new ArrayList<>();
            Media media = new Media(videoPath, videoName, Media.VIDEO_TYPE);
            list.add(media);
            StatusUtil.GotoLocalFileCast(mContext, list, true);
        }

        public int getVideoId() {
            return videoId;
        }

        public void setVideoId(int videoId) {
            this.videoId = videoId;
        }

        public String getVideoPath() {
            return videoPath;
        }

        public void setVideoPath(String videoPath) {
            this.videoPath = videoPath;
        }

        public int getVideoIndex() {
            return videoIndex;
        }

        public void setVideoIndex(int videoIndex) {
            this.videoIndex = videoIndex;
        }

        public String getVideoName() {
            return videoName;
        }

        public void setVideoName(String videoName) {
            this.videoName = videoName;
        }
    }

    public Video getVideoAt(int i) {
        return arraylist.get(i);
    }

    public void addVideoTo(int i, Video video) {
        arraylist.add(i, video);
    }

    private class videoFrontPicTask extends AsyncTask<ItemHolder, Void, String> {
        private  ItemHolder mItemHolder = null;
        HashMap<String, String> metaDataMap = null;
        @Override
        protected String doInBackground(ItemHolder... itemHolders) {
            String  picPath = null;
            metaDataMap = null;
            mItemHolder = itemHolders[0];
            File outputDir = mContext.getCacheDir();
            String adsDir  = outputDir.getAbsolutePath()+"/frontPic";
            String absPath = outputDir.getAbsolutePath()+"/frontPic/"+itemHolders[0].videoId+".jpg";
            int width = 480;
            int height = 320;

            if (mItemHolder.getVideoIndex()>=0 && mItemHolder.getVideoIndex()<arraylist.size()) {
                Video localItem = arraylist.get(mItemHolder.getVideoIndex());

                //获取宽高
                if (localItem.width <=0 || localItem.height <=0) {
                    metaDataMap = mParser.getMetaData(mItemHolder.videoPath);
                    if (metaDataMap != null && metaDataMap.get("video_width")!=null && metaDataMap.get("video_height")!=null) {
                        localItem.width = Integer.valueOf(metaDataMap.get("video_width")).intValue();
                        localItem.height = Integer.valueOf(metaDataMap.get("video_height")).intValue();
                    }
                } else {
                    metaDataMap = new HashMap<>();
                    metaDataMap.put("video_width", String.valueOf(localItem.width));
                    metaDataMap.put("video_height", String.valueOf(localItem.height));
                }

                if (localItem.width > 0 && localItem.height > 0){
                    width = localItem.width;
                    height = localItem.height;
                }

                //获取时长
                if (localItem.duration < 0) {
                    if (metaDataMap == null) {
                        metaDataMap = mParser.getMetaData(mItemHolder.videoPath);
                    }
                    if (metaDataMap != null && metaDataMap.get("duration")!=null) {
                        localItem.duration = Integer.valueOf(metaDataMap.get("duration")).intValue();
                    }
                } else {
                    if (metaDataMap != null) {
                        metaDataMap.put("duration", String.valueOf(localItem.duration));
                    }
                }
            } else {
                metaDataMap = mParser.getMetaData(mItemHolder.videoPath);
                if (metaDataMap != null && metaDataMap.get("video_width")!=null && metaDataMap.get("video_height")!=null) {
                    width = Integer.valueOf(metaDataMap.get("video_width")).intValue();
                    height = Integer.valueOf(metaDataMap.get("video_height")).intValue();
                    if (width <=0 || height<=0) {
                        width = 480;
                        height = 320;
                    }
                }
            }

            FileUtil.createDir(adsDir);
            File tmpFile = new File(absPath);
            if (!tmpFile.exists()) {
                float rate = (float) width/height;
                width = 240;
                height = (int)(width/rate);
                byte[] ARGB = mParser.getPictureAt(mItemHolder.videoPath, width, height, -1);
                if (ARGB == null) {
                    return null;
                }
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(ARGB));

                Bitmap copyBitmap = bitmap;
                if (metaDataMap != null && metaDataMap.get("video_rotation")!=null){
                    int rotate = Integer.valueOf(metaDataMap.get("video_rotation")).intValue();
                    if (rotate > 0) {
                        copyBitmap = BitmapUtil.adjustPhotoRotation(bitmap, rotate);
                    }
                }

                if (copyBitmap == null){
                    copyBitmap = bitmap;
                } else {
                    if (copyBitmap.getWidth() >0 && copyBitmap.getHeight()>0) {
                        float showRate = 1.5f;
                        float videoRate = (float) copyBitmap.getWidth() / copyBitmap.getHeight();
                        if (videoRate > showRate+0.3f){
                            copyBitmap = BitmapUtil.cropBitmap(copyBitmap, showRate, videoRate);
                        } else if (videoRate < showRate-0.3f) {
                            copyBitmap = BitmapUtil.cropBitmap(copyBitmap, showRate, videoRate);
                        }
                    }
                }

                try {
                    FileOutputStream fos=new FileOutputStream(tmpFile);
                    copyBitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return absPath;
        }

        @Override
        protected void onPostExecute(String absPath) {
            //首页已经生成
            if (!TextUtils.isEmpty(absPath)) {
                Bitmap b = FileUtil.openImage(absPath);
                mItemHolder.frontPicView.post(new Runnable() {
                    public void run() {
                        mItemHolder.frontPicView.setImageBitmap(b);
                    }
                });
            }

            if (metaDataMap != null && metaDataMap.get("video_width") != null && metaDataMap.get("video_height") != null) {
                String resolution = metaDataMap.get("video_width") + "X" + metaDataMap.get("video_height");
                mItemHolder.resolution.post(new Runnable() {
                    public void run() {
                        mItemHolder.resolution.setText(resolution);
                    }
                });
            }

            if (metaDataMap != null && metaDataMap.get("duration") != null) {
                int duration = Integer.valueOf(metaDataMap.get("duration")).intValue();//metaDataMap.get("duration")
                String time = TimeUtil.sumSecondToTime(duration/1000);
                mItemHolder.durtion.post(new Runnable() {
                    public void run() {
                        mItemHolder.durtion.setText(time);
                    }
                });
            }
        }

        @Override
        protected void onPreExecute() {
        }
    }

    private void setOnPopupMenuListener(final ItemHolder itemHolder, final int position) {

        itemHolder.popupMenu.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                if (TouchUtil.isFastClick()) {
                    final PopupMenu menu = new PopupMenu(mContext, v);
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (TouchUtil.isFastClick()) {
                                switch (item.getItemId()) {
                                    case R.id.popup_meida_add_playlist: //添加到队列
                                        addVideoItem2Playlist(itemHolder);
                                        break;
                                    case R.id.popup_meida_play: //本地播放
                                        VideoPlayActivity.startPlayActivity(mContext, itemHolder.videoPath);
                                        break;
                                    case R.id.popup_meida_play_tv: //在电视上播放
                                        itemHolder.gotoLocalFileCast();
                                        break;
                                }
                            }
                            return false;
                        }
                    });
                    menu.inflate(R.menu.popup_video);

                    try {
                        Field field = menu.getClass().getDeclaredField("mPopup");
                        field.setAccessible(true);
                        MenuPopupHelper mHelper = (MenuPopupHelper) field.get(menu);
                        mHelper.setForceShowIcon(true);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } finally {
                        menu.show();
                    }
                }
            }
        });
    }


    private void addVideoItem2Playlist(ItemHolder itemHolder){
        if (itemHolder == null || itemHolder.videoPath == null){
            return;
        }
        String mediaplaylist = sp.getString("mediaplaylist", null);
        if (mediaplaylist != null && !mediaplaylist.equals("")) {
            Gson gson = new Gson();
            List<MediaListItem> list =gson.fromJson(mediaplaylist, new TypeToken<List<MediaListItem>>() {}.getType());
            if (list != null) {
                boolean hasInserted = false;
                for (MediaListItem item:list){
                    if (item.getPath()!=null && item.getPath().equals(itemHolder.videoPath)){
                        hasInserted = true;
                    }
                }

                if (hasInserted ==  false) {
                    MediaListItem item = getMediaItem(itemHolder);
                    if (item != null) {
                        list.add(item);
                        saveListData(list);
                    }
                } else {
                }
            }
        } else { //第一个列表视频
            MediaListItem item = getMediaItem(itemHolder);
            if (item != null) {
                List<MediaListItem> list = new ArrayList<MediaListItem>();
                list.add(item);
                saveListData(list);
            }
        }
    }

    private MediaListItem getMediaItem(ItemHolder itemHolder){
        HashMap<String, String> metaMap = mParser.getMetaData(itemHolder.videoPath);
        if (metaMap != null && metaMap.get("video_width") != null && metaMap.get("video_height") != null
                && metaMap.get("duration") != null) {
            int width = Integer.valueOf(metaMap.get("video_width")).intValue();
            int height = Integer.valueOf(metaMap.get("video_height")).intValue();
            int duration = Integer.valueOf(metaMap.get("duration")).intValue();
            MediaListItem item = new MediaListItem();
            item.setName(itemHolder.videoName);
            item.setPath(itemHolder.videoPath);
            item.setType(MediaListItem.VIDEO_TYPE);
            item.setBePlayed(false);
            item.setWidth(width);
            item.setHeight(height);
            item.setDur(duration/1000);

            File outputDir = mContext.getCacheDir();
            String absPath = outputDir.getAbsolutePath()+"/frontPic/"+itemHolder.videoId+".jpg";
            item.setFrontpgPath(absPath);
            return item;
        } else {
            return null;
        }
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


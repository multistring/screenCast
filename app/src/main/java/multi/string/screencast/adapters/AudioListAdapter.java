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
import multi.string.screencast.models.Audio;
import multi.string.screencast.models.Media;
import multi.string.screencast.models.MediaListItem;
import multi.string.screencast.utils.FileUtil;
import multi.string.screencast.utils.StatusUtil;
import multi.string.screencast.utils.TimeUtil;
import multi.string.screencast.utils.TouchUtil;
import sdk.android.zplayer.playercore.InnoNativeParser;

public class AudioListAdapter extends BaseMediaAdapter<AudioListAdapter.ItemHolder>{
    private List<Audio> arraylist;
    private FragmentActivity mContext;
    private final InnoNativeParser mParser;

    //队列存取相关
    // 声明SharedPreferences 对象
    private SharedPreferences sp;
    // 声明editor 对象
    private SharedPreferences.Editor editor;

    public AudioListAdapter(FragmentActivity context, List<Audio> arraylist) {
        this.arraylist = arraylist;
        mContext = context;
        mParser = new InnoNativeParser();

        // 获得只能被本应用程序读、写的SharedPreferences对象
        sp = context.getSharedPreferences("testpreferences", MODE_PRIVATE);
        // 获得Editor对象
        editor = sp.edit();
    }

    @Override
    public AudioListAdapter.ItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_audio, viewGroup, false);
        AudioListAdapter.ItemHolder ml = new AudioListAdapter.ItemHolder(v);
        return ml;
    }

    @Override
    public void onBindViewHolder(AudioListAdapter.ItemHolder itemHolder, int i) {
        Audio localItem = arraylist.get(i);

        String simple_title = localItem.title;
        if (localItem.title != null && localItem.title.length() > 11) {
            simple_title = localItem.title.substring(0, 11) + "...";
        }
        itemHolder.title.setText(simple_title!=null?simple_title:"demo");
        itemHolder.title.setTextColor(Color.WHITE);
        itemHolder.duration.setTextColor(Color.LTGRAY);

        itemHolder.setAudioId(localItem.id);
        itemHolder.setAudioIndex(i);
        itemHolder.setAudioPath(localItem.path);
        itemHolder.setAudioName(simple_title);

        new AudioListAdapter.audioFrontPicTask().execute(itemHolder);
        setOnPopupMenuListener(itemHolder, i);
    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected TextView title;
        protected TextView duration;
        protected ImageView frontPicView;
        protected ImageView popupMenu; //右边的三点操作按钮
        public long audioId; //视频的唯一标志符
        public int audioIndex; //视频在list中的index
        public String audioPath;
        public String audioName;
        public ItemHolder(@NonNull View view) {
            super(view);
            this.title = (TextView) view.findViewById(R.id.audio_title);
            this.duration = (TextView) view.findViewById(R.id.audio_duration);
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

        public void gotoLocalFileCast(){
            ArrayList<Media> list = new ArrayList<>();
            Media media = new Media(audioPath, "audioName", Media.AUDIO_TYPE);
            list.add(media);
            StatusUtil.GotoLocalFileCast(mContext, list, true);
        }

        public long getAudioId() {
            return audioId;
        }

        public void setAudioId(long audioId) {
            this.audioId = audioId;
        }

        public int getAudioIndex() {
            return audioIndex;
        }

        public void setAudioIndex(int audioIndex) {
            this.audioIndex = audioIndex;
        }

        public String getAudioPath() {
            return audioPath;
        }

        public void setAudioPath(String audioPath) {
            this.audioPath = audioPath;
        }

        public String getAudioName() {
            return audioName;
        }

        public void setAudioName(String audioName) {
            this.audioName = audioName;
        }
    }

    public Audio getVideoAt(int i) {
        return arraylist.get(i);
    }

    public void addVideoTo(int i, Audio audio) {
        arraylist.add(i, audio);
    }

    private class audioFrontPicTask extends AsyncTask<ItemHolder, Void, String> {
        private AudioListAdapter.ItemHolder mItemHolder = null;
        HashMap<String, String> metaDataMap = null;
        @Override
        protected String doInBackground(AudioListAdapter.ItemHolder... itemHolders) {
            String  picPath = null;
            mItemHolder = itemHolders[0];
            File outputDir = mContext.getCacheDir();
            String adsDir  = outputDir.getAbsolutePath()+"/audiofrontPic";
            String absPath = outputDir.getAbsolutePath()+"/audiofrontPic/"+itemHolders[0].audioId+".jpg";

            if (mItemHolder.getAudioIndex()<0 || mItemHolder.getAudioIndex()>=arraylist.size()) {
                return absPath;
            }

            Audio item = arraylist.get(mItemHolder.getAudioIndex());

            if (item.is_show == false) {
                FileUtil.createDir(adsDir);
                File tmpFile = new File(absPath);
                if (!tmpFile.exists()) {
                    int width = 300;
                    int height = 300;
                    byte[] ARGB = mParser.getPictureAt(mItemHolder.audioPath, width, height, -1);
                    if (ARGB != null) {
                        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(ARGB));

                        try {
                            FileOutputStream fos = new FileOutputStream(tmpFile);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (mItemHolder.getAudioIndex()>=0 && mItemHolder.getAudioIndex()<arraylist.size()) {
                Audio localItem = arraylist.get(mItemHolder.getAudioIndex());
                if (localItem.duration < 0) {
                    metaDataMap = mParser.getMetaData(mItemHolder.audioPath);
                    try {
                        if (metaDataMap != null && metaDataMap.get("duration")!=null) {
                            if (Integer.valueOf(metaDataMap.get("duration")) > 10) {
                                localItem.duration = Integer.valueOf(metaDataMap.get("duration")).intValue();
                            } else {
                                localItem.duration = 0;
                            }
                        }
                    } catch (Exception e) {
                        localItem.duration = 0;
                    }
                } else {
                    metaDataMap = new HashMap<>();
                    metaDataMap.put("duration", String.valueOf(localItem.duration));
                }
            } else {
                metaDataMap = mParser.getMetaData(mItemHolder.audioPath);
            }

            item.is_show = true;
            return absPath;
        }

        @Override
        protected void onPostExecute(String absPath) {
            //首页已经生成
            if (!TextUtils.isEmpty(absPath)) {
                Bitmap b = FileUtil.openImage(absPath);

                mItemHolder.frontPicView.post(new Runnable() {
                    public void run() {
                        if (b != null) {
                            mItemHolder.frontPicView.setImageBitmap(b);
                        } else {
                            mItemHolder.frontPicView.setImageResource(R.drawable.ic_empty_audio);
                        }
                    }
                });
            } else {
                mItemHolder.frontPicView.post(new Runnable() {
                    public void run() {
                        mItemHolder.frontPicView.setImageResource(R.drawable.ic_empty_audio);
                    }
                });
            }

            if (metaDataMap != null && metaDataMap.get("duration") != null) {
                String dur = metaDataMap.get("duration");
                mItemHolder.duration.post(new Runnable() {
                    public void run() {
                        try {
                            int ms = Integer.valueOf(dur).intValue();
                            ms /=1000;
                            mItemHolder.duration.setText(TimeUtil.sumSecondToTime(ms));
                        } catch (Exception e){
                            mItemHolder.duration.setText("00:00:00");
                        }
                    }
                });
            } else {
                mItemHolder.duration.post(new Runnable() {
                    public void run() {
                        mItemHolder.duration.setText("00:00:00");
                    }
                });
            }
        }

        @Override
        protected void onPreExecute() {
        }
    }

    private void setOnPopupMenuListener(final AudioListAdapter.ItemHolder itemHolder, final int position) {

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
                                    case R.id.popup_meida_add_playlist:
                                        addAudioItem2Playlist(itemHolder);
                                        break;
                                    case R.id.popup_meida_play:
                                        VideoPlayActivity.startPlayActivity(mContext, itemHolder.audioPath);
                                        break;
                                    case R.id.popup_meida_play_tv:
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

    private void addAudioItem2Playlist(AudioListAdapter.ItemHolder itemHolder){
        if (itemHolder == null || itemHolder.audioPath == null){
            return;
        }
        String mediaplaylist = sp.getString("mediaplaylist", null);
        if (mediaplaylist != null && !mediaplaylist.equals("")) {
            Gson gson = new Gson();
            List<MediaListItem> list =gson.fromJson(mediaplaylist, new TypeToken<List<MediaListItem>>() {}.getType());

            if (list != null) {
                boolean hasInserted = false;
                for (MediaListItem item:list){
                    if (item.getPath()!=null && item.getPath().equals(itemHolder.audioPath)){
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
        } else { //第一个列表音频
            MediaListItem item = getMediaItem(itemHolder);
            if (item != null) {
                List<MediaListItem> list = new ArrayList<MediaListItem>();
                list.add(item);
                saveListData(list);
            }
        }
    }

    private MediaListItem getMediaItem(AudioListAdapter.ItemHolder itemHolder){
        HashMap<String, String> metaMap = mParser.getMetaData(itemHolder.audioPath);
        if (metaMap != null && metaMap.get("duration")!=null) {
            int duration = Integer.valueOf(metaMap.get("duration")).intValue();
            MediaListItem item = new MediaListItem();
            item.setName(itemHolder.audioName);
            item.setPath(itemHolder.audioPath);
            item.setType(MediaListItem.AUDIO_TYPE);
            item.setBePlayed(false);
            item.setDur(duration/1000);
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


package multi.string.screencast.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.daimajia.swipe.SwipeLayout;

import java.io.File;
import java.util.ArrayList;

import multi.string.screencast.R;
import multi.string.screencast.dlna.DlnaWrapper;
import multi.string.screencast.models.MediaListItem;
import multi.string.screencast.utils.FileUtil;
import multi.string.screencast.utils.TimeUtil;
import multi.string.screencast.utils.TouchUtil;

public class VideoQueueAdapter extends BaseQuickAdapter<MediaListItem, BaseViewHolder> {
    private OnItemClickListen onItemClickListen = null;
    private Context mContext = null;

    public VideoQueueAdapter(Context context) {
        super(R.layout.video_list_item);
        mContext = context;
    }

    public VideoQueueAdapter(OnItemClickListen listen, Context context) {
        super(R.layout.video_list_item);
        onItemClickListen = listen;
        mContext = context;
    }

    public VideoQueueAdapter(int layoutResId, ArrayList<MediaListItem> data) {
        super(layoutResId, data);
    }


    @Override
    protected void convert(BaseViewHolder holder, MediaListItem item) {
        SwipeLayout swipeLayout = holder.getView(R.id.swipeLayout);
        swipeLayout.setSwipeEnabled(false);

        //视频封面
        if (item.getType() == MediaListItem.VIDEO_TYPE) {
            AppCompatImageView videoImage = holder.getView(R.id.videoImg);
            if (item.getFrontpgPath() != null) {
                File tmpFile = new File(item.getFrontpgPath());
                if (tmpFile.exists() && tmpFile.length() > 1000) {
                    Bitmap b = FileUtil.openImage(item.getFrontpgPath());
                    videoImage.setImageBitmap(b);
                }
            }
        } else { //音频 默认就是音频icon
            AppCompatImageView audioImage = holder.getView(R.id.videoImg);
            audioImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_empty_audio));
        }

        //视频名字
        TextView videoName = holder.getView(R.id.nameTv);
        if (item.getName() != null) {
            videoName.setText(item.getName());
        }

        //分辨率
        TextView videoSize = holder.getView(R.id.sizeTv);
        if (item.getType() == MediaListItem.VIDEO_TYPE) {
            videoSize.setTextColor(Color.LTGRAY);
            if (item.getWidth() > 0 && item.getHeight() > 0) {
                videoSize.setText(Integer.valueOf(item.getWidth()).toString() + "X" + Integer.valueOf(item.getHeight()).toString());
            }
        } else {
            videoSize.setTextColor(Color.LTGRAY);
            videoSize.setText("0X0");
        }
        //时长
        TextView videoTime = holder.getView(R.id.timeTv);
        videoTime.setTextColor(Color.LTGRAY);
        if (item.getDur() > 0) {
            String time = TimeUtil.sumSecondToTime(item.getDur());
            videoTime.setText(time);
        }

        if (DlnaWrapper.getInstance().getmDLNAPlayer() != null) {
            String curPlayPath = DlnaWrapper.getInstance().getmDLNAPlayer().getCurFilePath();
            if (curPlayPath != null && curPlayPath.equals(item.getPath())) {
                videoName.setTextColor(Color.GREEN);
                videoSize.setTextColor(Color.GREEN);
                videoTime.setTextColor(Color.GREEN);
            }
        }

        //右边的更多
        ImageView moreImg = holder.getView(R.id.moreImg);
        moreImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TouchUtil.isFastClick()) {
                    if (swipeLayout.getOpenStatus().equals(SwipeLayout.Status.Close)) {
                        swipeLayout.open();
                        closeOther(swipeLayout);
                        ((ImageView) v).setImageDrawable(mContext.getResources().getDrawable(R.drawable.media_list_toright));
                    } else {
                        swipeLayout.close();
                        ((ImageView) v).setImageDrawable(mContext.getResources().getDrawable(R.drawable.media_list_toleft));
                    }
                }
            }
        });

        ImageView upImage = holder.getView(R.id.upImg);
        upImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    if (onItemClickListen != null) {
                        onItemClickListen.onUpItem(item.getPath());
                    }
                }
            }
        });

        ImageView delImage = holder.getView(R.id.delImg);
        delImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    if (onItemClickListen != null) {
                        onItemClickListen.onDelItem(item.getPath());
                    }
                }
            }
        });

        ImageView playImage = holder.getView(R.id.playImg);
        playImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    if (onItemClickListen != null) {
                        onItemClickListen.onPlayItem(item, true);
                    }
                }
            }
        });
    }

    private void closeOther(SwipeLayout swipeLayout){
        for (int i=0; i < getRecyclerView().getChildCount();i++) {
            RecyclerView.ViewHolder viewHolder = getRecyclerView().findViewHolderForLayoutPosition(i);
            if (viewHolder != null && viewHolder instanceof BaseViewHolder){
                BaseViewHolder baseViewHolder = (BaseViewHolder)viewHolder;
                SwipeLayout otherSwipelayout = ((BaseViewHolder)baseViewHolder).getView(R.id.swipeLayout);
                if (!otherSwipelayout.equals(swipeLayout)) {
                    otherSwipelayout.close();
                    ImageView view = baseViewHolder.getView(R.id.moreImg);
                    ((ImageView)view).setImageDrawable(mContext.getResources().getDrawable(R.drawable.media_list_toleft));
                }
            }
        }
    }

    public interface OnItemClickListen{
        void onUpItem(String path);
        void onDelItem(String path);
        void onPlayItem(MediaListItem item , boolean needFresh);
    }

}


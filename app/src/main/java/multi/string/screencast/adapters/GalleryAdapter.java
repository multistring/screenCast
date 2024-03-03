package multi.string.screencast.adapters;

import android.content.Context;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import multi.string.screencast.R;
import multi.string.screencast.models.Photo;

public class GalleryAdapter extends BaseQuickAdapter<Photo, BaseViewHolder> {
    private Context mContext;

    public GalleryAdapter(Context context) {
        super(R.layout.gallery_item);
        this.mContext = context;
    }

    @Override
    protected void convert(BaseViewHolder holder, Photo photo) {
        Glide.with(mContext).load(photo.path).into((ImageView) holder.getView(R.id.imgView));
        ConstraintLayout constraintLayout = holder.getView(R.id.constraintLayout);
        if (photo.selected) {
            constraintLayout.setBackgroundResource(R.drawable.gray_bg_shape);
        } else {
            constraintLayout.setBackgroundResource(0);
        }

        constraintLayout.setTag(getItemPosition(photo));
        constraintLayout.setOnClickListener(v -> {
            int p=(int)v.getTag();
            for (int i = 0; i < getData().size(); i++) {
                getData().get(i).selected=false;
            }
            getData().get(p).selected=true;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onItemClick(photo);
            }
        });

    }

    public interface OnItemClickListener {
        void onItemClick(Photo photo);
    }

    public OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}


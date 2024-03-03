package multi.string.screencast.adapters;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import multi.string.screencast.R;
import multi.string.screencast.models.AlbumItem;

public class FolderGridAdapter extends BaseQuickAdapter<AlbumItem, BaseViewHolder> {
    private Context mContext;

    public FolderGridAdapter(Context context) {
        super(R.layout.folder_grid_item);
        this.mContext = context;
    }

    @Override
    protected void convert(BaseViewHolder holder, AlbumItem albumItem) {
        if (albumItem != null && albumItem.photos != null && albumItem.photos.size() > 0) {
            Glide.with(mContext).load(albumItem.photos.get(0).path).into((ImageView) holder.getView(R.id.img));
        }
        holder.setText(R.id.folderNameTv, albumItem.name);
        holder.setText(R.id.folderSizeTv, mContext.getString(R.string.folder_num, albumItem.photos.size()));
        holder.getView(R.id.constraintLayout).setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(albumItem);
            }
        });

    }

    public interface OnItemClickListener {
        void onItemClick(AlbumItem albumItem);
    }

    public OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}

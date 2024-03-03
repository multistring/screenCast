package multi.string.screencast.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import multi.string.screencast.models.Video;

public class BaseMediaAdapter<V extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<V> {

    @Override
    public V onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(V holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public class ItemHolder extends RecyclerView.ViewHolder {

        public ItemHolder(View view) {
            super(view);
        }

    }

    public void removeSongAt(int i){}
    public void updateDataSet(List<Video> arraylist) {}

}

package multi.string.screencast.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.appthemeengine.Config;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;

import multi.string.screencast.utils.Helpers;

public class BaseRecyclerView extends RecyclerView {
    private View emptyView;
    private AdapterDataObserver emptyObserver = new AdapterDataObserver() {

        @Override
        public void onChanged() {
            Adapter<?> adapter =  getAdapter();
            if(adapter != null && emptyView != null) {
                if(adapter.getItemCount() == 0) {
                    emptyView.setVisibility(View.VISIBLE);
                    BaseRecyclerView.this.setVisibility(View.GONE);
                }
                else {
                    emptyView.setVisibility(View.GONE);
                    BaseRecyclerView.this.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    public BaseRecyclerView(Context context) {
        super(context);
    }

    public BaseRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);

        if(adapter != null) {
            adapter.registerAdapterDataObserver(emptyObserver);
        }

        emptyObserver.onChanged();
    }

    public void setEmptyView(Context context, View emptyView, String text) {
        this.emptyView = emptyView;
        ((TextView) emptyView).setText(text);

        MaterialDrawableBuilder builder = MaterialDrawableBuilder.with(context)
                .setIcon(MaterialDrawableBuilder.IconValue.MUSIC_NOTE)
                .setColor(Config.textColorPrimary(context, Helpers.getATEKey(context)))
                .setSizeDp(30);

        ((TextView) emptyView).setCompoundDrawables(null, builder.build(), null, null);
    }
}
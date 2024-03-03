package multi.string.screencast.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.multimedia.lib.screening.bean.DeviceInfo;

import org.fourthline.cling.model.meta.Device;

import multi.string.screencast.R;

public class DevicesAdapter extends ArrayAdapter<DeviceInfo> {
    private LayoutInflater mInflater;
    private OnItemClickListen onItemClickListen=null;
    private Context mContext = null;

    public DevicesAdapter(Context context, OnItemClickListen listen) {
        super(context, 0);
        mInflater = LayoutInflater.from(context);
        onItemClickListen = listen;
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = mInflater.inflate(R.layout.item_device, parent, false);

        DeviceInfo item = getItem(position);
        if (item == null) {
            return convertView;
        }

        Device device = item.getDevice();
        ImageView imageView = convertView.findViewById(R.id.listview_item_image);
        imageView.setBackgroundResource(R.drawable.ic_action_dock);

        TextView textView = convertView.findViewById(R.id.listview_item_line_one);
        textView.setText(device.getDetails().getFriendlyName());

        Button statBtn = convertView.findViewById(R.id.device_status_bt);
        if (item.getRealState() == 0) {  //未连接状态
            statBtn.setText(mContext.getString(R.string.btn_connect));
            statBtn.setBackgroundColor(Color.parseColor("#FF9CE86B"));
            statBtn.setClickable(true);
        } else if (item.getRealState() == 1) { //连接状态
            statBtn.setText(mContext.getString(R.string.btn_disconnect));
            statBtn.setBackgroundColor(Color.parseColor("#FFF6E866"));
            statBtn.setClickable(true);
        } else { //失效状态
            statBtn.setText(mContext.getString(R.string.btn_connect));
            statBtn.setBackgroundColor(Color.parseColor("#FF454544"));
            statBtn.setClickable(false);
        }

        final int pos = position;
        statBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListen != null) {
                    onItemClickListen.onClickItem(pos);
                }
            }
        });
        return convertView;
    }


    public interface OnItemClickListen{
        void onClickItem(int pos);
    }
}

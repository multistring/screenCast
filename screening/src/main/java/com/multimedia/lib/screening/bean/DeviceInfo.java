package com.multimedia.lib.screening.bean;

import android.graphics.Color;
import android.util.Log;
import android.widget.Button;

import com.multimedia.lib.screening.DLNAPlayer;

import org.fourthline.cling.model.meta.Device;

import java.io.Serializable;

/**
 * Description：设备信息
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-09
 */
public class DeviceInfo implements Serializable {
    private Device device;
    private String name;
    private String mediaID;
    private String oldMediaID;
    private int state = DLNAPlayer.UNKNOWN;
    private boolean connected;

    private int realState = 0; // 0 断开状态  1 连接状态 2

    //显示相关
    private Button pro_button = null;
    public DeviceInfo(Device device, String name) {
        this.device = device;
        this.name = name;
    }

    public DeviceInfo() {
    }

    public Device getDevice() {
        return this.device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMediaID(String mediaId) {
        this.mediaID = mediaId;
    }

    public String getMediaID() {
        return this.mediaID;
    }

    public void setOldMediaID(String oldMediaID) {
        this.oldMediaID = oldMediaID;
    }

    public String getOldMediaID() {
        return this.oldMediaID;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public Button getPro_button() {
        return pro_button;
    }

    public void setPro_button(Button pro_button) {
        this.pro_button = pro_button;
    }

    public int getRealState() {
        return realState;
    }

    public void setRealState(int realState) {
        this.realState = realState;
    }

    public void proc_bt(int state){
        if (state == 0) { //断开状态
            if (pro_button != null) {
                pro_button.post(new Runnable() {
                    public void run() {
                        pro_button.setText("connect");
                        pro_button.setBackgroundColor(Color.parseColor("#FF9CE86B"));
                        pro_button.setClickable(true);
                    }
                });
            }
        } else if(state == 1){ //连接状态
            if (pro_button != null) {
                pro_button.post(new Runnable() {
                    public void run() {
                        pro_button.setText("disconnect");
                        pro_button.setBackgroundColor(Color.parseColor("#FFF6E866"));
                        pro_button.setClickable(true);
                    }
                });
            }
        } else if (state == 2){ //失效状态
            if (pro_button != null) {
                pro_button.post(new Runnable() {
                    public void run() {
                        pro_button.setText("connect");
                        pro_button.setBackgroundColor(Color.parseColor("#FF454544"));
                        pro_button.setClickable(false);
                    }
                });
            }
        }
    }
}

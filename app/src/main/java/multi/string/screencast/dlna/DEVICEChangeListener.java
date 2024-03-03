package multi.string.screencast.dlna;

import com.multimedia.lib.screening.bean.DeviceInfo;

import java.util.List;

public interface DEVICEChangeListener {
    public void onDeviceChanged(List<DeviceInfo> deviceInfoList);
}

package multi.string.screencast.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class WifiStateUtils {
    /**
     * 检查wifi是否处开连接状态
     * @return
     */
    public static NetworkInfo WifiInfo(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifiInfo;
    }

}

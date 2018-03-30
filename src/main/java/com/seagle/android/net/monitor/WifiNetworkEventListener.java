package com.seagle.android.net.monitor;

import android.net.wifi.WifiInfo;

/**
 * <h1>WIFI网络状态事件</h1>
 * <p>打开，关闭，切换</P>
 *
 * @author : xiudong.yuan@midea.com.cn
 * @date : 2016/5/18
 */
public interface WifiNetworkEventListener {
    /**
     * 网络断开
     */
    void onWifiDisconnected(WifiInfo wifiInfo);

    /**
     * WIFI连接
     */
    void onWifiConnected(WifiInfo wifiInfo);
}

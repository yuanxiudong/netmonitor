package com.seagle.android.net.monitor;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

/**
 * Wifi monitor.
 * Created by seagle on 2018/4/23.
 */

public class WifiNetworkMonitor extends NetworkMonitor.NetStateMachine {
    private static final String TAG = "WifiNetworkMonitor";
    /**
     * WiFi network connected.
     * It has extras {@link #EXTRA_NETWORK_INFO} and {@link #EXTRA_WIFI_INFO}.
     */
    public static final String ACTION_WIFI_NETWORK_CONNECTED = "com.seagle.android.net.monitor.ACTION_WIFI_NETWORK_CONNECTED";

    /**
     * WiFi network disconnected.
     * It has previous network info extras {@link #EXTRA_NETWORK_INFO} and {@link #EXTRA_WIFI_INFO}.
     */
    public static final String ACTION_WIFI_NETWORK_DISCONNECTED = "com.seagle.android.net.monitor.ACTION_WIFI_NETWORK_DISCONNECTED";

    /**
     * Network info extras.
     */
    public static final String EXTRA_NETWORK_INFO = "networkInfo";

    /**
     * WiFi info extras.
     */
    public static final String EXTRA_WIFI_INFO = "wifiInfo";

    private volatile WifiInfo mWifiInfo;
    private WifiManager mWifiManager;

    public WifiNetworkMonitor(Context context) {
        super(context);
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    protected void notifyWifiState(boolean connected, NetworkInfo networkInfo) {
        if (connected) {
            mNetworkInfo = networkInfo;
            mWifiInfo = mWifiManager.getConnectionInfo();
            Intent intent = new Intent(ACTION_WIFI_NETWORK_CONNECTED);
            intent.putExtra(EXTRA_NETWORK_INFO, networkInfo);
            intent.putExtra(EXTRA_WIFI_INFO, mWifiInfo);
            mContext.sendStickyBroadcast(intent);
            Log.i(TAG, "Ethernet network connected: " + mNetworkInfo);
        } else {
            Log.i(TAG, "Ethernet network disconnected: " + mNetworkInfo);
            Intent intent = new Intent(ACTION_WIFI_NETWORK_DISCONNECTED);
            if (mNetworkInfo != null) {
                intent.putExtra(EXTRA_NETWORK_INFO, mNetworkInfo);
                intent.putExtra(EXTRA_WIFI_INFO, mWifiInfo);
            }
            mContext.sendStickyBroadcast(intent);
            mNetworkInfo = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected NetworkRequest getNetRequest() {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        return builder.build();
    }
}

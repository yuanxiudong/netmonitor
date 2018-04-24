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

import static com.seagle.android.net.monitor.NetworkMonitor.EXTRA_NETWORK_INFO;
import static com.seagle.android.net.monitor.NetworkMonitor.EXTRA_NETWORK_STATE;
import static com.seagle.android.net.monitor.NetworkMonitor.EXTRA_PRE_NETWORK_INFO;
import static com.seagle.android.net.monitor.NetworkMonitor.STATE_DISCONNECTED;
import static com.seagle.android.net.monitor.NetworkMonitor.STATE_CONNECTED;

/**
 * Wifi monitor.
 * Created by seagle on 2018/4/23.
 */

public class WifiNetworkMonitor extends NetworkMonitor.NetStateMachine {

    private static final String TAG = "WifiNetworkMonitor";

    /**
     * WiFi network connected.
     * App can get the broadcast event wifi network state by extra {@link NetworkMonitor#EXTRA_NETWORK_STATE},if has two state value:<br>
     * {@link NetworkMonitor#STATE_DISCONNECTED} means current wifi network is disconnect,
     * if previous state is connected and app can get previous network info by extra {@link NetworkMonitor#EXTRA_PRE_NETWORK_INFO},may be null<br>
     * {@link NetworkMonitor#STATE_CONNECTED} means current wifi network is connected,app can get current wifi network
     * info by extra {@link NetworkMonitor#EXTRA_NETWORK_INFO} and get wifi info by {@link #EXTRA_WIFI_INFO}<br>
     */
    public static final String ACTION_WIFI_STATE_CHANGED = "com.seagle.android.net.monitor.ACTION_WIFI_STATE_CONNECTED";

    /**
     * WiFi info extras.
     */
    public static final String EXTRA_WIFI_INFO = "wifiInfo";

    private volatile WifiInfo mWifiInfo;
    private WifiManager mWifiManager;

    WifiNetworkMonitor(Context context) {
        super(context);
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    protected void notifyWifiState(boolean connected, NetworkInfo networkInfo) {
        if (connected) {
            mNetworkInfo = networkInfo;
            mWifiInfo = mWifiManager.getConnectionInfo();
            Intent broadCastIntent = new Intent(ACTION_WIFI_STATE_CHANGED);
            broadCastIntent.putExtra(EXTRA_NETWORK_STATE, STATE_CONNECTED);
            broadCastIntent.putExtra(EXTRA_NETWORK_INFO, mNetworkInfo);
            broadCastIntent.putExtra(EXTRA_WIFI_INFO, mWifiInfo);
            mContext.sendStickyBroadcast(broadCastIntent);
            Log.i(TAG, "Ethernet network connected: " + mNetworkInfo);
        } else {
            Log.i(TAG, "Ethernet network disconnected: " + mNetworkInfo);
            Intent broadCastIntent = new Intent(ACTION_WIFI_STATE_CHANGED);
            broadCastIntent.putExtra(EXTRA_NETWORK_STATE, STATE_DISCONNECTED);
            if (mNetworkInfo != null) {
                broadCastIntent.putExtra(EXTRA_PRE_NETWORK_INFO, mNetworkInfo);
                broadCastIntent.putExtra(EXTRA_WIFI_INFO, mWifiInfo);
            }
            mContext.sendStickyBroadcast(broadCastIntent);
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

    @Override
    void stop() {
        if (mContext != null) {
            Intent broadCastIntent = new Intent(ACTION_WIFI_STATE_CHANGED);
            mContext.removeStickyBroadcast(broadCastIntent);
        }
        super.stop();
    }
}

package com.seagle.android.net.monitor;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

/**
 * Ethernet monitor.
 * Created by seagle on 2018/4/23.
 */

public class EthernetNetworkMonitor extends NetworkMonitor.NetStateMachine {
    private static final String TAG = "EthernetNetworkMonitor";

    /**
     * Ethernet network connected.
     * It has extras {@link #EXTRA_NETWORK_INFO}.
     */
    public static final String ACTION_ETHERNET_NETWORK_CONNECTED = "com.seagle.android.net.monitor.ACTION_ETHERNET_NETWORK_CONNECTED";

    /**
     * Ethernet network disconnected.
     * It has previous network info extras {@link #EXTRA_NETWORK_INFO},
     */
    public static final String ACTION_ETHERNET_NETWORK_DISCONNECTED = "com.seagle.android.net.monitor.ACTION_ETHERNET_NETWORK_DISCONNECTED";

    /**
     * Network info extras.
     */
    public static final String EXTRA_NETWORK_INFO = "networkInfo";

    EthernetNetworkMonitor(Context context) {
        super(context);
    }

    @Override
    protected void notifyWifiState(boolean connected, NetworkInfo networkInfo) {
        if (connected) {
            mNetworkInfo = networkInfo;
            Intent intent = new Intent(ACTION_ETHERNET_NETWORK_CONNECTED);
            intent.putExtra(EXTRA_NETWORK_INFO, networkInfo);
            mContext.sendStickyBroadcast(intent);
            Log.i(TAG, "Ethernet network connected: " + mNetworkInfo);
        } else {
            Log.i(TAG, "Ethernet network disconnected: " + mNetworkInfo);
            Intent intent = new Intent(ACTION_ETHERNET_NETWORK_DISCONNECTED);
            if (mNetworkInfo != null) {
                intent.putExtra(EXTRA_NETWORK_INFO, mNetworkInfo);
            }
            mContext.sendStickyBroadcast(intent);
            mNetworkInfo = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected NetworkRequest getNetRequest() {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
        return builder.build();
    }
}

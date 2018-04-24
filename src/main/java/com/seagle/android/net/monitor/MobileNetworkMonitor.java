package com.seagle.android.net.monitor;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import static com.seagle.android.net.monitor.NetworkMonitor.EXTRA_NETWORK_INFO;
import static com.seagle.android.net.monitor.NetworkMonitor.EXTRA_NETWORK_STATE;
import static com.seagle.android.net.monitor.NetworkMonitor.EXTRA_PRE_NETWORK_INFO;
import static com.seagle.android.net.monitor.NetworkMonitor.STATE_DISCONNECTED;
import static com.seagle.android.net.monitor.NetworkMonitor.STATE_CONNECTED;

/**
 * Mobile monitor.
 * Created by seagle on 2018/4/23.
 */

public class MobileNetworkMonitor extends NetworkMonitor.NetStateMachine {

    private static final String TAG = "MobileNetworkMonitor";

    /**
     * Mobile network state changed.
     * App can get the broadcast event mobile network state by extra {@link NetworkMonitor#EXTRA_NETWORK_STATE},if has two state value:<br>
     * {@link NetworkMonitor#STATE_DISCONNECTED} means current mobile network is disconnect,
     * if previous state is connected and app can get previous network info by extra {@link NetworkMonitor#EXTRA_PRE_NETWORK_INFO},may be null<br>
     * {@link NetworkMonitor#STATE_CONNECTED} means current mobile network is connected,app can get current mobile network
     * info by extra {@link NetworkMonitor#EXTRA_NETWORK_INFO}<br>
     */
    public static final String ACTION_MOBILE_STATE_CHANGED = "com.seagle.android.net.monitor.ACTION_MOBILE_STATE_CONNECTED";


    MobileNetworkMonitor(Context context) {
        super(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected NetworkRequest getNetRequest() {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        return builder.build();
    }

    protected void notifyWifiState(boolean connected, NetworkInfo networkInfo) {
        if (connected) {
            mNetworkInfo = networkInfo;
            Intent broadCastIntent = new Intent(ACTION_MOBILE_STATE_CHANGED);
            broadCastIntent.putExtra(EXTRA_NETWORK_STATE, STATE_CONNECTED);
            broadCastIntent.putExtra(EXTRA_NETWORK_INFO, mNetworkInfo);
            mContext.sendStickyBroadcast(broadCastIntent);
            Log.i(TAG, "Ethernet network connected: " + mNetworkInfo);
        } else {
            Log.i(TAG, "Ethernet network disconnected: " + mNetworkInfo);
            Intent broadCastIntent = new Intent(ACTION_MOBILE_STATE_CHANGED);
            broadCastIntent.putExtra(EXTRA_NETWORK_STATE, STATE_DISCONNECTED);
            if (mNetworkInfo != null) {
                broadCastIntent.putExtra(EXTRA_PRE_NETWORK_INFO, mNetworkInfo);
            }
            mContext.sendStickyBroadcast(broadCastIntent);
            mNetworkInfo = null;
        }
    }

    @Override
    void stop(){
        if(mContext != null) {
            Intent broadCastIntent = new Intent(ACTION_MOBILE_STATE_CHANGED);
            mContext.removeStickyBroadcast(broadCastIntent);
        }
        super.stop();
    }
}

package com.seagle.android.net.monitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 * The parent class of network state monitor.
 * <p>Created by seagle on 2018/4/23.
 *
 * @author yuanxiudong66@sina.com
 * @since 2018-4-23
 */
abstract class NetStateMachine {
    volatile NetworkInfo mNetworkInfo;
    volatile Network mNetwork;
    private ConnectivityManager.NetworkCallback mNetCallback;
    private ConnectivityManager mConnectivityManager;
    Context mContext;

    NetStateMachine(Context context) {
        mContext = context;
        mConnectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    void setNetworkInfo(NetworkInfo networkInfo) {
        mNetworkInfo = networkInfo;
    }

    /**
     * Start network state monitor
     */
    void start() {
        if (mNetworkInfo == null || mNetworkInfo.getState() != NetworkInfo.State.CONNECTED) {
            notifyNetworkState(false, null);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mNetCallback = new ConnectivityManager.NetworkCallback() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onAvailable(Network network) {
                    mNetwork = network;
                    if (network != null) {
                        NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
                        notifyNetworkState(true, networkInfo);
                    }
                }

                @Override
                public void onLost(Network network) {
                    mNetwork = network;
                    notifyNetworkState(false, null);
                }
            };
            mConnectivityManager.registerNetworkCallback(getNetRequest(), mNetCallback);
        }
    }

    /**
     * Stop network state monitor.
     * Do some thing release.
     */
    void stop() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mConnectivityManager.unregisterNetworkCallback(mNetCallback);
        }
        mContext = null;
        mNetworkInfo = null;
        mNetwork = null;
    }

    /**
     * Return is connected.
     *
     * @return network is connected
     */
    public boolean isConnected() {
        NetworkInfo networkInfo = mNetworkInfo;
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Return network info or null.
     *
     * @return NetworkInfo
     */
    public NetworkInfo getNetworkInfo() {
        return mNetworkInfo;
    }

    /**
     * Return network.
     * If wifi not connected or below android LOLLIPOP version return null.
     *
     * @return Network
     */
    public Network getNetwork() {
        return mNetwork;
    }

    /**
     * Return NetworkRequest.
     *
     * @return NetworkRequest
     */
    protected abstract NetworkRequest getNetRequest();

    /**
     * Notify the network state changed.
     *
     * @param connected   Is network connected
     * @param networkInfo NetworkInfo
     */
    protected abstract void notifyNetworkState(boolean connected, NetworkInfo networkInfo);
}

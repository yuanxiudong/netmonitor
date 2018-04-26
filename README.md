# SENetMonitor

### 简介
本工具主要提供监听系统网络状态以及连接WIFI网络等功能。在很多Android设备
中可能存在有线，无线（WiFi），移动(4G/3G)等网络类型，系统提供了监听网络
切换的广播，但是仅能针对系统当前网络进行监听，在Android 6.0 以上的版本系
统允许同时存在多个网络连接，SENetworkMonitor库提供的功能如下：
1. 监听有线网络状态。
2. 监听无线网络状态。
3. 监听移动网络状态。
4. 连接WiFi热点。

![类图](http://blog-image-1253454865.cosgz.myqcloud.com/networkmonitor_class.png)

### 权限配置
1. 基本权限要求
```
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
    <uses-permission android:name="android.permission.BROADCAST_STICKY"/>
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```
2. Android 6.0+的系统还必须打开GPS（部分手机显示的是位置信息）。
3. 如果使用WiFiNetworkMonitor功能，还需要动态申请以下权限：
```
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_WIFI_STATE,
```

### 启动网络监控
NetworkMonitor 是整个库对外访问的入口，采用单例模式实现，一般在App的
Application的onCreate里面启动对系统网络状态的监控。调用接口：
```
NetworkMonitor#startMonitoring(Context context)
```
在不需要监控系统网络状态的时候，调用停止监控接口释放资源：
```
NetworkMonitor#stopMonitoring()
```
如果不启动系统网络状态监控，很多功能将无法使用。

### 监听系统当前网络连接状态
监听当前网络连接状态，主要是监听当前网络连接状态的改变，包括：
1. 网络从连接状态变成断开状态。
2. 网络从断开状态变成连接状态。
3. 网络在WiFi网络，移动网络，有线网络等不同网络类型之间的切换，例如从移动网络切换到WiFi网络。

NetworkMonitor提供了对这三种事件的广播通知：
1. ACTION_NETWORK_STATE_CHANGED：网络连接状态变化广播
- 收到此广播代表系统网络连接情况发生了变化，可以从广播的Intent中得到网络连接状态以及网络信息。
```
    intent#getBooleanExtra(EXTRA_NETWORK_STATE},false)
```
- 如果是网络断开，还可以获取到之前连接的网络信息。
```
    intent#getParcelableExtra(EXTRA_PRE_NETWORK_INFO)
```
- 如果是网络连接，还可以获取到现在连接的网络信息。
```
    intent#getParcelableExtra(EXTRA_NETWORK_STATE)
```
其它主动获取当前网络连接状态及当前网络信息的接口，参见API。

2. ACTION_NETWORK_TYPE_CHANGED：网络类型切换广播
- 该广播代表系统网络的连接类型发生了变化，可以通过Intent获取到前后两个网络的信息。
```
   intent#getParcelableExtra(EXTRA_PRE_NETWORK_INFO)
   intent#getParcelableExtra(EXTRA_NETWORK_STATE)
```
- 注意：该广播会伴随发生网络连接的广播通知。

### 获取各个网络监控类
WiFiNetworkMonitor/MobileNetworkMonitor/EthernetNetworkMonitor等具体某个
网络的监控类都需要通过NetworkMonitor进行获取，并且一般在启动网络监控以后才能获取到。
```
//获取WiFi监控器对象
NetworkMonitor#getWiFiNetworkMonitor();
//获取移动网络监控器对象
NetworkMonitor#getMobileNetworkMonitor();
//获取有线监控器对象
NetworkMonitor#getEthernetNetworkMonitor();
```
### 监控WiFi连接状态
WiFiNetworkMonitor提供监听其连接状态的广播：
- ACTION_WIFI_STATE_CHANGED
Intent中的数据和前面系统网络连接状态广播一样，可以获取到之前连接的和当前连接的WiFi信息和网络信息。

### 连接WiF热点
WiFiNetworkMonitor提供了开启/关闭 WiFi的接口，以及获取当前WiFi连接状态的接口。
```
WiFiNetworkMonitor#enableWiFi();
WiFiNetworkMonitor#disableWiFi();
WiFiNetworkMonitor#isWiFiEnabled();
WiFiNetworkMonitor#getWiFiInfo();
WiFiNetworkMonitor#isConnected();
WiFiNetworkMonitor#getNetworkInfo();
WiFiNetworkMonitor#getNetwork();
```
其中getNetwork接口在多网络环境下会比较有用，可以让APP指定网络数据通道走WiFi网络。
WiFiNetworkMonitor提供连接WiFi热点的接口：

```
connectWiFi(ScanResult result, String password, final WiFiConnectCallback callback);
connectWiFi(String ssid, String capabilities, String password, final WiFiConnectCallback callback);
```
这两个接口支持同步和异步两种请求方式，如果传入callback就是异步请求，不传就是同步请求。
同步请求的情况下，将会阻塞调用线程，通过返回的错误码判断是否连接成功。异步请求的错误码会通过
回调返回。

### 监控移动网络连接状态
MobileNetworkMonitor提供监听其连接状态的广播：
- ACTION_MOBILE_STATE_CHANGED
Intent中的数据和前面系统网络连接状态广播一样，可以获取到当前连接的移动网络信息。

### 监控有线网络连接状态
EthernetNetworkMonitor提供监听其连接状态的广播：
- ACTION_ETHERNET_STATE_CHANGED
Intent中的数据和前面系统网络连接状态广播一样，可以获取到当前连接的有线网络信息。
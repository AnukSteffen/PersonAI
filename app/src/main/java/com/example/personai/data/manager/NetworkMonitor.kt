package com.example.personai.data.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor( //怎么得到网络状态呢？模拟发送一个标准网络请求后通过回调放回状态即可
    @ApplicationContext private val context: Context
) {
    // 实时网络状态流：true=有网, false=无网
    val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() { //1.创建回调
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder() //2.网络请求作为模子
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) //只检测具有互联网访问能力的网络，而不是仅仅连接到局域网的网络
            .build()

        connectivityManager.registerNetworkCallback(request, callback) //3.注册监听

        //3.初始状态检查
        val activeNetwork = connectivityManager.activeNetwork

        //4.状态更新
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(isConnected)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged() //去重处理，确保只有状态变化时才发射事件
}
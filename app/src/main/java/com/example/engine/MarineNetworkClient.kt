package com.example.engine

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Uygulama genelinde paylaşılan yüksek hızlı ve kararlı HTTP bağlantı havuzu.
 * Her ağ çağrısında yeni soket/TLS el sıkışması yapılmasını önleyerek gecikmeyi ve bellek tüketimini düşürür.
 */
object MarineNetworkClient {

  private val sharedConnectionPool = ConnectionPool(
    maxIdleConnections = 8,
    keepAliveDuration = 5,
    timeUnit = TimeUnit.MINUTES
  )

  private val sharedDispatcher = Dispatcher(
    Executors.newCachedThreadPool { runnable ->
      Thread(runnable, "MarineNetPool").apply { isDaemon = true }
    }
  ).apply {
    maxRequests = 24
    maxRequestsPerHost = 8
  }

  /** EMODnet Bathymetry ve Open-Meteo gibi hızlı telemetri servisleri için optimize istemci */
  val fastApiClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectionPool(sharedConnectionPool)
      .dispatcher(sharedDispatcher)
      .connectTimeout(5, TimeUnit.SECONDS)
      .readTimeout(6, TimeUnit.SECONDS)
      .writeTimeout(5, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()
  }

  /** Bellek ve boşta kalan soketleri optimize eder */
  fun evictIdleConnections() {
    try {
      sharedConnectionPool.evictAll()
    } catch (_: Exception) {}
  }
}

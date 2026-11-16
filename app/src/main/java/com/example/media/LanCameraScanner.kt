package com.example.media

import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class DiscoveredCameraHost(
  val ipAddress: String,
  val openPorts: List<Int>,
) {
  val preferredPort: Int
    get() =
      when {
        554 in openPorts -> 554
        8554 in openPorts -> 8554
        8080 in openPorts -> 8080
        else -> openPorts.firstOrNull() ?: 554
      }
}

class LanCameraScanner {
  private val cameraPorts = listOf(554, 8554, 80, 8080, 8000)

  fun detectLocalSubnetPrefix(): String? {
    return runCatching {
        NetworkInterface.getNetworkInterfaces()
          ?.toList()
          ?.filter { it.isUp && !it.isLoopback }
          ?.flatMap { it.inetAddresses.toList() }
          ?.filterIsInstance<Inet4Address>()
          ?.firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
          ?.hostAddress
          ?.substringBeforeLast('.')
      }
      .getOrNull()
  }

  suspend fun scanSubnet(subnetPrefix: String? = detectLocalSubnetPrefix()): List<DiscoveredCameraHost> =
    withContext(Dispatchers.IO) {
      val prefix = subnetPrefix ?: return@withContext emptyList()
      val semaphore = Semaphore(36)
      coroutineScope {
        (1..254)
          .map { host ->
            async {
              semaphore.withPermit {
                val ip = "$prefix.$host"
                val open = mutableListOf<Int>()
                for (port in cameraPorts) {
                  if (isPortOpen(ip, port, timeoutMs = 260)) {
                    open.add(port)
                  }
                }
                if (open.isNotEmpty()) DiscoveredCameraHost(ip, open) else null
              }
            }
          }
          .awaitAll()
          .filterNotNull()
      }
    }

  private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
    return try {
      Socket().use { socket ->
        socket.connect(InetSocketAddress(ip, port), timeoutMs)
        true
      }
    } catch (_: Throwable) {
      false
    }
  }
}

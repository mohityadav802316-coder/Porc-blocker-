package com.example.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-Performance, 100% On-Device Local DNS Filter VPN Service.
 *
 * Intercepts DNS queries on-device to block adult domains across all applications and browsers
 * (Brave, Chrome, Firefox, Samsung, Opera, etc.) at the network resolution level.
 *
 * 1. Zero External Servers: All filtering logic executes locally on the device.
 * 2. Instant Strict Lock Activation: When a blocked domain query is detected, instantly engages
 *    15-minute Strict Lock.
 * 3. Minimal Overhead: Only routes DNS traffic (UDP port 53), preserving battery and normal speed.
 */
class LocalDnsVpnService : VpnService() {

  companion object {
    private const val TAG = "LocalDnsVpnService"
    const val CHANNEL_ID = "life_tracker_dns_vpn_channel"
    const val NOTIFICATION_ID = 9015
    const val ACTION_START = "com.example.focus.ACTION_START_DNS_VPN"
    const val ACTION_STOP = "com.example.focus.ACTION_STOP_DNS_VPN"

    private const val UPSTREAM_DNS_PRIMARY = "1.1.1.1"
    private const val UPSTREAM_DNS_SECONDARY = "8.8.8.8"

    private val _isVpnRunning = MutableStateFlow(false)
    val isVpnRunning: StateFlow<Boolean> = _isVpnRunning.asStateFlow()

    fun isPrepared(context: Context): Boolean {
      return prepare(context) == null
    }

    fun start(context: Context) {
      val intent = Intent(context, LocalDnsVpnService::class.java).apply {
        action = ACTION_START
      }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to start LocalDnsVpnService", e)
      }
    }

    fun stop(context: Context, pinVerified: Boolean = false): Boolean {
      if (FocusModeManager.isStrictLockActive(context)) {
        Log.w(TAG, "Cannot stop DNS VPN: Strict Lock is ACTIVE! Stop rejected.")
        return false
      }
      if (!pinVerified && !AlwaysOnSecurityManager.isTemporarilyUnlocked(context) && !ProtectionPauseManager.isPaused(context)) {
        if (AccountabilityPinManager.isPinSet(context)) {
          Log.w(TAG, "Cannot stop DNS VPN: PIN verification or 24-hr unlock required!")
          return false
        }
      }
      val intent = Intent(context, LocalDnsVpnService::class.java).apply {
        action = ACTION_STOP
      }
      try {
        context.stopService(intent)
        return true
      } catch (e: Exception) {
        Log.e(TAG, "Failed to stop LocalDnsVpnService", e)
        return false
      }
    }
  }

  private var vpnInterface: ParcelFileDescriptor? = null
  private val isRunning = AtomicBoolean(false)
  private var vpnThread: Thread? = null
  private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      if (FocusModeManager.isStrictLockActive(this)) {
        Log.w(TAG, "Stop command received but Strict Lock is active! Keeping VPN running.")
        return START_STICKY
      }
      stopVpn()
      return START_NOT_STICKY
    }

    startForegroundNotification()
    startVpn()

    return START_STICKY
  }

  private fun startForegroundNotification() {
    val notification = buildNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun buildNotification(): Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val contentPending = PendingIntent.getActivity(this, 9015, openIntent, pendingFlags)

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("🛡️ ऑन-डिवाइस DNS फ़िल्टर सक्रिय")
      .setContentText("सभी ब्राउज़रों में ख़राब डोमेन डिवाइस पर ही रोके जा रहे हैं")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("100% ऑफ़लाइन DNS सुरक्षा: Brave, Chrome, Firefox, Samsung, Opera सहित सभी ऐप्स में अनुपयुक्त डोमेन स्वतः अवरोधित होते हैं।")
      )
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setAutoCancel(false)
      .setContentIntent(contentPending)
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (nm.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          "DNS Filter Protection",
          NotificationManager.IMPORTANCE_LOW
        ).apply {
          description = "Displays status for local on-device DNS adult content filtering"
          setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
      }
    }
  }

  private fun startVpn() {
    if (isRunning.get()) return

    try {
      val builder = Builder()
        .setSession("LifeTrackerLocalDns")
        .addAddress("10.200.0.2", 30)
        .addDnsServer(UPSTREAM_DNS_PRIMARY)
        .addDnsServer(UPSTREAM_DNS_SECONDARY)
        // Route only DNS servers to capture port 53 traffic without affecting entire internet pipe
        .addRoute(UPSTREAM_DNS_PRIMARY, 32)
        .addRoute(UPSTREAM_DNS_SECONDARY, 32)
        .setBlocking(true)

      vpnInterface = builder.establish()
      if (vpnInterface == null) {
        Log.e(TAG, "Failed to establish VPN interface (null)")
        return
      }

      isRunning.set(true)
      _isVpnRunning.value = true

      vpnThread = Thread { runVpnLoop() }.apply {
        name = "LocalDnsVpnThread"
        start()
      }

      Log.i(TAG, "Local DNS Filter VPN established successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Error starting Local DNS VPN", e)
      stopVpn()
    }
  }

  private fun runVpnLoop() {
    val pfd = vpnInterface ?: return
    val inStream = FileInputStream(pfd.fileDescriptor)
    val outStream = FileOutputStream(pfd.fileDescriptor)

    val packetBuffer = ByteBuffer.allocate(32767)
    val upstreamSocket = DatagramSocket()
    protect(upstreamSocket)
    upstreamSocket.soTimeout = 3000

    val primaryDns = InetAddress.getByName(UPSTREAM_DNS_PRIMARY)

    try {
      while (isRunning.get()) {
        packetBuffer.clear()
        val readLength = inStream.read(packetBuffer.array())
        if (readLength <= 0) continue

        packetBuffer.limit(readLength)

        // Parse IPv4 packet
        if (readLength < 28) continue
        val versionAndIHL = packetBuffer.get(0).toInt() and 0xFF
        val version = versionAndIHL shr 4
        if (version != 4) continue // IPv4 only

        val ihl = (versionAndIHL and 0x0F) * 4
        val protocol = packetBuffer.get(9).toInt() and 0xFF
        if (protocol != 17) continue // UDP only

        // UDP Header
        val srcPort = ((packetBuffer.get(ihl).toInt() and 0xFF) shl 8) or (packetBuffer.get(ihl + 1).toInt() and 0xFF)
        val dstPort = ((packetBuffer.get(ihl + 2).toInt() and 0xFF) shl 8) or (packetBuffer.get(ihl + 3).toInt() and 0xFF)

        // DNS is on port 53
        if (dstPort == 53) {
          val dnsOffset = ihl + 8
          val dnsLength = readLength - dnsOffset
          if (dnsLength > 12) {
            val qname = parseDnsQuestionName(packetBuffer.array(), dnsOffset + 12, readLength)
            if (!qname.isNullOrBlank()) {
              val isBlocked = AdultBlocklistManager.isDomainBlocked(this, qname)
              if (isBlocked) {
                Log.w(TAG, "🚨 DNS BLOCK: Attempted query for adult domain: '$qname'! Triggering Strict Lock.")

                // Record statistics: Detected Attempt + Blocked Attempt + Strict Lock
                PeeweeStatsManager.recordAttempt(this@LocalDnsVpnService, successfullyBlocked = true, trigger = qname)
                PeeweeStatsManager.recordStrictLockActivation(this@LocalDnsVpnService)

                // 1. Immediately engage 15-minute Strict Lock!
                serviceScope.launch(Dispatchers.Main) {
                  FocusModeManager.activateStrictLock(
                    context = this@LocalDnsVpnService,
                    durationMinutes = 15,
                    reason = "वयस्क डोमेन अवरोधित (DNS): $qname",
                    triggerWord = qname,
                    packageName = "DNS-Filter"
                  )
                  FocusBlockingOverlayManager.showStrictLock(this@LocalDnsVpnService, "DNS-Filter", qname)
                }

                // 2. Synthesize NXDOMAIN DNS response back to client so browser connection fails instantly
                val blockedResponse = createNxDomainResponse(packetBuffer.array(), ihl, readLength)
                outStream.write(blockedResponse)
                outStream.flush()
                continue
              }
            }
          }

          // Safe domain: Forward to upstream DNS server
          try {
            val udpPayload = ByteArray(readLength - ihl - 8)
            System.arraycopy(packetBuffer.array(), ihl + 8, udpPayload, 0, udpPayload.size)

            val outPacket = DatagramPacket(udpPayload, udpPayload.size, primaryDns, 53)
            upstreamSocket.send(outPacket)

            val inBuffer = ByteArray(2048)
            val inPacket = DatagramPacket(inBuffer, inBuffer.size)
            upstreamSocket.receive(inPacket)

            val responseIpPacket = buildUdpIpPacket(
              srcIp = packetBuffer.array(),
              srcIpOffset = 16, // Original destination is now source
              dstIpOffset = 12, // Original source is now destination
              srcPort = dstPort,
              dstPort = srcPort,
              payload = inBuffer,
              payloadLength = inPacket.length
            )
            outStream.write(responseIpPacket)
            outStream.flush()
          } catch (e: Exception) {
            // DNS resolution timeout or transient network drop
          }
        }
      }
    } catch (e: Exception) {
      if (isRunning.get()) {
        Log.e(TAG, "Error in VPN loop", e)
      }
    } finally {
      upstreamSocket.close()
    }
  }

  private fun parseDnsQuestionName(bytes: ByteArray, offset: Int, maxLen: Int): String? {
    var pos = offset
    val sb = StringBuilder()

    while (pos < maxLen) {
      val len = bytes[pos].toInt() and 0xFF
      if (len == 0) break
      if ((len and 0xC0) == 0xC0) break // Pointer
      pos++

      if (pos + len > maxLen) return null
      if (sb.isNotEmpty()) sb.append(".")
      for (i in 0 until len) {
        sb.append(bytes[pos + i].toInt().toChar())
      }
      pos += len
    }

    return sb.toString().lowercase()
  }

  private fun createNxDomainResponse(packet: ByteArray, ihl: Int, packetLen: Int): ByteArray {
    val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
    val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)

    val dnsOffset = ihl + 8
    val dnsLen = packetLen - dnsOffset
    val dnsPayload = ByteArray(dnsLen)
    System.arraycopy(packet, dnsOffset, dnsPayload, 0, dnsLen)

    // Set QR=1 (Response), RCODE=3 (NXDOMAIN)
    if (dnsPayload.size >= 4) {
      dnsPayload[2] = (dnsPayload[2].toInt() or 0x80).toByte() // Response
      dnsPayload[3] = ((dnsPayload[3].toInt() and 0xF0) or 0x03).toByte() // NXDOMAIN
    }

    return buildUdpIpPacket(
      srcIp = packet,
      srcIpOffset = 16,
      dstIpOffset = 12,
      srcPort = dstPort,
      dstPort = srcPort,
      payload = dnsPayload,
      payloadLength = dnsPayload.size
    )
  }

  private fun buildUdpIpPacket(
    srcIp: ByteArray,
    srcIpOffset: Int,
    dstIpOffset: Int,
    srcPort: Int,
    dstPort: Int,
    payload: ByteArray,
    payloadLength: Int
  ): ByteArray {
    val totalLen = 20 + 8 + payloadLength
    val ip = ByteArray(totalLen)

    ip[0] = 0x45.toByte() // IPv4, Header length 20
    ip[1] = 0x00.toByte()
    ip[2] = (totalLen shr 8).toByte()
    ip[3] = (totalLen and 0xFF).toByte()
    ip[4] = 0x00.toByte()
    ip[5] = 0x01.toByte()
    ip[6] = 0x40.toByte() // Don't fragment
    ip[7] = 0x00.toByte()
    ip[8] = 64.toByte()   // TTL
    ip[9] = 17.toByte()   // UDP

    // Swap Source and Destination IPs
    System.arraycopy(srcIp, srcIpOffset, ip, 12, 4)
    System.arraycopy(srcIp, dstIpOffset, ip, 16, 4)

    // IP Checksum calculation
    var checksum = 0
    for (i in 0 until 10) {
      if (i == 5) continue // Skip checksum field
      val w = ((ip[i * 2].toInt() and 0xFF) shl 8) or (ip[i * 2 + 1].toInt() and 0xFF)
      checksum += w
    }
    checksum = (checksum shr 16) + (checksum and 0xFFFF)
    checksum += (checksum shr 16)
    val finalIpCheck = checksum.inv() and 0xFFFF
    ip[10] = (finalIpCheck shr 8).toByte()
    ip[11] = (finalIpCheck and 0xFF).toByte()

    // UDP Header
    ip[20] = (srcPort shr 8).toByte()
    ip[21] = (srcPort and 0xFF).toByte()
    ip[22] = (dstPort shr 8).toByte()
    ip[23] = (dstPort and 0xFF).toByte()

    val udpLen = 8 + payloadLength
    ip[24] = (udpLen shr 8).toByte()
    ip[25] = (udpLen and 0xFF).toByte()
    ip[26] = 0.toByte() // Optional UDP Checksum
    ip[27] = 0.toByte()

    // Copy Payload
    System.arraycopy(payload, 0, ip, 28, payloadLength)

    return ip
  }

  private fun stopVpn() {
    isRunning.set(false)
    _isVpnRunning.value = false

    try {
      vpnThread?.interrupt()
      vpnThread = null
    } catch (_: Exception) {}

    try {
      vpnInterface?.close()
      vpnInterface = null
    } catch (_: Exception) {}

    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
    Log.i(TAG, "Local DNS VPN stopped cleanly")
  }

  override fun onRevoke() {
    super.onRevoke()
    Log.w(TAG, "VPN permission revoked by user or another VPN service connected")
    try {
      ProtectionHealthManager.logHealthEvent(
        context = this,
        eventType = "DNS_VPN_REVOKED",
        message = "DNS VPN फ़िल्टर को सिस्टम या दूसरे VPN द्वारा हटा दिया गया!",
        severity = "HIGH"
      )
      // Check if pause is active; if not active, post an urgent alert
      if (!ProtectionPauseManager.isPaused(this) && !AlwaysOnSecurityManager.isTemporarilyUnlocked(this)) {
        StrictLockWatchdog.postAlertNotification(
          context = this,
          title = "⚠️ सुरक्षा चेतावनी: DNS फ़िल्टर बंद हो गया!",
          message = "किसी अन्य VPN या सिस्टम सेटिंग द्वारा Life Tracker का लोकल DNS फ़िल्टर हटा दिया गया है। कृपया सुरक्षा दोबारा चालू करें।",
          intentAction = android.provider.Settings.ACTION_VPN_SETTINGS
        )
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error logging VPN revocation: ${e.message}")
    }
    stopVpn()
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
    stopVpn()
  }
}

package com.example.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PeeweePersistentProtectionTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    PeeweeStatsManager.resetStatistics(context)
  }

  @Test
  fun testMasterProtectionPreferencePersistence() {
    // 1. Defaults to true
    PeeweeProtectionManager.setMasterProtectionEnabled(context, true)
    assertTrue("Master protection should be enabled", PeeweeProtectionManager.isMasterProtectionEnabled(context))

    // 2. Set to false and verify persistence
    PeeweeProtectionManager.setMasterProtectionEnabled(context, false)
    assertFalse("Master protection should be disabled", PeeweeProtectionManager.isMasterProtectionEnabled(context))

    // 3. Re-enable
    PeeweeProtectionManager.setMasterProtectionEnabled(context, true)
    assertTrue("Master protection should be re-enabled", PeeweeProtectionManager.isMasterProtectionEnabled(context))
  }

  @Test
  fun testHonestStatusEvaluationWhenDisabled() {
    PeeweeProtectionManager.setMasterProtectionEnabled(context, false)
    val report = PeeweeProtectionManager.evaluateProtectionState(context)

    assertEquals(ProtectionStatus.PROTECTION_DISABLED, report.status)
    assertFalse(report.isMasterEnabled)
    assertNotNull(report.actionRequired)
    assertTrue(report.actionRequired!!.contains("turned off", ignoreCase = true))
  }

  @Test
  fun testHonestStatusEvaluationWhenPermissionsMissing() {
    PeeweeProtectionManager.setMasterProtectionEnabled(context, true)
    val report = PeeweeProtectionManager.evaluateProtectionState(context)

    // In unit test environment, accessibility service and VPN are not granted
    assertTrue(
      "Expected PERMISSION_REQUIRED or PROTECTION_LIMITED",
      report.status == ProtectionStatus.PERMISSION_REQUIRED || report.status == ProtectionStatus.PROTECTION_LIMITED
    )
    assertTrue(report.isMasterEnabled)
    assertNotNull(report.actionRequired)
  }

  @Test
  fun testBootReceiverOnBootCompletedRestoresProtection() {
    PeeweeProtectionManager.setMasterProtectionEnabled(context, true)
    val receiver = PeeweeBootReceiver()

    val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
    receiver.onReceive(context, bootIntent)

    val report = PeeweeProtectionManager.evaluateProtectionState(context)
    assertTrue(report.isMasterEnabled)
    assertNotNull(report.formattedLastCheck)
  }

  @Test
  fun testBootReceiverRespectsUserDisabledPreference() {
    PeeweeProtectionManager.setMasterProtectionEnabled(context, false)
    val receiver = PeeweeBootReceiver()

    val replaceIntent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED)
    receiver.onReceive(context, replaceIntent)

    val report = PeeweeProtectionManager.evaluateProtectionState(context)
    assertEquals(ProtectionStatus.PROTECTION_DISABLED, report.status)
  }

  @Test
  fun testNotifyProtectionDegradedDispatchesNotification() {
    PeeweeProtectionManager.notifyProtectionDegraded(
      context = context,
      title = "Test Alert",
      message = "Protection service was stopped"
    )

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notifications = notificationManager.activeNotifications
    assertTrue("At least one notification should be posted", notifications.isNotEmpty())
  }

  @Test
  fun testDayStatsCalculationAndPersistence() {
    PeeweeStatsManager.recordAttempt(context, successfullyBlocked = true, trigger = "DNS_QUERY")
    PeeweeStatsManager.recordStrictLockActivation(context)

    val todayStats = PeeweeStatsManager.getTodayStats(context)
    assertEquals(1, todayStats.blockAttempts)
    assertEquals(1, todayStats.strictLockActivations)
    assertEquals(1, todayStats.blockedAttempts)

    // Test weekly aggregation
    val weeklyStats = PeeweeStatsManager.getWeeklyStats(context)
    assertTrue(weeklyStats.totalBlockedAttempts >= 1)
    assertEquals(7, weeklyStats.dailyBreakdown.size)

    // Test monthly aggregation
    val monthlyStats = PeeweeStatsManager.getMonthlyStats(context)
    assertTrue(monthlyStats.totalBlockedAttempts >= 1)

    // Test safe reset
    PeeweeStatsManager.resetStatistics(context)
    val resetStats = PeeweeStatsManager.getTodayStats(context)
    assertEquals(0, resetStats.blockAttempts)
    assertEquals(0, resetStats.strictLockActivations)
    assertEquals(0, resetStats.blockedAttempts)
  }
}

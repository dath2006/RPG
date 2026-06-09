package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ProductivityApplication
import com.example.data.DailyStats
import com.example.data.LedgerEntry
import com.example.data.TransactionCategory
import java.util.Calendar

class DailyAuditWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ProductivityApplication
        val configRepo = app.economyConfigRepository
        val ledgerRepo = app.ledgerRepository
        val statsRepo = app.statsRepository

        val nowMs = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMs
            // We look at yesterday's date
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayEpoch = calendar.timeInMillis / (24 * 60 * 60 * 1000L)

        // Check if user earned any focal points yesterday
        val startOfYesterday = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfYesterday = startOfYesterday + (24 * 60 * 60 * 1000L) - 1

        val earnedYesterday = ledgerRepo.getTotalEarnedSince(startOfYesterday)

        val currentStreakConfig = configRepo.getVal("current_streak", defaultInt = 0)
        val currentStreak = currentStreakConfig.valueInt ?: 0

        val lazyTaxConfig = configRepo.getVal("lazy_tax_paise", defaultLong = 10000L) // ₹100
        val lazyTaxPaise = lazyTaxConfig.valuePaise ?: 10000L

        var newStreak = 0
        var gotTaxed = false

        if (earnedYesterday == 0L) {
            // Did not work yesterday -> Apply Lazy Tax & reset streak
            ledgerRepo.insertDebit(
                amountPaise = lazyTaxPaise,
                category = TransactionCategory.LAZY_TAX,
                subcategory = "Missed work yesterday! 💤"
            )
            configRepo.setInt("current_streak", 0)
            configRepo.setInt("earning_multiplier_bps", 10000) // Reset to 1.0x
            gotTaxed = true
            sendAuditNotification("Lazy Tax Applied! 💤", "You were taxed ₹${lazyTaxPaise / 100} for not studying/focusing yesterday.")
        } else {
            // Worked yesterday -> Increment streak
            newStreak = currentStreak + 1
            configRepo.setInt("current_streak", newStreak)

            // Update streak multiplier (milestones: 3 days -> 1.2x, 7 days -> 1.5x, 14 days -> 1.75x, 30 days -> 2.0x)
            val newMultiplier = when {
                newStreak >= 30 -> 20000 // 2.0x
                newStreak >= 14 -> 17500 // 1.75x
                newStreak >= 7  -> 15000 // 1.5x
                newStreak >= 3  -> 12000 // 1.2x
                else            -> 10000 // 1.0x
            }
            configRepo.setInt("earning_multiplier_bps", newMultiplier)

            // Grant streak milestones bonus if divisible by 7
            if (newStreak % 7 == 0) {
                val bonusPaise = 20000L // ₹200 bonus
                ledgerRepo.insertEarning(
                    amountPaise = bonusPaise,
                    category = TransactionCategory.STREAK_BONUS,
                    subcategory = "$newStreak-Day Streak Milestone! 🔥"
                )
            }
            sendAuditNotification("Streak Maintained! 🔥", "Current streak: $newStreak days. Active Multiplier is now ${newMultiplier / 10000.0}x!")
        }

        // Save entry in global stats
        val totalSpentYesterday = ledgerRepo.getTotalSpentSince(startOfYesterday)
        statsRepo.saveStats(
            DailyStats(
                dateEpochDay = yesterdayEpoch,
                totalEarnedPaise = earnedYesterday,
                totalSpentPaise = totalSpentYesterday,
                streakDayCount = newStreak,
                earningMultiplierBps = configRepo.getInt("earning_multiplier_bps", 10000),
                lazyTaxApplied = gotTaxed
            )
        )

        return Result.success()
    }

    private fun sendAuditNotification(title: String, content: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_audit_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Daily Economy Audit", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(2001, notif)
    }
}

class DistractionDrainWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ProductivityApplication
        val ruleRepo = app.distractionRuleRepository
        val ledgerRepo = app.ledgerRepository

        val rules = ruleRepo.getAllRulesOnce()
        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val nowMs = System.currentTimeMillis()
        val lastFifteenMin = nowMs - (15 * 60 * 1000L)

        var totalDeductions = 0L

        if (usageStatsManager != null) {
            try {
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    lastFifteenMin,
                    nowMs
                )

                if (!stats.isNullOrEmpty()) {
                    for (rule in rules) {
                        val appStat = stats.find { it.packageName == rule.packageName }
                        val forumTimeMs = appStat?.totalTimeInForeground ?: 0L
                        val mins = forumTimeMs / 60_000.0

                        if (mins >= 0.5) {
                            val cost = (rule.costPerMinutePaise * mins).toLong()
                            if (cost > 0) {
                                ledgerRepo.insertDebit(
                                    amountPaise = cost,
                                    category = TransactionCategory.DISTRACTION_DRAIN,
                                    subcategory = "${rule.appDisplayName} (${mins.toInt()} min)"
                                )
                                totalDeductions += cost
                                ruleRepo.incrementSpent(rule.packageName, cost)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // If query usage stats fails (due to no permissions or unsupported environments),
                // we handle it gracefully. No crash.
            }
        }

        return Result.success()
    }
}

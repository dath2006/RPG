package com.example.service

import com.example.data.EconomyConfigRepository
import com.example.data.LedgerRepository

class MercyTokenManager(
    private val configRepo: EconomyConfigRepository,
    private val ledgerRepo: LedgerRepository
) {
    suspend fun getAvailableTokens(): Int = configRepo.getInt("mercy_tokens", 1)

    // Called from UI when user wants to protect today's streak
    suspend fun spendMercyToken(): MercyResult {
        val available = getAvailableTokens()
        if (available <= 0) return MercyResult.NoTokens

        val currentStreak = configRepo.getInt("current_streak", 0)
        if (currentStreak == 0) return MercyResult.NoStreakToProtect

        configRepo.setInt("mercy_tokens", available - 1)
        // Streak is preserved by the DailyAuditWorker checking the mercy flag

        return MercyResult.Success(streakProtected = currentStreak)
    }

    // Tokens refill: 1 per month from WorkManager, + earned from Boss Fights
    suspend fun refillMonthlyToken() {
        val maxTokens = 3  // Never more than 3 in hand
        val current = getAvailableTokens()
        if (current < maxTokens) {
            configRepo.setInt("mercy_tokens", current + 1)
        }
    }

    // Bonus tokens from Boss Fights / Salary Day performance
    suspend fun awardBonusToken(reason: String) {
        val current = getAvailableTokens()
        configRepo.setInt("mercy_tokens", minOf(current + 1, 3))
    }
}

sealed class MercyResult {
    data class Success(val streakProtected: Int) : MercyResult()
    object NoTokens : MercyResult()
    object NoStreakToProtect : MercyResult()
}

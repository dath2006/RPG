package com.example.service

import com.example.data.EconomyConfigRepository

class CreditScoreManager(
    private val configRepo: EconomyConfigRepository
) {
    private val BASE_SCORE = 600

    suspend fun getScore(): Int = configRepo.getInt("credit_score", BASE_SCORE)

    suspend fun getLabel(): String = when (getScore()) {
        in 750..850 -> "Excellent"
        in 650..749 -> "Good"
        in 550..649 -> "Fair"
        in 450..549 -> "Poor"
        else -> "Bad"
    }

    // Events that affect score
    suspend fun onOathCompletedEarly() = adjustScore(+15)
    suspend fun onOathCompletedOnTime() = adjustScore(+10)
    suspend fun onOathDefaulted() = adjustScore(-30)
    suspend fun onStreakMilestone(days: Int) = adjustScore(+days / 7)
    suspend fun onLazyTaxApplied() = adjustScore(-5)
    suspend fun onWeeklyPositiveBalance() = adjustScore(+3)

    private suspend fun adjustScore(delta: Int) {
        val current = getScore()
        val newScore = (current + delta).coerceIn(0, 850)
        configRepo.setInt("credit_score", newScore)
    }
}

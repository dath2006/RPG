package com.example.service

import com.example.data.*
import java.time.LocalDate

class SalaryAndStepService(
    private val ledgerRepo: LedgerRepository,
    private val configRepo: EconomyConfigRepository,
    private val statsRepo: StatsRepository,
    private val creditScoreManager: CreditScoreManager
) {

    suspend fun simulateSalaryDay() {
        val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)

        val weeklyEarned = ledgerRepo.getTotalEarnedSince(weekStart)
        val weeklySpent = ledgerRepo.getTotalSpentSince(weekStart)
        
        // Find distraction cost - approximation for prototype
        val allEntries = ledgerRepo.balanceFlow // This is a bit tricky, but we can just use 0 if we don't have a direct query
        
        val earnSpendRatio = if (weeklySpent > 0) weeklyEarned.toDouble() / weeklySpent else 10.0
        val bonusMultiplier = when {
            earnSpendRatio >= 3.0 -> 1.5
            earnSpendRatio >= 2.0 -> 1.25
            earnSpendRatio >= 1.0 -> 1.0
            else -> 0.5
        }
        val baseBonus = configRepo.getLong("salary_day_base_bonus_paise", 5000L)
        val bonusPaise = (baseBonus * bonusMultiplier).toLong()

        ledgerRepo.insertEarning(bonusPaise, TransactionCategory.SALARY_DAY_BONUS,
            subcategory = "Week Ending Bonus")
            
        if (weeklyEarned > weeklySpent) {
            creditScoreManager.onWeeklyPositiveBalance()
        }
    }

    suspend fun simulateStepIncome(steps: Int) {
        if (steps >= 1000) {
            val incomePerThousand = configRepo.getLong("step_income_per_1000_paise", 1000L)
            val earnedPaise = (steps / 1000) * incomePerThousand
            val cappedEarned = minOf(earnedPaise, 15000L)

            ledgerRepo.insertEarning(
                amountPaise = cappedEarned,
                category = TransactionCategory.STEP_INCOME,
                subcategory = "$steps steps simulated"
            )
        }
    }
}

package com.example.service

import com.example.data.*

class OathService(
    private val oathRepo: OathRepository,
    private val ledgerRepo: LedgerRepository,
    private val configRepo: EconomyConfigRepository,
    private val creditScoreManager: CreditScoreManager
) {
    suspend fun createOath(
        loanAmountPaise: Long,
        taskDescription: String,
        dueDateMs: Long
    ): Oath {
        val currentBalance = ledgerRepo.getBalanceSnapshot()
        require(currentBalance >= 0) { "Cannot take oath while in debt" }

        val interestBps = getCreditAdjustedInterestRate()

        val oath = Oath(
            initialLoanAmountPaise = loanAmountPaise,
            currentDebtAmountPaise = loanAmountPaise,
            taskDescription = taskDescription,
            createdAtMs = System.currentTimeMillis(),
            dueDateMs = dueDateMs,
            dailyInterestRateBps = interestBps,
            status = "ACTIVE"
        )

        oathRepo.createOath(oath)
        // Credit wallet immediately
        ledgerRepo.insertEarning(loanAmountPaise, TransactionCategory.OATH_LOAN, sourceRef = oath.id)
        return oath
    }

    suspend fun applyOvernightInterest() {
        val activeOaths = oathRepo.getActiveOaths()
        val nowMs = System.currentTimeMillis()

        activeOaths.forEach { oath ->
            if (nowMs > oath.dueDateMs) {
                // Overdue: apply compound interest
                val dailyRate = oath.dailyInterestRateBps / 10000.0
                val interest = (oath.currentDebtAmountPaise * dailyRate).toLong()
                val newDebt = oath.currentDebtAmountPaise + interest

                oathRepo.updateDebt(oath.id, newDebt)
                ledgerRepo.insertDebit(interest, TransactionCategory.OATH_INTEREST, sourceRef = oath.id)
            }
        }
    }

    suspend fun completeOath(oathId: String) {
        val oath = oathRepo.getById(oathId) ?: return
        if (oath.status != "ACTIVE") return
        val nowMs = System.currentTimeMillis()
        val isEarly = nowMs < oath.dueDateMs

        val status = if (isEarly) "COMPLETED_EARLY" else "COMPLETED_ON_TIME"
        val creditImpact = if (isEarly) 15 else 10

        oathRepo.completeOath(oathId, status, nowMs, creditImpact)
        if (isEarly) creditScoreManager.onOathCompletedEarly() 
        else creditScoreManager.onOathCompletedOnTime()

        // Debt is forgiven on completion — no deduction needed
        // The original loan credit and debt balance it out
    }
    
    suspend fun defaultOath(oathId: String) {
        val oath = oathRepo.getById(oathId) ?: return
        if (oath.status != "ACTIVE") return
        
        oathRepo.completeOath(oathId, "DEFAULTED", System.currentTimeMillis(), -30)
        creditScoreManager.onOathDefaulted()
        
        // When defaulted, you must pay back the debt immediately
        ledgerRepo.insertDebit(oath.currentDebtAmountPaise, TransactionCategory.OATH_PENALTY, sourceRef = oathId)
    }

    // Shadow Budget: credit score improves interest rates
    private suspend fun getCreditAdjustedInterestRate(): Int {
        val creditScore = creditScoreManager.getScore()  // 0–850 scale
        val baseRate = configRepo.getInt("default_oath_interest_bps", 500)  // 5%
        return when {
            creditScore >= 750 -> (baseRate * 0.5).toInt()   // 2.5% for excellent
            creditScore >= 650 -> (baseRate * 0.75).toInt()  // 3.75% for good
            creditScore >= 550 -> baseRate                    // 5% for fair
            else -> (baseRate * 1.5).toInt()                 // 7.5% for poor
        }
    }
}

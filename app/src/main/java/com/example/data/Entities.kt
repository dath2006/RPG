package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "ledger_entries")
data class LedgerEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amountPaise: Long,           // Positive for earnings, negative for debits
    val category: String,            // Store Enum name as String: TransactionCategory
    val subcategory: String? = null,  // e.g., "Instagram", "Swiggy", "NFC-Session"
    val timestampMs: Long = System.currentTimeMillis(),
    val isVerified: Boolean = true,
    val isDisputeFlagged: Boolean = false,
    val disputeNotes: String? = null,
    val sourceRef: String? = null,   // e.g., NFC tag UID, UPI ref number, Session ID
    val merchantName: String? = null,
    val spendingClassification: String = "UNCLASSIFIED" // SpendingClass
)

@Entity(tableName = "oaths")
data class Oath(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val initialLoanAmountPaise: Long,
    val currentDebtAmountPaise: Long,
    val taskDescription: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val dueDateMs: Long,
    val dailyInterestRateBps: Int = 500, // Basis points: 500 = 5%
    val status: String = "ACTIVE",       // OathStatus
    val completedAtMs: Long? = null,
    val creditScoreImpact: Int = 0
)

@Entity(tableName = "distraction_rules")
data class DistractionRule(
    @PrimaryKey val packageName: String,
    val appDisplayName: String,
    val costPerMinutePaise: Long,
    val surgeCostPerMinutePaise: Long,
    val isSurgePricingEnabled: Boolean = false,
    val monthlyCapPaise: Long? = null,
    val currentMonthSpentPaise: Long = 0L
)

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey val dateEpochDay: Long, // LocalDate.toEpochDay()
    val totalEarnedPaise: Long = 0L,
    val totalSpentPaise: Long = 0L,
    val totalDistractionMinutes: Int = 0,
    val nfcMinutesWorked: Int = 0,
    val stepCount: Int = 0,
    val streakDayCount: Int = 0,
    val earningMultiplierBps: Int = 10000, // 10000 = 1.0x
    val lazyTaxApplied: Boolean = false,
    val mercyTokenUsed: Boolean = false
)

@Entity(tableName = "evidence_submissions")
data class EvidenceSubmission(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val claimDescription: String,
    val claimAmountPaise: Long,
    val photoFilePath: String, // Local file path
    val submittedAtMs: Long = System.currentTimeMillis(),
    val status: String = "PENDING_AI", // EvidenceStatus
    val aiVerdict: Boolean? = null,
    val aiReasoning: String? = null,
    val disputeOverride: Boolean = false
)

@Entity(tableName = "economy_config")
data class EconomyConfig(
    @PrimaryKey val key: String,
    val valuePaise: Long? = null,
    val valueInt: Int? = null,
    val valueBool: Boolean? = null,
    val valueString: String? = null
)

@Entity(tableName = "nfc_sessions")
data class NfcSession(
    @PrimaryKey val sessionId: String = UUID.randomUUID().toString(),
    val nfcTagUid: String,
    val startTimeMs: Long = System.currentTimeMillis(),
    val endTimeMs: Long? = null,
    val durationMinutes: Int? = null,
    val amountEarnedPaise: Long? = null,
    val isActive: Boolean = true
)

@Entity(tableName = "dispute_queue")
data class DisputeEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val originalLedgerEntryId: String,
    val reason: String, // DisputeReason
    val userNotes: String? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val status: String = "OPEN", // DisputeStatus
    val resolutionNotes: String? = null,
    val reversalLedgerEntryId: String? = null
)

// Supported Enum Constants represented as strings above:
object TransactionCategory {
    const val NFC_FOCUS = "NFC_FOCUS"
    const val MANUAL_EVIDENCE = "MANUAL_EVIDENCE"
    const val STEP_INCOME = "STEP_INCOME"
    const val STREAK_BONUS = "STREAK_BONUS"
    const val SALARY_DAY_BONUS = "SALARY_DAY_BONUS"
    const val LOOT_DROP = "LOOT_DROP"
    const val OATH_LOAN = "OATH_LOAN"
    const val UPI_DEBIT_ESSENTIAL = "UPI_DEBIT_ESSENTIAL"
    const val UPI_DEBIT_DISCRETIONARY = "UPI_DEBIT_DISCRETIONARY"
    const val UPI_DEBIT_UNCLASSIFIED = "UPI_DEBIT_UNCLASSIFIED"
    const val DISTRACTION_DRAIN = "DISTRACTION_DRAIN"
    const val LAZY_TAX = "LAZY_TAX"
    const val OATH_INTEREST = "OATH_INTEREST"
    const val OATH_PENALTY = "OATH_PENALTY"
    const val DISPUTE_REVERSAL = "DISPUTE_REVERSAL"
    const val UNLOCK_TAX = "UNLOCK_TAX"
}

enum class SpendingClass { ESSENTIAL, DISCRETIONARY, UNCLASSIFIED }
enum class OathStatus { ACTIVE, COMPLETED_EARLY, COMPLETED_ON_TIME, DEFAULTED }
enum class EvidenceStatus { PENDING_AI, VERIFIED, REJECTED, DISPUTED }
enum class DisputeReason { DUPLICATE_CHARGE, NFC_GLITCH, SMS_FALSE_POSITIVE, WRONG_AMOUNT, OTHER }
enum class DisputeStatus { OPEN, AI_REVIEWED, RESOLVED_REVERSED, RESOLVED_UPHELD, MANUALLY_OVERRIDDEN }

package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LedgerEntry)

    @Query("SELECT SUM(amountPaise) FROM ledger_entries")
    fun observeSumPaise(): Flow<Long?>

    @Query("SELECT * FROM ledger_entries ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries ORDER BY timestampMs DESC")
    suspend fun getAllOnce(): List<LedgerEntry>

    @Query("SELECT SUM(amountPaise) FROM ledger_entries WHERE amountPaise > 0 AND timestampMs >= :startMs")
    suspend fun getTotalEarnedSince(startMs: Long): Long

    @Query("SELECT SUM(amountPaise) FROM ledger_entries WHERE amountPaise < 0 AND timestampMs >= :startMs")
    suspend fun getTotalSpentSince(startMs: Long): Long

    @Query("SELECT * FROM ledger_entries WHERE id = :id")
    suspend fun getById(id: String): LedgerEntry?

    @Query("SELECT COUNT(*) > 0 FROM ledger_entries WHERE amountPaise = :amountPaise AND timestampMs >= :sinceMs")
    suspend fun hasRecentDebit(amountPaise: Long, sinceMs: Long): Boolean

    @Query("SELECT COUNT(*) > 0 FROM ledger_entries WHERE amountPaise = :amountPaise AND id != :excludeId AND timestampMs >= :startMs AND timestampMs <= :endMs")
    suspend fun hasDuplicateWithin(amountPaise: Long, startMs: Long, endMs: Long, excludeId: String): Boolean
}

@Dao
interface NfcSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: NfcSession)

    @Update
    suspend fun update(session: NfcSession)

    @Query("SELECT * FROM nfc_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): NfcSession?

    @Query("SELECT * FROM nfc_sessions ORDER BY startTimeMs DESC")
    fun observeAllSessions(): Flow<List<NfcSession>>

    @Query("UPDATE nfc_sessions SET isActive = 0, endTimeMs = :endTimeMs, durationMinutes = :duration, amountEarnedPaise = :earned WHERE sessionId = :sessionId")
    suspend fun completeSession(sessionId: String, endTimeMs: Long, duration: Int, earned: Long)

    @Query("DELETE FROM nfc_sessions WHERE sessionId = :sessionId")
    suspend fun cancelSession(sessionId: String)
}

@Dao
interface EvidenceSubmissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(submission: EvidenceSubmission)

    @Update
    suspend fun update(submission: EvidenceSubmission)

    @Query("SELECT * FROM evidence_submissions ORDER BY submittedAtMs DESC")
    fun observeAll(): Flow<List<EvidenceSubmission>>

    @Query("SELECT * FROM evidence_submissions WHERE status = 'PENDING_AI' ORDER BY submittedAtMs ASC")
    suspend fun getPendingAI(): List<EvidenceSubmission>

    @Query("SELECT * FROM evidence_submissions WHERE id = :id")
    suspend fun getById(id: String): EvidenceSubmission?

    @Query("UPDATE evidence_submissions SET status = :status, aiReasoning = :reason WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, reason: String)
}

@Dao
interface DistractionRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: DistractionRule)

    @Query("SELECT * FROM distraction_rules")
    fun observeAllRules(): Flow<List<DistractionRule>>

    @Query("SELECT * FROM distraction_rules")
    suspend fun getAllRules(): List<DistractionRule>

    @Query("SELECT * FROM distraction_rules WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): DistractionRule?

    @Query("UPDATE distraction_rules SET currentMonthSpentPaise = currentMonthSpentPaise + :amount WHERE packageName = :packageName")
    suspend fun incrementMonthlySpent(packageName: String, amount: Long)

    @Query("UPDATE distraction_rules SET currentMonthSpentPaise = 0")
    suspend fun resetAllMonthlySpent()
}

@Dao
interface DailyStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: DailyStats)

    @Query("SELECT * FROM daily_stats ORDER BY dateEpochDay DESC")
    fun observeAllStats(): Flow<List<DailyStats>>

    @Query("SELECT * FROM daily_stats")
    suspend fun getAllOnce(): List<DailyStats>

    @Query("SELECT * FROM daily_stats WHERE dateEpochDay = :dateEpochDay")
    suspend fun getForDay(dateEpochDay: Long): DailyStats?
}

@Dao
interface EconomyConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: EconomyConfig)

    @Query("SELECT * FROM economy_config WHERE `key` = :key")
    suspend fun getByKey(key: String): EconomyConfig?

    @Query("SELECT * FROM economy_config")
    fun observeAll(): Flow<List<EconomyConfig>>
}

@Dao
interface OathDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(oath: Oath)

    @Update
    suspend fun update(oath: Oath)

    @Query("SELECT * FROM oaths ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<Oath>>

    @Query("SELECT * FROM oaths ORDER BY createdAtMs DESC")
    suspend fun getAllOnce(): List<Oath>

    @Query("SELECT * FROM oaths WHERE status = 'ACTIVE'")
    suspend fun getActiveOaths(): List<Oath>

    @Query("SELECT * FROM oaths WHERE id = :id")
    suspend fun getById(id: String): Oath?

    @Query("UPDATE oaths SET status = :status, completedAtMs = :completedAtMs, creditScoreImpact = :creditScoreImpact WHERE id = :id")
    suspend fun complete(id: String, status: String, completedAtMs: Long, creditScoreImpact: Int)

    @Query("UPDATE oaths SET currentDebtAmountPaise = :newDebtAmount WHERE id = :id")
    suspend fun updateDebt(id: String, newDebtAmount: Long)
}

@Dao
interface DisputeQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dispute: DisputeEntry)

    @Update
    suspend fun update(dispute: DisputeEntry)

    @Query("SELECT * FROM dispute_queue ORDER BY createdAtMs DESC")
    fun observeAllDisplay(): Flow<List<DisputeEntry>>

    @Query("SELECT * FROM dispute_queue WHERE id = :id")
    suspend fun getById(id: String): DisputeEntry?

    @Query("UPDATE dispute_queue SET status = :status, resolutionNotes = :notes, reversalLedgerEntryId = :reversalId WHERE id = :id")
    suspend fun resolve(id: String, status: String, notes: String, reversalId: String?)
}

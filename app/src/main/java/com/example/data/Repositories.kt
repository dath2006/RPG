package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LedgerRepository(private val ledgerDao: LedgerDao) {
    // Current balance computed reactively
    val balanceFlow: Flow<Long> = ledgerDao.observeSumPaise().map { it ?: 0L }

    val allEntries: Flow<List<LedgerEntry>> = ledgerDao.observeAll()

    suspend fun getBalanceSnapshot(): Long {
        val all = ledgerDao.getAllOnce()
        return all.sumOf { it.amountPaise }
    }

    suspend fun insertEarning(
        amountPaise: Long,
        category: String,
        subcategory: String? = null,
        sourceRef: String? = null
    ): LedgerEntry {
        require(amountPaise > 0) { "Earning amount must be positive" }
        val entry = LedgerEntry(
            amountPaise = amountPaise,
            category = category,
            subcategory = subcategory,
            isVerified = true,
            sourceRef = sourceRef
        )
        ledgerDao.insert(entry)
        return entry
    }

    suspend fun insertDebit(
        amountPaise: Long,
        category: String,
        subcategory: String? = null,
        merchantName: String? = null,
        classification: String = "UNCLASSIFIED",
        sourceRef: String? = null
    ): LedgerEntry {
        require(amountPaise > 0) { "Debit amount in parameters must be positive; it will be stored as negative in the DB" }
        val entry = LedgerEntry(
            amountPaise = -amountPaise,
            category = category,
            subcategory = subcategory,
            merchantName = merchantName,
            isVerified = true,
            spendingClassification = classification,
            sourceRef = sourceRef
        )
        ledgerDao.insert(entry)
        return entry
    }

    suspend fun getTotalEarnedSince(sinceMs: Long): Long {
        return ledgerDao.getTotalEarnedSince(sinceMs)
    }

    suspend fun getTotalSpentSince(sinceMs: Long): Long {
        return ledgerDao.getTotalSpentSince(sinceMs)
    }

    suspend fun getEntryById(id: String): LedgerEntry? {
        return ledgerDao.getById(id)
    }

    suspend fun hasDuplicateWithinSafe(amountPaise: Long, timestampMs: Long, windowMs: Long, excludeId: String): Boolean {
        val startMs = timestampMs - windowMs
        val endMs = timestampMs + windowMs
        return ledgerDao.hasDuplicateWithin(amountPaise, startMs, endMs, excludeId)
    }
    
    suspend fun hasRecentDebit(amountPaise: Long, sinceMs: Long): Boolean {
        return ledgerDao.hasRecentDebit(amountPaise, sinceMs)
    }
}

class NfcSessionRepository(private val nfcSessionDao: NfcSessionDao) {
    val allSessions: Flow<List<NfcSession>> = nfcSessionDao.observeAllSessions()

    suspend fun getActiveSession(): NfcSession? {
        return nfcSessionDao.getActiveSession()
    }

    suspend fun startSession(tagUid: String): NfcSession {
        val session = NfcSession(
            nfcTagUid = tagUid,
            startTimeMs = System.currentTimeMillis(),
            isActive = true
        )
        nfcSessionDao.insert(session)
        return session
    }

    suspend fun completeSession(sessionId: String, endTimeMs: Long, duration: Int, earned: Long) {
        nfcSessionDao.completeSession(sessionId, endTimeMs, duration, earned)
    }

    suspend fun cancelSession(sessionId: String) {
        nfcSessionDao.cancelSession(sessionId)
    }
}

class EvidenceRepository(private val evidenceDao: EvidenceSubmissionDao) {
    val allSubmissions: Flow<List<EvidenceSubmission>> = evidenceDao.observeAll()

    suspend fun submit(claimDescription: String, claimAmountPaise: Long, photoPath: String): EvidenceSubmission {
        val submission = EvidenceSubmission(
            claimDescription = claimDescription,
            claimAmountPaise = claimAmountPaise,
            photoFilePath = photoPath,
            submittedAtMs = System.currentTimeMillis(),
            status = "PENDING_AI"
        )
        evidenceDao.insert(submission)
        return submission
    }

    // Phase 1: Manually verify/reject for debugging/simple flow, as Gemma vision AI is in Phase 3
    suspend fun updateStatus(id: String, status: String, reasoning: String) {
        evidenceDao.updateStatus(id, status, reasoning)
    }

    suspend fun getById(id: String): EvidenceSubmission? {
        return evidenceDao.getById(id)
    }

    suspend fun insertRaw(submission: EvidenceSubmission) {
        evidenceDao.insert(submission)
    }
}

class EconomyConfigRepository(private val configDao: EconomyConfigDao) {
    val allConfigs: Flow<List<EconomyConfig>> = configDao.observeAll()

    suspend fun getVal(key: String, defaultLong: Long? = null, defaultInt: Int? = null, defaultBool: Boolean? = null, defaultStr: String? = null): EconomyConfig {
        val existing = configDao.getByKey(key)
        if (existing != null) return existing
        return EconomyConfig(key, valuePaise = defaultLong, valueInt = defaultInt, valueBool = defaultBool, valueString = defaultStr)
    }

    suspend fun setVal(config: EconomyConfig) {
        configDao.insert(config)
    }

    suspend fun getLong(key: String, default: Long): Long {
        return configDao.getByKey(key)?.valuePaise ?: default
    }

    suspend fun getInt(key: String, default: Int): Int {
        return configDao.getByKey(key)?.valueInt ?: default
    }

    suspend fun getBool(key: String, default: Boolean): Boolean {
        return configDao.getByKey(key)?.valueBool ?: default
    }

    suspend fun setInt(key: String, value: Int) {
        configDao.insert(EconomyConfig(key, valueInt = value))
    }

    suspend fun setLong(key: String, value: Long) {
        configDao.insert(EconomyConfig(key, valuePaise = value))
    }

    suspend fun setBool(key: String, value: Boolean) {
        configDao.insert(EconomyConfig(key, valueBool = value))
    }
}

class DistractionRuleRepository(private val ruleDao: DistractionRuleDao) {
    val allRules: Flow<List<DistractionRule>> = ruleDao.observeAllRules()

    suspend fun getAllRulesOnce(): List<DistractionRule> {
        return ruleDao.getAllRules()
    }

    suspend fun insertOrUpdate(rule: DistractionRule) {
        ruleDao.insert(rule)
    }

    suspend fun incrementSpent(packageName: String, amount: Long) {
        ruleDao.incrementMonthlySpent(packageName, amount)
    }

    suspend fun resetAllMonthlySpent() {
        ruleDao.resetAllMonthlySpent()
    }
}

class StatsRepository(private val statsDao: DailyStatsDao) {
    val allStats: Flow<List<DailyStats>> = statsDao.observeAllStats()

    suspend fun saveStats(stats: DailyStats) {
        statsDao.upsert(stats)
    }

    suspend fun getForDay(epochDay: Long): DailyStats? {
        return statsDao.getForDay(epochDay)
    }
}

class OathRepository(private val oathDao: OathDao) {
    val allOaths: Flow<List<Oath>> = oathDao.observeAll()

    suspend fun createOath(oath: Oath) {
        oathDao.insert(oath)
    }

    suspend fun getActiveOaths(): List<Oath> {
        return oathDao.getActiveOaths()
    }

    suspend fun updateDebt(id: String, debt: Long) {
        oathDao.updateDebt(id, debt)
    }

    suspend fun completeOath(id: String, status: String, completedAtMs: Long, creditScoreImpact: Int) {
        oathDao.complete(id, status, completedAtMs, creditScoreImpact)
    }

    suspend fun getById(id: String): Oath? {
        return oathDao.getById(id)
    }
}

class DisputeQueueRepository(private val disputeDao: DisputeQueueDao) {
    val allDisputes: Flow<List<DisputeEntry>> = disputeDao.observeAllDisplay()
    
    suspend fun getById(id: String): DisputeEntry? {
        return disputeDao.getById(id)
    }

    suspend fun insertRaw(dispute: DisputeEntry) {
        disputeDao.insert(dispute)
    }
    
    suspend fun resolveWithReversal(disputeId: String, status: String, notes: String, reversalId: String?) {
        disputeDao.resolve(disputeId, status, notes, reversalId)
    }
}

package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ProductivityApplication
import com.example.data.*
import com.example.service.DisputeService
import com.example.service.EvidenceVerifier
import com.example.service.GemmaModelManager
import com.example.service.OathService
import com.example.service.SalaryAndStepService
import com.example.service.MercyTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DashboardViewModel(
    private val appCtx: android.app.Application,
    private val ledgerRepo: LedgerRepository,
    private val nfcRepo: NfcSessionRepository,
    private val evidenceRepo: EvidenceRepository,
    private val configRepo: EconomyConfigRepository,
    private val distractionRepo: DistractionRuleRepository,
    private val statsRepo: StatsRepository,
    private val oathRepo: OathRepository,
    private val disputeService: DisputeService,
    private val evidenceVerifier: EvidenceVerifier,
    private val gemmaModelManager: GemmaModelManager,
    private val oathService: OathService,
    private val salaryAndStepService: SalaryAndStepService,
    private val mercyTokenManager: MercyTokenManager,
    private val databaseExporter: com.example.service.DatabaseExporter
) : ViewModel() {

    // Live reactive states bounded to Room Flow
    val balance: StateFlow<Long> = ledgerRepo.balanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val ledgerEntries: StateFlow<List<LedgerEntry>> = ledgerRepo.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val evidenceSubmissions: StateFlow<List<EvidenceSubmission>> = evidenceRepo.allSubmissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distractionRules: StateFlow<List<DistractionRule>> = distractionRepo.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val oaths: StateFlow<List<Oath>> = oathRepo.allOaths
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNfcSession = MutableStateFlow<NfcSession?>(null)

    // Combined or configured properties
    val configState = MutableStateFlow<Map<String, Any>>(emptyMap())

    val streak: StateFlow<Int> = configRepo.allConfigs.combine(MutableStateFlow(0)) { list, _ ->
        list.find { it.key == "current_streak" }?.valueInt ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val multiplier: StateFlow<Double> = configRepo.allConfigs.combine(MutableStateFlow(0)) { list, _ ->
        val bps = list.find { it.key == "earning_multiplier_bps" }?.valueInt ?: 10000
        bps / 10000.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

    val creditScore: StateFlow<Int> = configRepo.allConfigs.combine(MutableStateFlow(0)) { list, _ ->
        list.find { it.key == "credit_score" }?.valueInt ?: 600
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 600)

    val mercyTokens: StateFlow<Int> = configRepo.allConfigs.combine(MutableStateFlow(0)) { list, _ ->
        list.find { it.key == "mercy_tokens" }?.valueInt ?: 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    // Model state
    val modelDownloadState: StateFlow<com.example.service.ModelDownloadState> = 
        gemmaModelManager.downloadState
        
    fun downloadAiModel() {
        gemmaModelManager.downloadModel()
    }

    init {
        viewModelScope.launch {
            activeNfcSession.value = nfcRepo.getActiveSession()
        }
    }

    // --- NFC FOCUS SESSIONS CONTROL ---
    fun startNfcSession(tagUid: String) {
        viewModelScope.launch {
            val session = nfcRepo.startSession(tagUid)
            activeNfcSession.value = session
            showJailStateNotification(true)
        }
    }

    fun endNfcSession(simulatedMinutes: Int) {
        viewModelScope.launch {
            val session = activeNfcSession.value ?: return@launch
            val nowMs = System.currentTimeMillis()
            val ratePerHourConfig = configRepo.getVal("nfc_rate_per_hour_paise", defaultLong = 20000L)
            val ratePerHour = ratePerHourConfig.valuePaise ?: 20000L

            val finalMinutes = if (simulatedMinutes > 0) simulatedMinutes else {
                val diff = nowMs - session.startTimeMs
                (diff / 60_000.0).coerceAtLeast(1.0).toInt()
            }

            // Calculate wage: (ratePerHour * hours) * multiplier
            val currentMultiplierBps = configRepo.getVal("earning_multiplier_bps", defaultInt = 10000).valueInt ?: 10000
            val rawEarning = (ratePerHour * (finalMinutes / 60.0)).toLong()
            val finalEarnings = (rawEarning * (currentMultiplierBps / 10000.0)).toLong()

            nfcRepo.completeSession(session.sessionId, nowMs, finalMinutes, finalEarnings)
            activeNfcSession.value = null

            // Record into central general ledger
            ledgerRepo.insertEarning(
                amountPaise = finalEarnings,
                category = TransactionCategory.NFC_FOCUS,
                subcategory = "Desk Focus: $finalMinutes min 🧠",
                sourceRef = session.sessionId
            )
            showJailStateNotification(false)
        }
    }

    fun discardNfcSession() {
        viewModelScope.launch {
            val session = activeNfcSession.value ?: return@launch
            nfcRepo.cancelSession(session.sessionId)
            activeNfcSession.value = null
            showJailStateNotification(false)
        }
    }

    private fun showJailStateNotification(active: Boolean) {
        val manager = appCtx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel("jail_channel", "Focus Jail", android.app.NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        
        if (active) {
            val notification = androidx.core.app.NotificationCompat.Builder(appCtx, "jail_channel")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("🔒 IN FOCUS JAIL")
                .setContentText("Complete your task to get paid. Do not cancel!")
                .setOngoing(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .build()
            manager.notify(101, notification)
        } else {
            manager.cancel(101)
        }
    }

    // --- MANUAL STUDY EVIDENCE SUBMISSIONS ---
    fun submitStudyEvidence(claim: String, amountPaise: Long, imagePreset: String) {
        viewModelScope.launch {
            evidenceRepo.submit(
                claimDescription = claim,
                claimAmountPaise = amountPaise,
                photoPath = imagePreset // Using a mock URI preset
            )
        }
    }

    fun runAutoAIVerifier(id: String) {
        viewModelScope.launch {
            evidenceVerifier.verifyAsync(id)
        }
    }

    // --- DISTRACTION PRICE SIMULATOR ---
    fun simulateDistractionScroll(packageName: String, minutes: Int) {
        viewModelScope.launch {
            val rules = distractionRepo.getAllRulesOnce()
            val rule = rules.find { it.packageName == packageName } ?: return@launch
            val costPerMinute = rule.costPerMinutePaise
            val spentPaise = costPerMinute * minutes

            ledgerRepo.insertDebit(
                amountPaise = spentPaise,
                category = TransactionCategory.DISTRACTION_DRAIN,
                subcategory = "${rule.appDisplayName} ($minutes min) 📱"
            )
            distractionRepo.incrementSpent(packageName, spentPaise)
        }
    }

    // --- MIDNIGHT AUDIT MANUAL SIMULATOR ---
    fun simulateMidnightAudit() {
        viewModelScope.launch {
            val last24Hr = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
            val earnings = ledgerRepo.getTotalEarnedSince(last24Hr)

            val curStreak = configRepo.getVal("current_streak", defaultInt = 0).valueInt ?: 0
            val lazyTaxPaise = configRepo.getVal("lazy_tax_paise", defaultLong = 10000L).valuePaise ?: 10000L

            var newStreak = 0
            var gotTaxes = false

            val isMercyActive = configRepo.getBool("mercy_active_today", false)

            if (earnings == 0L && !isMercyActive) {
                ledgerRepo.insertDebit(
                    amountPaise = lazyTaxPaise,
                    category = TransactionCategory.LAZY_TAX,
                    subcategory = "[SIM] Missed work yesterday! 💤"
                )
                configRepo.setInt("current_streak", 0)
                configRepo.setInt("earning_multiplier_bps", 10000)
                gotTaxes = true
            } else if (earnings == 0L && isMercyActive) {
                // Mercerized! Protected from tax and streak loss
                newStreak = curStreak
                configRepo.setBool("mercy_active_today", false) // Reset for tomorrow
            } else {
                newStreak = curStreak + 1
                configRepo.setInt("current_streak", newStreak)

                val nextMult = when {
                    newStreak >= 30 -> 20000
                    newStreak >= 14 -> 17500
                    newStreak >= 7  -> 15000
                    newStreak >= 3  -> 12000
                    else            -> 10000
                }
                configRepo.setInt("earning_multiplier_bps", nextMult)

                if (newStreak % 7 == 0) {
                    ledgerRepo.insertEarning(
                        amountPaise = 20000L,
                        category = TransactionCategory.STREAK_BONUS,
                        subcategory = "[SIM] $newStreak-Day Streak Milestone! 🔥"
                    )
                }
            }

            // Update stats
            val spent = ledgerRepo.getTotalSpentSince(last24Hr)
            statsRepo.saveStats(
                DailyStats(
                    dateEpochDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000L),
                    totalEarnedPaise = earnings,
                    totalSpentPaise = spent,
                    streakDayCount = newStreak,
                    earningMultiplierBps = configRepo.getInt("earning_multiplier_bps", 10000),
                    lazyTaxApplied = gotTaxes
                )
            )
        }
    }

    // --- OATH CREATOR & PAYER ---
    fun createOath(loanAmountPaise: Long, taskDescription: String, durationHours: Int) {
        viewModelScope.launch {
            val due = System.currentTimeMillis() + (durationHours * 60 * 60 * 1000L)
            try {
                oathService.createOath(loanAmountPaise, taskDescription, due)
            } catch (e: IllegalArgumentException) {
                // Ignore for now, or show a toast
            }
        }
    }

    fun completeOath(id: String) {
        viewModelScope.launch {
            oathService.completeOath(id)
        }
    }

    fun defaultOath(id: String) {
        viewModelScope.launch {
            oathService.defaultOath(id)
        }
    }

    val exportString = MutableStateFlow<String?>(null)

    fun generateExport() {
        viewModelScope.launch {
            try {
                val data = databaseExporter.exportToEncryptedString()
                exportString.value = data
            } catch (e: Exception) {
                exportString.value = "Error exporting data: ${e.message}"
            }
        }
    }
    
    fun dismissExport() {
        exportString.value = null
    }

    // --- PHASE 4 GAMIFICATION SIMULATORS ---
    fun simulateSalaryDay() {
        viewModelScope.launch {
            salaryAndStepService.simulateSalaryDay()
        }
    }

    fun simulateStepIncome(steps: Int) {
        viewModelScope.launch {
            salaryAndStepService.simulateStepIncome(steps)
        }
    }
    
    fun spendMercyToken() {
        viewModelScope.launch {
            val result = mercyTokenManager.spendMercyToken()
            if (result is com.example.service.MercyResult.Success) {
                // If they successfully spent it, maybe we want to simulate the lazy tax not hitting them today.
                // In a real app this flag is read by the Midnight Audit Worker
                configRepo.setBool("mercy_active_today", true)
            }
        }
    }

    // --- CUSTOM LEDGER CORRECTIONS ---
    fun insertManualAdd(amountPaise: Long, category: String, notes: String) {
        viewModelScope.launch {
            ledgerRepo.insertEarning(amountPaise, category, notes)
        }
    }

    fun flagDispute(entryId: String, reason: String, notes: String) {
        viewModelScope.launch {
            disputeService.flagTransaction(entryId, reason, notes)
        }
    }

    fun insertManualDeduct(amountPaise: Long, category: String, notes: String) {
        viewModelScope.launch {
            ledgerRepo.insertDebit(amountPaise, category, notes)
        }
    }

    class Factory(
        private val app: ProductivityApplication
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                app,
                app.ledgerRepository,
                app.nfcSessionRepository,
                app.evidenceRepository,
                app.economyConfigRepository,
                app.distractionRuleRepository,
                app.statsRepository,
                app.oathRepository,
                app.disputeService,
                app.evidenceVerifier,
                app.gemmaModelManager,
                app.currentOathService,
                app.currentSalaryAndStepService,
                app.currentMercyTokenManager,
                app.databaseExporter
            ) as T
        }
    }
}

package com.example

import android.app.Application
import com.example.data.*
import com.example.service.DisputeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ProductivityApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    val ledgerRepository by lazy { LedgerRepository(database.ledgerDao()) }
    val nfcSessionRepository by lazy { NfcSessionRepository(database.nfcSessionDao()) }
    val evidenceRepository by lazy { EvidenceRepository(database.evidenceSubmissionDao()) }
    val economyConfigRepository by lazy { EconomyConfigRepository(database.economyConfigDao()) }
    val distractionRuleRepository by lazy { DistractionRuleRepository(database.distractionRuleDao()) }
    val statsRepository by lazy { StatsRepository(database.dailyStatsDao()) }
    val oathRepository by lazy { OathRepository(database.oathDao()) }
    val disputeQueueRepository by lazy { DisputeQueueRepository(database.disputeQueueDao()) }

    val gemmaModelManager by lazy { com.example.service.GemmaModelManager() }
    val gemmaSessionManager by lazy { com.example.service.GemmaSessionManager(gemmaModelManager) }
    val evidenceVerifier by lazy { com.example.service.EvidenceVerifier(gemmaSessionManager, database.evidenceSubmissionDao(), ledgerRepository) }
    
    val currentCreditScoreManager by lazy { com.example.service.CreditScoreManager(economyConfigRepository) }
    val currentMercyTokenManager by lazy { com.example.service.MercyTokenManager(economyConfigRepository, ledgerRepository) }
    val currentOathService by lazy { com.example.service.OathService(oathRepository, ledgerRepository, economyConfigRepository, currentCreditScoreManager) }
    val currentSalaryAndStepService by lazy { com.example.service.SalaryAndStepService(ledgerRepository, economyConfigRepository, statsRepository, currentCreditScoreManager) }
    val databaseExporter by lazy { com.example.service.DatabaseExporter(ledgerRepository, economyConfigRepository) }
    
    val recentRefCache = RecentRefCache()
    val spendingClassifier by lazy { SpendingClassifier(gemmaSessionManager) }
    val expenseProcessor by lazy { 
        ExpenseProcessor(ledgerRepository, spendingClassifier, recentRefCache) 
    }
    
    val disputeService by lazy {
        DisputeService(disputeQueueRepository, ledgerRepository)
    }
}

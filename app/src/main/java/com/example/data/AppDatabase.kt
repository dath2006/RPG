package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        LedgerEntry::class,
        Oath::class,
        DistractionRule::class,
        DailyStats::class,
        EvidenceSubmission::class,
        EconomyConfig::class,
        NfcSession::class,
        DisputeEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
    abstract fun nfcSessionDao(): NfcSessionDao
    abstract fun evidenceSubmissionDao(): EvidenceSubmissionDao
    abstract fun distractionRuleDao(): DistractionRuleDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun economyConfigDao(): EconomyConfigDao
    abstract fun oathDao(): OathDao
    abstract fun disputeQueueDao(): DisputeQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "productivity_economy_db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDefaultConfig(database.economyConfigDao())
                    populateDefaultDistractionRules(database.distractionRuleDao())
                    populateInitialBalance(database.ledgerDao())
                }
            }
        }

        private suspend fun populateDefaultConfig(dao: EconomyConfigDao) {
            val defaults = mapOf(
                "nfc_rate_per_hour_paise" to EconomyConfig("nfc_rate_per_hour_paise", valuePaise = 20000L), // ₹200 / hr
                "lazy_tax_paise" to EconomyConfig("lazy_tax_paise", valuePaise = 10000L), // ₹100 lazy tax
                "streak_3d_multiplier_bps" to EconomyConfig("streak_3d_multiplier_bps", valueInt = 12000), // 1.2x
                "streak_7d_multiplier_bps" to EconomyConfig("streak_7d_multiplier_bps", valueInt = 15000), // 1.5x
                "streak_14d_multiplier_bps" to EconomyConfig("streak_14d_multiplier_bps", valueInt = 17500), // 1.75x
                "streak_30d_multiplier_bps" to EconomyConfig("streak_30d_multiplier_bps", valueInt = 20000), // 2.0x
                "default_oath_interest_bps" to EconomyConfig("default_oath_interest_bps", valueInt = 500), // 5% daily
                "surge_start_time" to EconomyConfig("surge_start_time", valueString = "09:00"),
                "surge_end_time" to EconomyConfig("surge_end_time", valueString = "17:00"),
                "step_income_per_1000_paise" to EconomyConfig("step_income_per_1000_paise", valuePaise = 1000L), // ₹10 per 1000
                "credit_score" to EconomyConfig("credit_score", valueInt = 600), // Default 600
                "current_streak" to EconomyConfig("current_streak", valueInt = 0),
                "earning_multiplier_bps" to EconomyConfig("earning_multiplier_bps", valueInt = 10000) // 1.0x starting
            )
            for (configEntry in defaults.values) {
                dao.insert(configEntry)
            }
        }

        private suspend fun populateDefaultDistractionRules(dao: DistractionRuleDao) {
            val apps = listOf(
                DistractionRule("com.instagram.android", "Instagram", costPerMinutePaise = 200, surgeCostPerMinutePaise = 1000),
                DistractionRule("com.zhiliaoapp.musically", "TikTok", costPerMinutePaise = 200, surgeCostPerMinutePaise = 1000),
                DistractionRule("com.snapchat.android", "Snapchat", costPerMinutePaise = 200, surgeCostPerMinutePaise = 1000),
                DistractionRule("com.google.android.youtube", "YouTube", costPerMinutePaise = 100, surgeCostPerMinutePaise = 500),
                DistractionRule("com.twitter.android", "Twitter/X", costPerMinutePaise = 150, surgeCostPerMinutePaise = 600),
                DistractionRule("com.facebook.katana", "Facebook", costPerMinutePaise = 100, surgeCostPerMinutePaise = 500)
            )
            for (app in apps) {
                dao.insert(app)
            }
        }

        private suspend fun populateInitialBalance(dao: LedgerDao) {
            // Give user initial ₹500 starter balance to keep them motivated!
            dao.insert(
                LedgerEntry(
                    amountPaise = 50000L, // ₹500
                    category = TransactionCategory.LOOT_DROP,
                    subcategory = "Starter Loot Drop! 🎒",
                    isVerified = true
                )
            )
        }
    }
}

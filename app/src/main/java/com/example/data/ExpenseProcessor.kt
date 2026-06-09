package com.example.data

class ExpenseProcessor(
    private val ledgerRepo: LedgerRepository,
    private val classifier: SpendingClassifier,
    private val seenRefsCache: RecentRefCache
) {
    suspend fun processUpiDebit(
        amountPaise: Long,
        merchantName: String?,
        source: String,
        referenceId: String?
    ) {
        if (referenceId != null && seenRefsCache.contains(referenceId)) return
        referenceId?.let { seenRefsCache.add(it) }

        // Dupe by amount + time
        val isDuplicate = ledgerRepo.hasRecentDebit(amountPaise, System.currentTimeMillis() - 60_000L)
        if (isDuplicate) return

        val classification = classifier.classify(merchantName)

        val category = when (classification) {
            SpendingClass.ESSENTIAL.name -> TransactionCategory.UPI_DEBIT_ESSENTIAL
            SpendingClass.DISCRETIONARY.name -> TransactionCategory.UPI_DEBIT_DISCRETIONARY
            else -> TransactionCategory.UPI_DEBIT_UNCLASSIFIED
        }

        ledgerRepo.insertDebit(
            amountPaise = amountPaise,
            category = category,
            merchantName = merchantName,
            classification = classification,
            sourceRef = referenceId ?: "$source-${System.currentTimeMillis()}"
        )
    }
}

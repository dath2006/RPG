package com.example.service

import com.example.data.DisputeEntry
import com.example.data.DisputeReason
import com.example.data.DisputeStatus
import com.example.data.DisputeQueueRepository
import com.example.data.LedgerEntry
import com.example.data.LedgerRepository
import com.example.data.TransactionCategory

class DisputeService(
    private val disputeRepo: DisputeQueueRepository,
    private val ledgerRepo: LedgerRepository
) {
    suspend fun flagTransaction(
        ledgerEntryId: String,
        reason: String,
        userNotes: String?
    ): DisputeEntry {
        val entry = ledgerRepo.getEntryById(ledgerEntryId)
            ?: throw IllegalArgumentException("Entry not found")

        val dispute = DisputeEntry(
            originalLedgerEntryId = ledgerEntryId,
            reason = reason,
            userNotes = userNotes,
            status = DisputeStatus.OPEN.name
        )
        disputeRepo.insertRaw(dispute)

        // For clear-cut auto-reversible cases, process immediately
        if (reason == DisputeReason.DUPLICATE_CHARGE.name) {
            autoResolveIfDuplicate(dispute, entry)
        }

        return dispute
    }

    private suspend fun autoResolveIfDuplicate(dispute: DisputeEntry, entry: LedgerEntry) {
        // Look for another entry with same amount within 2 minutes
        val duplicateExists = ledgerRepo.hasDuplicateWithinSafe(
            amountPaise = entry.amountPaise,
            timestampMs = entry.timestampMs,
            windowMs = 120_000L,
            excludeId = entry.id
        )

        if (duplicateExists) {
            resolveWithReversal(dispute.id, "Auto-detected duplicate within 2 minutes")
        }
    }

    suspend fun resolveWithReversal(disputeId: String, resolutionNotes: String) {
        val dispute = disputeRepo.getById(disputeId) ?: return
        val originalEntry = ledgerRepo.getEntryById(dispute.originalLedgerEntryId) ?: return

        // Insert a correcting counter-entry (maintains immutability)
        // Reversal of debit is earnings and viceversa
        val reversalEntry = if (originalEntry.amountPaise < 0) {
            ledgerRepo.insertEarning(
                amountPaise = -originalEntry.amountPaise,
                category = TransactionCategory.DISPUTE_REVERSAL,
                subcategory = "Reversal of ${originalEntry.category}",
                sourceRef = disputeId
            )
        } else {
            // Reversing an earning would be a debit
            ledgerRepo.insertDebit( // insertDebit internally negates it, so we pass positive
                amountPaise = originalEntry.amountPaise,
                category = TransactionCategory.DISPUTE_REVERSAL,
                subcategory = "Reversal of ${originalEntry.category}",
                sourceRef = disputeId
            )
        }

        disputeRepo.resolveWithReversal(
            disputeId = disputeId,
            status = DisputeStatus.RESOLVED_REVERSED.name,
            notes = resolutionNotes,
            reversalId = reversalEntry.id
        )
    }
}

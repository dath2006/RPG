package com.example.service

import com.example.data.EvidenceSubmission
import com.example.data.EvidenceSubmissionDao
import com.example.data.LedgerRepository
import com.example.data.TransactionCategory
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EvidenceVerifier(
    private val gemmaSession: GemmaSessionManager,
    private val evidenceDao: EvidenceSubmissionDao,
    private val ledgerRepo: LedgerRepository
) {
    suspend fun verifyAsync(submissionId: String) {
        val submission = evidenceDao.getById(submissionId) ?: return
        if (!gemmaSession.isModelLoaded()) return

        val prompt = buildVerificationPrompt(submission)

        // Using mock local image representation
        val response = gemmaSession.generateWithVision(
            prompt = prompt,
            imageFilePath = submission.photoFilePath,
            maxTokens = 150
        )

        processVerificationResponse(submissionId, submission, response)
    }

    private fun buildVerificationPrompt(submission: EvidenceSubmission): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return """
            You are a strict but fair productivity coach verifying study evidence.
            
            THE USER CLAIMS: "${submission.claimDescription}"
            REQUESTED REWARD: ₹${submission.claimAmountPaise / 100}
            SUBMITTED AT: ${dateFormat.format(Date(submission.submittedAtMs))}
            
            VERIFICATION RULES:
            1. The image must show a computer screen, textbook, notebook, or study material
            2. The content visible must be plausibly related to the claim
            3. Reject if: blank screen, phone lock screen, unrelated content, unclear image
            4. Partial credit (50%) if: image is related but claim seems exaggerated
            
            Analyze the image carefully and respond ONLY with this JSON:
            {
              "verified": true/false,
              "confidence": "HIGH"/"MEDIUM"/"LOW",
              "partialCredit": true/false,
              "creditPercent": 100,
              "reasoning": "one sentence explanation"
            }
        """.trimIndent()
    }

    private suspend fun processVerificationResponse(
        submissionId: String,
        submission: EvidenceSubmission,
        rawResponse: String
    ) {
        try {
            val json = JSONObject(rawResponse.trim())
            val verified = json.getBoolean("verified")
            val reasoning = json.getString("reasoning")
            val creditPercent = json.optInt("creditPercent", 100)

            if (verified) {
                val effectiveAmount = (submission.claimAmountPaise * creditPercent / 100)
                ledgerRepo.insertEarning(
                    amountPaise = effectiveAmount,
                    category = TransactionCategory.MANUAL_EVIDENCE,
                    subcategory = submission.claimDescription.take(50),
                    sourceRef = submissionId
                )
                evidenceDao.updateStatus(submissionId, "VERIFIED", reasoning)
            } else {
                evidenceDao.updateStatus(submissionId, "REJECTED", reasoning)
            }
        } catch (e: JSONException) {
            evidenceDao.updateStatus(submissionId, "PENDING_AI", "AI parse error — pending review")
        }
    }
}

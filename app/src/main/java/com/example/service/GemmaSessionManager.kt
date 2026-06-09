package com.example.service

import org.json.JSONObject
import kotlinx.coroutines.delay

class GemmaSessionManager(
    private val modelManager: GemmaModelManager
) {
    fun isModelLoaded(): Boolean = modelManager.isModelAvailable()

    // Used by SpendingClassifier
    suspend fun generate(prompt: String, maxTokens: Int): String {
        delay(800) // Simulate inference time
        // Just a mock response depending on the prompt
        return if (prompt.contains("ESSENTIAL or DISCRETIONARY", ignoreCase = true)) {
            if (prompt.contains("swiggy", ignoreCase = true) || prompt.contains("netflix", ignoreCase = true)) {
                JSONObject().put("classification", "DISCRETIONARY").toString()
            } else {
                JSONObject().put("classification", "ESSENTIAL").toString()
            }
        } else {
            JSONObject().put("classification", "UNCLASSIFIED").toString()
        }
    }

    // Used by EvidenceVerifier
    suspend fun generateWithVision(prompt: String, imageFilePath: String, maxTokens: Int): String {
        delay(1500) // Simulate vision inference time
        
        // Return a mock structured response
        val json = JSONObject()
        json.put("verified", true)
        json.put("confidence", "HIGH")
        json.put("partialCredit", false)
        json.put("creditPercent", 100)
        json.put("reasoning", "Plausible evidence of sustained study activity based on visual content.")
        
        return json.toString()
    }
}

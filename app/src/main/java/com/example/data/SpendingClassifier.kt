package com.example.data

import com.example.service.GemmaSessionManager
import org.json.JSONObject

class SpendingClassifier(
    private val gemmaSession: GemmaSessionManager
) {

    private val essentialKeywords = setOf(
        "hospital", "medical", "pharmacy", "medicine", "doctor", "clinic",
        "grocery", "vegetables", "milk", "provision", "supermarket", "dmart",
        "reliance fresh", "bigbasket", "metro", "bus", "petrol",
        "electricity", "water bill", "rent", "loan emi", "insurance"
    )

    private val discretionaryKeywords = setOf(
        "swiggy", "zomato", "blinkit", "dunzo", "zepto",
        "amazon", "flipkart", "myntra", "ajio", "meesho",
        "netflix", "hotstar", "prime", "spotify", "youtube premium",
        "starbucks", "mcdonalds", "dominos", "kfc", "pizza",
        "bar", "pub", "nightclub", "salon", "spa", "gaming"
    )

    suspend fun classify(merchantName: String?): String {
        if (merchantName == null) return SpendingClass.UNCLASSIFIED.name
        val lower = merchantName.lowercase()

        if (essentialKeywords.any { lower.contains(it) }) return SpendingClass.ESSENTIAL.name
        if (discretionaryKeywords.any { lower.contains(it) }) return SpendingClass.DISCRETIONARY.name
        
        if (!gemmaSession.isModelLoaded()) return SpendingClass.UNCLASSIFIED.name

        val prompt = """
            You are a spending classifier for an Indian personal finance app.
            Classify this merchant as either ESSENTIAL or DISCRETIONARY spending.
            
            ESSENTIAL = groceries, medicine, transport, utilities, rent, education fees, hospital
            DISCRETIONARY = restaurants, food delivery, shopping, entertainment, subscriptions, salon
            
            Merchant name: "$merchantName"
            
            Respond with ONLY a JSON object, nothing else:
            {"classification": "ESSENTIAL"} or {"classification": "DISCRETIONARY"}
        """.trimIndent()

        return try {
            val response = gemmaSession.generate(prompt, maxTokens = 20)
            val json = JSONObject(response.trim())
            when (json.getString("classification")) {
                "ESSENTIAL" -> SpendingClass.ESSENTIAL.name
                "DISCRETIONARY" -> SpendingClass.DISCRETIONARY.name
                else -> SpendingClass.UNCLASSIFIED.name
            }
        } catch (e: Exception) {
            SpendingClass.UNCLASSIFIED.name
        }
    }
}

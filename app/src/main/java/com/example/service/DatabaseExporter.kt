package com.example.service

import com.example.data.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64

class DatabaseExporter(
    private val ledgerRepo: LedgerRepository,
    private val configRepo: EconomyConfigRepository
) {
    suspend fun exportToEncryptedString(): String {
        val root = JSONObject()
        
        // Export config
        val configs = configRepo.allConfigs.first()
        val configArr = JSONArray()
        configs.forEach { 
            val obj = JSONObject()
            obj.put("key", it.key)
            obj.put("valueInt", it.valueInt ?: JSONObject.NULL)
            obj.put("valuePaise", it.valuePaise ?: JSONObject.NULL)
            obj.put("valueString", it.valueString ?: JSONObject.NULL)
            obj.put("valueBool", it.valueBool ?: JSONObject.NULL)
            configArr.put(obj)
        }
        root.put("configs", configArr)

        // Export a snapshot of the ledger (e.g. recent 100 entries to prevent massive size)
        val ledger = ledgerRepo.balanceFlow.first()
        root.put("currentBalancePaise", ledger)

        val rawJson = root.toString()
        // Simulate encryption with Base64
        return Base64.encodeToString(rawJson.toByteArray(), Base64.DEFAULT)
    }
}

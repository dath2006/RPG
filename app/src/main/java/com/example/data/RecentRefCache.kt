package com.example.data

class RecentRefCache(private val maxSize: Int = 200) {
    private val refs = mutableListOf<String>()

    @Synchronized
    fun contains(refId: String): Boolean {
        return refs.contains(refId)
    }

    @Synchronized
    fun add(refId: String) {
        if (!refs.contains(refId)) {
            refs.add(refId)
            if (refs.size > maxSize) {
                refs.removeAt(0)
            }
        }
    }
}

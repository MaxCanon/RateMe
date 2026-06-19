package com.example.rateme.data

import android.content.Context
import android.content.SharedPreferences

object SearchHistory {
    private const val PREFS_NAME = "search_history"
    private const val KEY_HISTORY = "history"
    private const val MAX_ITEMS = 10

    fun getHistory(context: Context): List<String> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            json.split("|||").filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addToHistory(context: Context, query: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = getHistory(context).toMutableList()
        history.remove(query)
        history.add(0, query)
        if (history.size > MAX_ITEMS) history.removeAt(history.lastIndex)
        prefs.edit().putString(KEY_HISTORY, history.joinToString("|||")).apply()
    }

    fun clearHistory(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
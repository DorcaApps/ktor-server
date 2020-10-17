package com.dorcaapps.android.ktor.datapersistence

enum class OrderType {
    MOST_RECENT_FIRST,
    MOST_RECENT_LAST;

    companion object {
        fun getWithDefault(value: String?) =
            if (value == null) MOST_RECENT_FIRST
            else values().singleOrNull { it.name == value }
    }
}
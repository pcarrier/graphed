package com.pcarrier.graphed.graphql

enum class OperationType(val repr: String) {
    QUERY("query"),
    MUTATION("mutation"),
    SUBSCRIPTION("subscription");

    companion object {
        fun fromString(str: String): OperationType {
            return when (str) {
                "query" -> QUERY
                "mutation" -> MUTATION
                "subscription" -> SUBSCRIPTION
                else -> throw IllegalArgumentException("Unknown operation type $str")
            }
        }
    }
}

package com.example.timecraft.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class WorkDay(
    val id: String? = null,
    val userId: String,
    val date: LocalDate,
    val isWorked: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val clients: List<String> = emptyList()
)

@Serializable
data class Expense(
    val id: String? = null,
    val amount: Double,
    val description: String,
    val category: String
)

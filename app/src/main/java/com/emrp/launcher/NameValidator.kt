package com.emrp.launcher

object NameValidator {
    // Firstname_Lastname; ASCII letters only; exactly one underscore.
    private val pattern = Regex("^[A-Za-z]+_[A-Za-z]+$")
    fun isValid(value: String): Boolean = pattern.matches(value)
}

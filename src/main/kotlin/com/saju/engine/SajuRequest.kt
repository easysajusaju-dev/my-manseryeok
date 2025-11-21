package com.saju.engine

data class SajuRequest(
    val yearStem: String,
    val yearBranch: String,
    val monthStem: String,
    val monthBranch: String,
    val dayStem: String,
    val dayBranch: String,
    val hourStem: String,
    val hourBranch: String,
    val gender: String,
    val birth: String,
    val solarTerms: List<SolarTerm>
)

data class SolarTerm(
    val name: String,
    val date: String,
    val isPrincipal: Boolean
)

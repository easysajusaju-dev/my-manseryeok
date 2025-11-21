package com.saju.engine

data class SajuResponse(
    val ganji: Map<String, String>,
    val sibsung: Map<String, String>,
    val branchSibsung: Map<String, String>,
    val twelve: Map<String, String>,
    val daewoon: DaewoonResult,
    val relations: Relations,
    val seun: SeunResult,
    val wolun: WolunResult
)

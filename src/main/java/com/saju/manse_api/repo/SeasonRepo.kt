package com.saju.manse_api.repo

import com.saju.manse_api.db.DbProvider
import java.time.LocalDateTime

data class SeasonPoint(val name: String, val dt: LocalDateTime)

object SeasonRepo {

    // -----------------------------------------------------
    // 내부 공통: 특정 년도들의 절기 로드
    // -----------------------------------------------------
    private fun selectYears(vararg years: Int): List<SeasonPoint> {
        val placeholders = years.joinToString(",") { "?" }
        val sql = """
            SELECT NAME, YEAR, MONTH, DAY, HOUR, MINUTE
            FROM SEASON
            WHERE YEAR IN ($placeholders)
            ORDER BY YEAR, MONTH, DAY, HOUR, MINUTE
        """.trimIndent()

        DbProvider.seasonConn().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                years.forEachIndexed { i, y -> ps.setInt(i + 1, y) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<SeasonPoint>()
                    while (rs.next()) {
                        list += SeasonPoint(
                            rs.getString("NAME"),
                            LocalDateTime.of(
                                rs.getInt("YEAR"),
                                rs.getInt("MONTH"),
                                rs.getInt("DAY"),
                                rs.getInt("HOUR"),
                                rs.getInt("MINUTE")
                            )
                        )
                    }
                    return list
                }
            }
        }
    }

    // -----------------------------------------------------
    // ✨ 정절기만 포함한 전체 리스트 생성 (안정 버전)
    // -----------------------------------------------------
    private val PRINCIPAL_TERMS = setOf(
        "입춘", "경칩", "청명",
        "입하", "망종", "소서",
        "입추", "백로", "한로",
        "입동", "대설", "소한"
    )

    private fun filterPrincipal(list: List<SeasonPoint>): List<SeasonPoint> =
        list.filter { it.name in PRINCIPAL_TERMS }

    // -----------------------------------------------------
    // ✨ 무한 루프 없는 nextAfter
    // -----------------------------------------------------
    fun nextAfter(y: Int, m: Int, d: Int, h: Int): SeasonPoint {
        val base = LocalDateTime.of(y, m, d, h, 0)

        // 현재 년도 + 이후 5년까지 로딩
        val years = (y..(y + 5)).toList().toTypedArray()

        val full = selectYears(*years)
        val principals = filterPrincipal(full)

        // base 이후 첫 정절기
        return principals.firstOrNull { it.dt.isAfter(base) }
            ?: principals.last() // 없으면 마지막 정절기라도 반환 (무한루프 방지)
    }

    // -----------------------------------------------------
    // ✨ 무한 루프 없는 prevBefore
    // -----------------------------------------------------
    fun prevBefore(y: Int, m: Int, d: Int, h: Int): SeasonPoint {
        val base = LocalDateTime.of(y, m, d, h, 0)

        // 과거 5년~현재까지 로딩
        val years = ((y - 5)..y).toList().toTypedArray()

        val full = selectYears(*years)
        val principals = filterPrincipal(full)

        // base 이전 마지막 정절기
        return principals.lastOrNull { it.dt.isBefore(base) }
            ?: principals.first() // 없으면 첫 정절기 반환
    }

    // -----------------------------------------------------
    // 현재 기준 절기 (가장 가까운 과거 절기)
    // -----------------------------------------------------
    fun currentAt(y: Int, m: Int, d: Int, h: Int): SeasonPoint {
        val base = LocalDateTime.of(y, m, d, h, 0)
        val years = ((y - 1)..(y + 1)).toList().toTypedArray()

        val full = selectYears(*years)
        val principals = filterPrincipal(full)

        return principals.lastOrNull { !it.dt.isAfter(base) }
            ?: principals.first()
    }
}

package com.saju.manse_api.repo

import com.saju.manse_api.db.DbProvider
import java.time.LocalDateTime

data class SeasonPoint(val name: String, val dt: LocalDateTime)

object SeasonRepo {

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

    // 기준 시각 이후 첫 절기
    fun nextAfter(y: Int, m: Int, d: Int, h: Int): SeasonPoint {
        val base = LocalDateTime.of(y, m, d, h, 0)
        return (selectYears(y, y + 1, y + 2)).firstOrNull { it.dt.isAfter(base) }
            ?: selectYears(y + 2, y + 3).first { it.dt.isAfter(base) }
    }

    // 기준 시각 이전 마지막 절기
    fun prevBefore(y: Int, m: Int, d: Int, h: Int): SeasonPoint {
        val base = LocalDateTime.of(y, m, d, h, 0)
        return (selectYears(y - 2, y - 1, y)).lastOrNull { it.dt.isBefore(base) }
            ?: selectYears(y - 3, y - 2).last { it.dt.isBefore(base) }
    }

    // 기준 시각이 속한 '현재 절기' (가장 가까운 과거 절기)
    fun currentAt(y: Int, m: Int, d: Int, h: Int): SeasonPoint {
        val base = LocalDateTime.of(y, m, d, h, 0)
        val list = selectYears(y - 1, y, y + 1)
        return list.lastOrNull { !it.dt.isAfter(base) } ?: list.first()
    }
}

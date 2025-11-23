package com.saju.manse_api.repo

import com.saju.manse_api.db.DbProvider
import java.time.LocalDateTime

data class SeasonPoint(val name: String, val dt: LocalDateTime)

object SeasonRepo {

    // 정절기(대운수 기준 절기)
    private val PRINCIPAL_TERMS = setOf(
        "입춘", "경칩", "청명",
        "입하", "망종", "소서",
        "입추", "백로", "한로",
        "입동", "대설", "소한"
    )

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

    /** 기준 시각 이후 첫 정절기 */
    fun nextAfter(y: Int, m: Int, d: Int, h: Int): SeasonPoint {
        val base = LocalDateTime.of(y, m, d, h, 0)

        // 3년치 절기 로드 → 정절기만 필터링
        val list = selectYears(y, y + 1, y + 2)
            .filter { it.name in PRINCIPAL_TERMS }

        return list.firstOrNull { it.dt.isAfter(base) }
            ?: run {
                val nextList = selectYears(y + 2, y + 3)
                    .filter { it.name in PRINCIPAL_TERMS }

                nextList.first { it.dt.isAfter(base) }
            }
    }

    /** 기준 시각 이전 마지막 정절기 */
    fun prevBefore(y: Int, m: Int, d: Int, h: Int): SeasonPoint {
        val base = LocalDateTime.of(y, m, d, h, 0)

        val list = selectYears(y - 2, y - 1, y)
            .filter { it.name in PRINCIPAL_TERMS }

        return list.lastOrNull { it.dt.isBefore(base) }
            ?: run {
                val prevList = selectYears(y - 3, y - 2)
                    .filter { it.name in PRINCIPAL_TERMS }

                prevList.last { it.dt.isBefore(base) }
            }
    }

    /** 기준 시각과 가장 가까운 ‘현재 정절기’ */
    fun currentAt(y: Int, m: Int, d: Int, h: Int): SeasonPoint {
        val base = LocalDateTime.of(y, m, d, h, 0)

        val list = selectYears(y - 1, y, y + 1)
            .filter { it.name in PRINCIPAL_TERMS }

        return list.lastOrNull { !it.dt.isAfter(base) } ?: list.first()
    }
}

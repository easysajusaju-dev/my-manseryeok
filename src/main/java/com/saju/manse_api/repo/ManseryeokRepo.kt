package com.saju.manse_api.repo

import com.saju.manse_api.db.DbProvider

data class DayInfo(
    val sy: Int,
    val sm: Int,
    val sd: Int,
    val hy: String, // 년주(한자)
    val hm: String, // 월주(한자)
    val hd: String // 일주(한자)
)

object ManseryeokRepo {

    // 양력으로 찾기
    fun findBySolar(sy: Int, sm: Int, sd: Int): DayInfo? {
        val sql = """
        SELECT cd_sy, cd_sm, cd_sd, cd_hyganjee, cd_hmganjee, cd_hdganjee
        FROM manseryeok
        WHERE cd_sy=? AND cd_sm=? AND cd_sd=?
        LIMIT 1
    """.trimIndent()
        DbProvider.manseConn().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setInt(1, sy)
                ps.setInt(2, sm)
                ps.setInt(3, sd)
                ps.executeQuery().use { rs ->
                    return if (rs.next()) DayInfo(
                        rs.getInt("cd_sy"),
                        rs.getInt("cd_sm"),
                        rs.getInt("cd_sd"),
                        rs.getString("cd_hyganjee"),
                        rs.getString("cd_hmganjee"),
                        rs.getString("cd_hdganjee")
                    ) else null
                }
            }
        }
    }

    // 음력으로 찾기 (윤달 여부 포함)
    fun findByLunar(ly: Int, lm: Int, ld: Int, leap: Boolean): DayInfo? {
        val sql = """
        SELECT cd_sy, cd_sm, cd_sd, cd_hyganjee, cd_hmganjee, cd_hdganjee
        FROM manseryeok
        WHERE cd_ly=? AND cd_lm=? AND cd_ld=? AND cd_leap_month=?
        LIMIT 1
    """.trimIndent()
        DbProvider.manseConn().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setInt(1, ly)
                ps.setInt(2, lm)
                ps.setInt(3, ld)
                ps.setInt(4, if (leap) 1 else 0)
                ps.executeQuery().use { rs ->
                    return if (rs.next()) DayInfo(
                        rs.getInt("cd_sy"),
                        rs.getInt("cd_sm"),
                        rs.getInt("cd_sd"),
                        rs.getString("cd_hyganjee"),
                        rs.getString("cd_hmganjee"),
                        rs.getString("cd_hdganjee")
                    ) else null
                }
            }
        }
    }
}
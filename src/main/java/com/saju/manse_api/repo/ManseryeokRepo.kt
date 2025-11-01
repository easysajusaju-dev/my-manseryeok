package com.saju.manse_api.repo

import com.saju.manse_api.db.DbProvider

// DB에서 뽑아오는 하루 정보(양력/음력 + 간지)
data class DayInfo(
    val sy: Int, val sm: Int, val sd: Int,                // 양력
    val ly: Int, val lm: Int, val ld: Int, val leap: Boolean, // 음력
    val hy: String, val hm: String, val hd: String        // 년/월/일 간지(한자)
)

object ManseryeokRepo {

    // 양력으로 조회
    fun findBySolar(sy: Int, sm: Int, sd: Int): DayInfo? {
        val sql = """
            SELECT cd_sy, cd_sm, cd_sd,
                   cd_ly, cd_lm, cd_ld, cd_leap_month,
                   cd_hyganjee, cd_hmganjee, cd_hdganjee
            FROM manseryeok
            WHERE cd_sy=? AND cd_sm=? AND cd_sd=?
            LIMIT 1
        """.trimIndent()
        DbProvider.manseConn().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setInt(1, sy); ps.setInt(2, sm); ps.setInt(3, sd)
                ps.executeQuery().use { rs ->
                    return if (rs.next()) DayInfo(
                        rs.getInt("cd_sy"),
                        rs.getInt("cd_sm"),
                        rs.getInt("cd_sd"),
                        rs.getInt("cd_ly"),
                        rs.getInt("cd_lm"),
                        rs.getInt("cd_ld"),
                        rs.getInt("cd_leap_month") == 1,
                        rs.getString("cd_hyganjee"),
                        rs.getString("cd_hmganjee"),
                        rs.getString("cd_hdganjee"),
                    ) else null
                }
            }
        }
    }

    // 음력으로 조회(윤달 여부 포함)
    fun findByLunar(ly: Int, lm: Int, ld: Int, leap: Boolean): DayInfo? {
        val sql = """
            SELECT cd_sy, cd_sm, cd_sd,
                   cd_ly, cd_lm, cd_ld, cd_leap_month,
                   cd_hyganjee, cd_hmganjee, cd_hdganjee
            FROM manseryeok
            WHERE cd_ly=? AND cd_lm=? AND cd_ld=? AND cd_leap_month=?
            LIMIT 1
        """.trimIndent()
        DbProvider.manseConn().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setInt(1, ly); ps.setInt(2, lm); ps.setInt(3, ld); ps.setInt(4, if (leap) 1 else 0)
                ps.executeQuery().use { rs ->
                    return if (rs.next()) DayInfo(
                        rs.getInt("cd_sy"),
                        rs.getInt("cd_sm"),
                        rs.getInt("cd_sd"),
                        rs.getInt("cd_ly"),
                        rs.getInt("cd_lm"),
                        rs.getInt("cd_ld"),
                        rs.getInt("cd_leap_month") == 1,
                        rs.getString("cd_hyganjee"),
                        rs.getString("cd_hmganjee"),
                        rs.getString("cd_hdganjee"),
                    ) else null
                }
            }
        }
    }
}

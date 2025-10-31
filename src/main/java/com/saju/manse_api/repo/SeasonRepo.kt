package com.saju.manse_api.repo

import com.saju.manse_api.db.DbProvider
import java.time.LocalDateTime

data class SeasonPoint(val name:String, val dt: LocalDateTime)

object SeasonRepo {


    private fun rowsForYears(y1:Int, y2:Int): List<SeasonPoint> {
        val sql = "SELECT NAME, YEAR, MONTH, DAY, HOUR, MINUTE FROM SEASON WHERE YEAR IN (?, ?) ORDER BY YEAR, MONTH, DAY, HOUR, MINUTE"
        DbProvider.seasonConn().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setInt(1, y1)
                ps.setInt(2, y2)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<SeasonPoint>()
                    while (rs.next()) {
                        val name = rs.getString("NAME")
                        val y = rs.getInt("YEAR")
                        val m = rs.getInt("MONTH")
                        val d = rs.getInt("DAY")
                        val h = rs.getInt("HOUR")
                        val min = rs.getInt("MINUTE")
                        list.add(SeasonPoint(name, LocalDateTime.of(y, m, d, h, min)))
                    }
                    return list
                }
            }
        }
    }

    fun nextAfter(y:Int, m:Int, d:Int, h:Int): SeasonPoint {
        val birth = LocalDateTime.of(y, m, d, h, 0)
        val list = rowsForYears(y, y+1)
        return list.firstOrNull { it.dt.isAfter(birth) }
            ?: rowsForYears(y+1, y+2).first { it.dt.isAfter(birth) }
    }

    fun prevBefore(y:Int, m:Int, d:Int, h:Int): SeasonPoint {
        val birth = LocalDateTime.of(y, m, d, h, 0)
        val list = rowsForYears(y-1, y)
        return list.lastOrNull { it.dt.isBefore(birth) }
            ?: rowsForYears(y-2, y-1).last { it.dt.isBefore(birth) }
    }
}
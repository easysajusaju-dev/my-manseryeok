package com.saju.manse_api.db

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.DriverManager

object DbProvider {
    // resources 내부 파일을 임시파일로 복사해서 SQLite가 읽을 수 있게 함
    private fun copyResourceToTemp(pathInResources: String): String {
        val ins = DbProvider::class.java.classLoader.getResourceAsStream(pathInResources)
            ?: throw IllegalArgumentException("resource not found: $pathInResources")
        val tmp = File.createTempFile("sqlite-", ".db")
        tmp.deleteOnExit()
        ins.use { input ->
            Files.copy(input, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        return tmp.absolutePath
    }

    fun manseConn(): Connection {
        val path = copyResourceToTemp("db/Manseryeok.db")
        return DriverManager.getConnection("jdbc:sqlite:$path")
    }

    fun seasonConn(): Connection {
        val path = copyResourceToTemp("db/season-24.db")
        return DriverManager.getConnection("jdbc:sqlite:$path")
    }
}
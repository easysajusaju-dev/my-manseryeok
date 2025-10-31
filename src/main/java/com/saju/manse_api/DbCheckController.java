package com.saju.manse_api;

import com.saju.manse_api.db.DbProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DbCheckController {


    @GetMapping("/db/tables")
    public Map<String, List<String>> tables() throws Exception {
        Map<String, List<String>> out = new LinkedHashMap<>();
        out.put("Manseryeok.db", listTables(DbProvider.INSTANCE.manseConn()));
        out.put("season-24.db", listTables(DbProvider.INSTANCE.seasonConn()));
        return out;
    }

    @GetMapping("/db/columns")
    public Map<String, List<Map<String, Object>>> columns() throws Exception {
        Map<String, List<Map<String, Object>>> out = new LinkedHashMap<>();
        out.put("Manseryeok.manseryeok", tableInfo(DbProvider.INSTANCE.manseConn(), "manseryeok"));
        out.put("season-24.SEASON", tableInfo(DbProvider.INSTANCE.seasonConn(), "SEASON"));
        return out;
    }

    @GetMapping("/db/sample")
    public Map<String, List<Map<String, Object>>> sample() throws Exception {
        Map<String, List<Map<String, Object>>> out = new LinkedHashMap<>();
        out.put("Manseryeok.manseryeok", firstRows(DbProvider.INSTANCE.manseConn(), "manseryeok", 5));
        out.put("season-24.SEASON", firstRows(DbProvider.INSTANCE.seasonConn(), "SEASON", 5));
        return out;
    }

    private List<String> listTables(Connection conn) throws Exception {
        try (conn) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")) {
                List<String> list = new ArrayList<>();
                while (rs.next()) list.add(rs.getString(1));
                return list;
            }
        }
    }

    private List<Map<String, Object>> tableInfo(Connection conn, String table) throws Exception {
        try (conn) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("PRAGMA table_info('" + table + "')")) {
                List<Map<String, Object>> list = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("cid", rs.getInt("cid"));
                    row.put("name", rs.getString("name"));
                    row.put("type", rs.getString("type"));
                    row.put("notnull", rs.getInt("notnull"));
                    row.put("pk", rs.getInt("pk"));
                    list.add(row);
                }
                return list;
            }
        }
    }

    private List<Map<String, Object>> firstRows(Connection conn, String table, int limit) throws Exception {
        try (conn) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM '" + table + "' LIMIT " + limit)) {
                List<Map<String, Object>> list = new ArrayList<>();
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(md.getColumnName(i), rs.getObject(i));
                    }
                    list.add(row);
                }
                return list;
            }
        }
    }
}
package com.scc.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseInitializer {
    public static void initializeDatabase() {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "roles TEXT NOT NULL, " +
                "status TEXT DEFAULT 'PENDING');";

        String sqlActivityLogs = "CREATE TABLE IF NOT EXISTS activity_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "action_type TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "timestamp TEXT NOT NULL DEFAULT (datetime('now','localtime')), " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE);";

        String sqlFuelSales = "CREATE TABLE IF NOT EXISTS fuel_sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "staff_id INTEGER NOT NULL, " +
                "fuel_type TEXT NOT NULL, " +
                "liters REAL NOT NULL, " +
                "price_per_liter REAL NOT NULL, " +
                "total_amount REAL NOT NULL, " +
                "status TEXT NOT NULL, " +
                "created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')), " +
                "FOREIGN KEY (staff_id) REFERENCES users(id) ON DELETE CASCADE);";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute(sqlUsers);
            repairOutdatedTables(stmt);
            stmt.execute(sqlActivityLogs);
            stmt.execute(sqlFuelSales);
            System.out.println("[DatabaseInitializer] -> Tables initialized successfully!");
            System.out.println("[DatabaseInitializer] -> Using database file: "
                    + new java.io.File("gasusersrecords.db").getAbsolutePath());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void repairOutdatedTables(Statement stmt) throws SQLException {
        List<String> userCols = columnsOf(stmt, "users");
        if (userCols.contains("role") && !userCols.contains("roles")) {
            System.out.println("[DatabaseInitializer] -> Renaming users.role to users.roles.");
            stmt.execute("ALTER TABLE users RENAME COLUMN role TO roles");
        }

        List<String> logCols = columnsOf(stmt, "activity_logs");
        if (!logCols.isEmpty()
                && !logCols.containsAll(List.of("id", "user_id", "action_type", "description", "timestamp"))) {
            System.out.println("[DatabaseInitializer] -> Outdated activity_logs table found; rebuilding it.");
            stmt.execute("DROP TABLE activity_logs");
        }
    }

    private static List<String> columnsOf(Statement stmt, String table) throws SQLException {
        List<String> cols = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                cols.add(rs.getString("name").toLowerCase());
            }
        }
        return cols;
    }
}
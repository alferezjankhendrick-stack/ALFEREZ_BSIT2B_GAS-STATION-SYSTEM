package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Creates every table on startup, upgrades older database files and seeds starter data. */
public class DatabaseInitializer {

    private static final String SQL_USERS = "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "username TEXT UNIQUE NOT NULL, " +
            "password TEXT NOT NULL, " +
            "roles TEXT NOT NULL, " +
            "status TEXT NOT NULL DEFAULT 'PENDING');";

    private static final String SQL_ACTIVITY_LOGS = "CREATE TABLE IF NOT EXISTS activity_logs (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER NOT NULL, " +
            "action_type TEXT NOT NULL, " +
            "description TEXT NOT NULL, " +
            "timestamp TEXT NOT NULL DEFAULT (datetime('now','localtime')), " +
            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE);";

    private static final String SQL_FUEL_TYPES = "CREATE TABLE IF NOT EXISTS fuel_types (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT UNIQUE NOT NULL, " +
            "price_per_liter REAL NOT NULL CHECK (price_per_liter > 0), " +
            "stock_liters REAL NOT NULL DEFAULT 0 CHECK (stock_liters >= 0), " +
            "capacity_liters REAL NOT NULL CHECK (capacity_liters > 0), " +
            "reorder_level REAL NOT NULL DEFAULT 0, " +
            "CHECK (stock_liters <= capacity_liters));";

    private static final String SQL_FUEL_SALES = "CREATE TABLE IF NOT EXISTS fuel_sales (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "staff_id INTEGER NOT NULL, " +
            "customer_id INTEGER, " +
            "fuel_type_id INTEGER NOT NULL, " +
            "liters REAL NOT NULL CHECK (liters > 0), " +
            "price_per_liter REAL NOT NULL, " +
            "total_amount REAL NOT NULL, " +
            "status TEXT NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED','VOIDED')), " +
            "created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')), " +
            "FOREIGN KEY (staff_id) REFERENCES users(id) ON DELETE RESTRICT, " +
            "FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE SET NULL, " +
            "FOREIGN KEY (fuel_type_id) REFERENCES fuel_types(id));";

    private static final String SQL_FUEL_DELIVERIES = "CREATE TABLE IF NOT EXISTS fuel_deliveries (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "fuel_type_id INTEGER NOT NULL, " +
            "liters REAL NOT NULL CHECK (liters > 0), " +
            "supplier TEXT NOT NULL, " +
            "received_by INTEGER NOT NULL, " +
            "created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')), " +
            "FOREIGN KEY (fuel_type_id) REFERENCES fuel_types(id), " +
            "FOREIGN KEY (received_by) REFERENCES users(id) ON DELETE RESTRICT);";

    private static final String SQL_SHIFT_REPORTS = "CREATE TABLE IF NOT EXISTS shift_reports (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "staff_id INTEGER NOT NULL, " +
            "shift_date TEXT NOT NULL, " +
            "sales_count INTEGER NOT NULL, " +
            "total_liters REAL NOT NULL, " +
            "expected_amount REAL NOT NULL, " +
            "cash_collected REAL NOT NULL CHECK (cash_collected >= 0), " +
            "variance REAL NOT NULL, " +
            "created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')), " +
            "FOREIGN KEY (staff_id) REFERENCES users(id) ON DELETE RESTRICT);";

    public static void initializeDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQL_USERS);
            repairOutdatedTables(stmt);
            stmt.execute(SQL_ACTIVITY_LOGS);
            stmt.execute(SQL_FUEL_TYPES);
            seedFuelTypes(conn);
            migrateLegacyFuelSales(conn, stmt);
            stmt.execute(SQL_FUEL_SALES);
            stmt.execute(SQL_FUEL_DELIVERIES);
            stmt.execute(SQL_SHIFT_REPORTS);
            seedDefaultAdmin(conn);

            System.out.println("[DatabaseInitializer] -> Tables initialized successfully!");
            System.out.println("[DatabaseInitializer] -> Using database file: " + DatabaseConnection.databasePath());
        } catch (SQLException e) {
            System.err.println("[DatabaseInitializer] -> Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedDefaultAdmin(Connection conn) throws SQLException {
        String check = "SELECT COUNT(*) FROM users WHERE username = ?";
        String insert = "INSERT INTO users (username, password, roles, status) VALUES (?, ?, 'ADMIN', 'APPROVED')";
        try (PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setString(1, "admin");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, "admin");
            ps.setString(2, "admin123");
            ps.executeUpdate();
            System.out.println("[DatabaseInitializer] -> Default admin created (admin / admin123).");
        }
    }

    private static void seedFuelTypes(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM fuel_types")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }
        String insert = "INSERT INTO fuel_types (name, price_per_liter, stock_liters, capacity_liters, reorder_level) "
                + "VALUES (?, ?, ?, ?, ?)";
        Object[][] seed = {
                {"DIESEL", 58.50, 5000.0, 10000.0, 1000.0},
                {"UNLEADED", 62.00, 6000.0, 10000.0, 1000.0},
                {"PREMIUM", 66.50, 4000.0, 8000.0, 800.0}
        };
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            for (Object[] row : seed) {
                ps.setString(1, (String) row[0]);
                ps.setDouble(2, (Double) row[1]);
                ps.setDouble(3, (Double) row[2]);
                ps.setDouble(4, (Double) row[3]);
                ps.setDouble(5, (Double) row[4]);
                ps.executeUpdate();
            }
        }
        System.out.println("[DatabaseInitializer] -> Default fuel types and tank stock created.");
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

    private static void migrateLegacyFuelSales(Connection conn, Statement stmt) throws SQLException {
        List<String> cols = columnsOf(stmt, "fuel_sales");
        if (cols.isEmpty() || cols.contains("fuel_type_id")) {
            return;
        }
        System.out.println("[DatabaseInitializer] -> Upgrading old fuel_sales table (keeping existing rows).");
        conn.setAutoCommit(false);
        try {
            stmt.execute("ALTER TABLE fuel_sales RENAME TO fuel_sales_legacy");
            stmt.execute(SQL_FUEL_SALES);
            stmt.execute("INSERT INTO fuel_sales (id, staff_id, fuel_type_id, liters, price_per_liter, "
                    + "total_amount, status, created_at) "
                    + "SELECT l.id, l.staff_id, ft.id, l.liters, l.price_per_liter, l.total_amount, l.status, l.created_at "
                    + "FROM fuel_sales_legacy l JOIN fuel_types ft ON ft.name = l.fuel_type");
            stmt.execute("DROP TABLE fuel_sales_legacy");
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
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
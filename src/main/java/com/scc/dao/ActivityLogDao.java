package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    private static final String SELECT_LOGS =
            "SELECT a.id, a.user_id, COALESCE(u.username, 'unknown') AS username, "
                    + "a.action_type, a.description, a.timestamp "
                    + "FROM activity_logs a LEFT JOIN users u ON u.id = a.user_id ";


    public boolean logAction(int userId, String actionType, String description) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return logAction(conn, userId, actionType, description);
        } catch (SQLException e) {
            System.err.println("Error writing activity log: " + e.getMessage());
        }
        return false;
    }

    public boolean logAction(Connection conn, int userId, String actionType, String description)
            throws SQLException {
        String sql = "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, actionType);
            pstmt.setString(3, description);
            boolean saved = pstmt.executeUpdate() > 0;
            if (saved) {
                System.out.println("[ActivityLog] Saved: " + actionType + " (user " + userId + ")");
            }
            return saved;
        }
    }

    public List<ActivityLog> getAllLogs() {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = SELECT_LOGS + "ORDER BY a.timestamp DESC, a.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                logs.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching activity logs: " + e.getMessage());
        }
        return logs;
    }

    public List<ActivityLog> getLogsByUserId(int userId) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = SELECT_LOGS + "WHERE a.user_id = ? ORDER BY a.timestamp DESC, a.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching logs for user " + userId + ": " + e.getMessage());
        }
        return logs;
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        return new ActivityLog(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("timestamp")
        );
    }
}
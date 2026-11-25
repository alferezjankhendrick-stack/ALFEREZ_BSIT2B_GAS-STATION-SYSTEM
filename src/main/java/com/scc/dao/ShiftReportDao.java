package com.scc.dao;

import com.scc.model.OperationResult;
import com.scc.model.ShiftReport;
import com.scc.util.MoneyUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ShiftReportDao {
    private static final String TODAY_TOTALS =
            "SELECT COUNT(*), COALESCE(SUM(liters), 0), COALESCE(SUM(total_amount), 0), date('now','localtime') "
                    + "FROM fuel_sales WHERE staff_id = ? AND status = 'COMPLETED' "
                    + "AND date(created_at) = date('now','localtime')";

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public ShiftReport getTodaySummary(int staffId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return summarize(conn, staffId, 0);
        } catch (SQLException e) {
            System.err.println("Error loading shift summary: " + e.getMessage());
        }
        return null;
    }

    public OperationResult recordShift(int staffId, double cashCollected) {
        if (cashCollected < 0) {
            return OperationResult.fail("Cash collected cannot be negative.");
        }
        String insertSql = "INSERT INTO shift_reports (staff_id, shift_date, sales_count, total_liters, "
                + "expected_amount, cash_collected, variance) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            ShiftReport s = summarize(conn, staffId, MoneyUtil.round2(cashCollected));
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, staffId);
                ps.setString(2, s.shiftDate());
                ps.setInt(3, s.salesCount());
                ps.setDouble(4, s.totalLiters());
                ps.setDouble(5, s.expectedAmount());
                ps.setDouble(6, s.cashCollected());
                ps.setDouble(7, s.variance());
                ps.executeUpdate();
            }
            int id = DatabaseConnection.lastInsertId(conn);
            activityLogDao.logAction(conn, staffId, "SHIFT_RECORDED",
                    String.format("Shift report #%d - expected P%.2f, collected P%.2f, variance P%.2f",
                            id, s.expectedAmount(), s.cashCollected(), s.variance()));
            return OperationResult.ok(String.format(
                    "Shift report #%d saved. Sales: %d | Liters: %.2f | Expected: P%.2f | Collected: P%.2f | Variance: P%.2f",
                    id, s.salesCount(), s.totalLiters(), s.expectedAmount(), s.cashCollected(), s.variance()), id);
        } catch (SQLException e) {
            System.err.println("Error recording shift: " + e.getMessage());
        }
        return OperationResult.fail("Failed to save the shift report.");
    }

    public List<ShiftReport> getReportsByStaff(int staffId) {
        String sql = "SELECT id, staff_id, shift_date, sales_count, total_liters, expected_amount, cash_collected, "
                + "variance FROM shift_reports WHERE staff_id = ? ORDER BY id DESC";
        List<ShiftReport> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ShiftReport(rs.getInt("id"), rs.getInt("staff_id"), rs.getString("shift_date"),
                            rs.getInt("sales_count"), rs.getDouble("total_liters"), rs.getDouble("expected_amount"),
                            rs.getDouble("cash_collected"), rs.getDouble("variance")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading shift reports: " + e.getMessage());
        }
        return list;
    }

    private ShiftReport summarize(Connection conn, int staffId, double cash) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(TODAY_TOTALS)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                double expected = MoneyUtil.round2(rs.getDouble(3));
                return new ShiftReport(0, staffId, rs.getString(4), rs.getInt(1),
                        MoneyUtil.round2(rs.getDouble(2)), expected, cash, MoneyUtil.round2(cash - expected));
            }
        }
    }
}
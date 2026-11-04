package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class FuelSaleDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public static double priceFor(String fuelType) {
        return switch (fuelType) {
            case "DIESEL" -> 58.50;
            case "UNLEADED" -> 62.00;
            case "PREMIUM" -> 66.50;
            default -> 0.0;
        };
    }


    public int processSale(int staffId, String fuelType, double liters) {
        String sql = "INSERT INTO fuel_sales (staff_id, fuel_type, liters, price_per_liter, total_amount, status) "
                + "VALUES (?, ?, ?, ?, ?, 'COMPLETED')";
        double price = priceFor(fuelType);
        double total = price * liters;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, staffId);
            pstmt.setString(2, fuelType);
            pstmt.setDouble(3, liters);
            pstmt.setDouble(4, price);
            pstmt.setDouble(5, total);
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int saleId = keys.getInt(1);
                    activityLogDao.logAction(staffId, "FUEL_SALE_PROCESSED",
                            String.format("Processed fuel sale #%d - %.2f L %s - Total: P%.2f",
                                    saleId, liters, fuelType, total));
                    return saleId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error processing fuel sale: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateSaleStatus(int staffId, int saleId, String newStatus) {
        String selectSql = "SELECT status FROM fuel_sales WHERE id = ?";
        String updateSql = "UPDATE fuel_sales SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            String oldStatus;
            try (PreparedStatement sel = conn.prepareStatement(selectSql)) {
                sel.setInt(1, saleId);
                try (ResultSet rs = sel.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    oldStatus = rs.getString("status");
                }
            }

            try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
                upd.setString(1, newStatus.toUpperCase());
                upd.setInt(2, saleId);
                if (upd.executeUpdate() > 0) {
                    activityLogDao.logAction(staffId, "SALE_STATUS_UPDATED",
                            "Updated Sale #" + saleId + " status from " + oldStatus
                                    + " to " + newStatus.toUpperCase());
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating sale status: " + e.getMessage());
        }
        return false;
    }
}
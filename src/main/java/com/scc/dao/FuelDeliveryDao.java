package com.scc.dao;

import com.scc.model.FuelDelivery;
import com.scc.model.OperationResult;
import com.scc.util.MoneyUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FuelDeliveryDao {
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    /** Records a delivery and adds it to tank stock in one transaction, refusing to overfill the tank. */
    public OperationResult logDelivery(int userId, int fuelTypeId, double liters, String supplier) {
        double amount = MoneyUtil.round2(liters);
        if (amount <= 0) {
            return OperationResult.fail("Liters delivered must be greater than 0.");
        }
        String selectSql = "SELECT name, stock_liters, capacity_liters FROM fuel_types WHERE id = ?";
        String insertSql = "INSERT INTO fuel_deliveries (fuel_type_id, liters, supplier, received_by) VALUES (?, ?, ?, ?)";
        String stockSql = "UPDATE fuel_types SET stock_liters = ROUND(stock_liters + ?, 2) WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String name;
                double stock;
                double capacity;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, fuelTypeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return OperationResult.fail("Fuel type not found.");
                        }
                        name = rs.getString("name");
                        stock = rs.getDouble("stock_liters");
                        capacity = rs.getDouble("capacity_liters");
                    }
                }
                if (stock + amount > capacity + 0.0001) {
                    conn.rollback();
                    return OperationResult.fail(String.format(
                            "Delivery rejected: the %s tank only has %.2f L of free space (capacity %.2f L).",
                            name, capacity - stock, capacity));
                }
                int id;
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, fuelTypeId);
                    ps.setDouble(2, amount);
                    ps.setString(3, supplier);
                    ps.setInt(4, userId);
                    ps.executeUpdate();
                    id = DatabaseConnection.lastInsertId(conn);
                }
                try (PreparedStatement ps = conn.prepareStatement(stockSql)) {
                    ps.setDouble(1, amount);
                    ps.setInt(2, fuelTypeId);
                    ps.executeUpdate();
                }
                activityLogDao.logAction(conn, userId, "FUEL_DELIVERY_LOGGED",
                        String.format("Delivery #%d - %.2f L %s from %s. New stock: %.2f L.",
                                id, amount, name, supplier, stock + amount));
                conn.commit();
                return OperationResult.ok(String.format("Delivery #%d recorded. %s stock is now %.2f L.",
                        id, name, stock + amount), id);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error logging delivery: " + e.getMessage());
            return OperationResult.fail("Database error while logging the delivery.");
        }
    }

    public List<FuelDelivery> getRecentDeliveries(int limit) {
        String sql = "SELECT d.id, ft.name AS fuel_name, d.liters, d.supplier, u.username AS receiver, d.created_at "
                + "FROM fuel_deliveries d JOIN fuel_types ft ON ft.id = d.fuel_type_id "
                + "JOIN users u ON u.id = d.received_by ORDER BY d.created_at DESC, d.id DESC LIMIT ?";
        List<FuelDelivery> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new FuelDelivery(rs.getInt("id"), rs.getString("fuel_name"), rs.getDouble("liters"),
                            rs.getString("supplier"), rs.getString("receiver"), rs.getString("created_at")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading deliveries: " + e.getMessage());
        }
        return list;
    }
}
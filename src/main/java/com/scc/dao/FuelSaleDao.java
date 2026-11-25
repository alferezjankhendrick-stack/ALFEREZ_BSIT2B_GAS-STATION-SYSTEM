package com.scc.dao;

import com.scc.model.FuelSale;
import com.scc.model.OperationResult;
import com.scc.util.MoneyUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class FuelSaleDao {
    public static final double PESOS_PER_POINT = 100.0;

    private static final String SELECT_SALES =
            "SELECT s.id, s.staff_id, st.username AS staff_name, s.customer_id, c.username AS customer_name, "
                    + "ft.name AS fuel_name, s.liters, s.price_per_liter, s.total_amount, s.status, s.created_at "
                    + "FROM fuel_sales s "
                    + "JOIN users st ON st.id = s.staff_id "
                    + "LEFT JOIN users c ON c.id = s.customer_id "
                    + "JOIN fuel_types ft ON ft.id = s.fuel_type_id ";

    private final ActivityLogDao activityLogDao = new ActivityLogDao();


    public OperationResult processSale(int staffId, Integer customerId, int fuelTypeId, double liters) {
        double amount = MoneyUtil.round2(liters);
        if (amount <= 0) {
            return OperationResult.fail("Liters must be greater than 0.");
        }
        String selectSql = "SELECT name, price_per_liter, stock_liters, reorder_level FROM fuel_types WHERE id = ?";
        String insertSql = "INSERT INTO fuel_sales (staff_id, customer_id, fuel_type_id, liters, price_per_liter, "
                + "total_amount, status) VALUES (?, ?, ?, ?, ?, ?, 'COMPLETED')";
        String stockSql = "UPDATE fuel_types SET stock_liters = ROUND(stock_liters - ?, 2) WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String name;
                double price;
                double stock;
                double reorder;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, fuelTypeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return OperationResult.fail("Fuel type not found.");
                        }
                        name = rs.getString("name");
                        price = rs.getDouble("price_per_liter");
                        stock = rs.getDouble("stock_liters");
                        reorder = rs.getDouble("reorder_level");
                    }
                }
                if (amount > stock) {
                    conn.rollback();
                    return OperationResult.fail(String.format(
                            "Sale rejected: only %.2f L of %s left in the tank.", stock, name));
                }

                double total = MoneyUtil.round2(price * amount);
                int saleId;
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, staffId);
                    if (customerId == null) {
                        ps.setNull(2, Types.INTEGER);
                    } else {
                        ps.setInt(2, customerId);
                    }
                    ps.setInt(3, fuelTypeId);
                    ps.setDouble(4, amount);
                    ps.setDouble(5, price);
                    ps.setDouble(6, total);
                    ps.executeUpdate();
                    saleId = DatabaseConnection.lastInsertId(conn);
                }
                try (PreparedStatement ps = conn.prepareStatement(stockSql)) {
                    ps.setDouble(1, amount);
                    ps.setInt(2, fuelTypeId);
                    ps.executeUpdate();
                }
                double remaining = MoneyUtil.round2(stock - amount);
                activityLogDao.logAction(conn, staffId, "FUEL_SALE_PROCESSED",
                        String.format("Processed fuel sale #%d - %.2f L %s - Total: P%.2f", saleId, amount, name, total));
                conn.commit();

                String message = String.format("Sale #%d recorded: %.2f L %s x P%.2f = P%.2f. Tank now %.2f L.",
                        saleId, amount, name, price, total, remaining);
                if (remaining <= reorder) {
                    message += String.format("%nWARNING: %s stock is at or below the reorder level (%.2f L).", name, reorder);
                }
                return OperationResult.ok(message, saleId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error processing fuel sale: " + e.getMessage());
            return OperationResult.fail("Database error while processing the sale.");
        }
    }


    public OperationResult updateSaleStatus(int staffId, int saleId, String newStatus) {
        String status = newStatus.toUpperCase();
        if (!status.equals("COMPLETED") && !status.equals("VOIDED")) {
            return OperationResult.fail("Status must be COMPLETED or VOIDED.");
        }
        String selectSql = "SELECT status, fuel_type_id, liters FROM fuel_sales WHERE id = ?";
        String stockSql = "SELECT stock_liters FROM fuel_types WHERE id = ?";
        String addBack = "UPDATE fuel_types SET stock_liters = ROUND(stock_liters + ?, 2) WHERE id = ?";
        String takeOut = "UPDATE fuel_types SET stock_liters = ROUND(stock_liters - ?, 2) WHERE id = ?";
        String updateSql = "UPDATE fuel_sales SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String oldStatus;
                int fuelTypeId;
                double liters;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, saleId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return OperationResult.fail("Sale #" + saleId + " was not found.");
                        }
                        oldStatus = rs.getString("status");
                        fuelTypeId = rs.getInt("fuel_type_id");
                        liters = rs.getDouble("liters");
                    }
                }
                if (oldStatus.equals(status)) {
                    conn.rollback();
                    return OperationResult.fail("Sale #" + saleId + " is already " + status + ".");
                }

                if (status.equals("VOIDED")) {
                    try (PreparedStatement ps = conn.prepareStatement(addBack)) {
                        ps.setDouble(1, liters);
                        ps.setInt(2, fuelTypeId);
                        ps.executeUpdate();
                    }
                } else {
                    double stock;
                    try (PreparedStatement ps = conn.prepareStatement(stockSql)) {
                        ps.setInt(1, fuelTypeId);
                        try (ResultSet rs = ps.executeQuery()) {
                            stock = rs.next() ? rs.getDouble(1) : 0;
                        }
                    }
                    if (liters > stock) {
                        conn.rollback();
                        return OperationResult.fail(String.format(
                                "Cannot complete sale #%d: only %.2f L left in the tank but %.2f L are needed.",
                                saleId, stock, liters));
                    }
                    try (PreparedStatement ps = conn.prepareStatement(takeOut)) {
                        ps.setDouble(1, liters);
                        ps.setInt(2, fuelTypeId);
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, status);
                    ps.setInt(2, saleId);
                    ps.executeUpdate();
                }
                activityLogDao.logAction(conn, staffId, "SALE_STATUS_UPDATED",
                        "Updated Sale #" + saleId + " status from " + oldStatus + " to " + status);
                conn.commit();
                return OperationResult.ok("Sale #" + saleId + " status set to " + status + ".", saleId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error updating sale status: " + e.getMessage());
            return OperationResult.fail("Could not update the sale (the tank may be over capacity).");
        }
    }



    public List<FuelSale> getAllSales() {
        return query(SELECT_SALES + "ORDER BY s.created_at DESC, s.id DESC", null);
    }

    public List<FuelSale> getTodaySales() {
        return query(SELECT_SALES + "WHERE date(s.created_at) = date('now','localtime') "
                + "ORDER BY s.created_at DESC, s.id DESC", null);
    }

    public List<FuelSale> getSalesByCustomer(int customerId) {
        return query(SELECT_SALES + "WHERE s.customer_id = ? ORDER BY s.created_at DESC, s.id DESC", customerId);
    }


    public int getLoyaltyPoints(int customerId) {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM fuel_sales WHERE customer_id = ? AND status = 'COMPLETED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return (int) Math.floor(rs.getDouble(1) / PESOS_PER_POINT);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error computing loyalty points: " + e.getMessage());
        }
        return 0;
    }

    private List<FuelSale> query(String sql, Integer customerId) {
        List<FuelSale> sales = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (customerId != null) {
                ps.setInt(1, customerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int cust = rs.getInt("customer_id");
                    Integer customer = rs.wasNull() ? null : cust;
                    sales.add(new FuelSale(rs.getInt("id"), rs.getInt("staff_id"), rs.getString("staff_name"),
                            customer, rs.getString("customer_name"), rs.getString("fuel_name"),
                            rs.getDouble("liters"), rs.getDouble("price_per_liter"), rs.getDouble("total_amount"),
                            rs.getString("status"), rs.getString("created_at")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading fuel sales: " + e.getMessage());
        }
        return sales;
    }
}
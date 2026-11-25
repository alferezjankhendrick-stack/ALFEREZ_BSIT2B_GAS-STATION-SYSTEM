package com.scc.dao;

import com.scc.model.FuelType;
import com.scc.model.OperationResult;
import com.scc.util.MoneyUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FuelTypeDao {
    private static final String SELECT =
            "SELECT id, name, price_per_liter, stock_liters, capacity_liters, reorder_level FROM fuel_types ";

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public List<FuelType> getAll() {
        List<FuelType> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error loading fuel types: " + e.getMessage());
        }
        return list;
    }

    public FuelType findById(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding fuel type: " + e.getMessage());
        }
        return null;
    }

    public OperationResult updatePrice(int userId, int fuelTypeId, double newPrice) {
        double price = MoneyUtil.round2(newPrice);
        if (price <= 0) {
            return OperationResult.fail("Price must be greater than 0.");
        }
        FuelType fuel = findById(fuelTypeId);
        if (fuel == null) {
            return OperationResult.fail("Fuel type not found.");
        }
        String sql = "UPDATE fuel_types SET price_per_liter = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, price);
            ps.setInt(2, fuelTypeId);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(conn, userId, "FUEL_PRICE_UPDATED",
                        String.format("Changed %s price from P%.2f to P%.2f per liter.",
                                fuel.name(), fuel.pricePerLiter(), price));
                return OperationResult.ok(String.format("%s price updated to P%.2f per liter.", fuel.name(), price));
            }
        } catch (SQLException e) {
            System.err.println("Error updating fuel price: " + e.getMessage());
        }
        return OperationResult.fail("Failed to update the price.");
    }

    private FuelType map(ResultSet rs) throws SQLException {
        return new FuelType(rs.getInt("id"), rs.getString("name"), rs.getDouble("price_per_liter"),
                rs.getDouble("stock_liters"), rs.getDouble("capacity_liters"), rs.getDouble("reorder_level"));
    }
}
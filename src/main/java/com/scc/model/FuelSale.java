package com.scc.model;

public record FuelSale(int id, int staffId, String staffName, Integer customerId, String customerName,
                       String fuelType, double liters, double pricePerLiter, double totalAmount,
                       String status, String createdAt) {
}
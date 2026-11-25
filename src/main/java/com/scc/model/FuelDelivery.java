package com.scc.model;

public record FuelDelivery(int id, String fuelType, double liters, String supplier,
                           String receivedBy, String createdAt) {
}
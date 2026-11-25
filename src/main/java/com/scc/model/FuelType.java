package com.scc.model;

public record FuelType(int id, String name, double pricePerLiter, double stockLiters,
                       double capacityLiters, double reorderLevel) {

    public boolean isLow() {
        return stockLiters <= reorderLevel;
    }

    public double freeSpace() {
        return capacityLiters - stockLiters;
    }
}
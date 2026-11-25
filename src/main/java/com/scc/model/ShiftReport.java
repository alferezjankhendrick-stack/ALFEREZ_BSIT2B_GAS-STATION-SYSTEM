package com.scc.model;

public record ShiftReport(int id, int staffId, String shiftDate, int salesCount, double totalLiters,
                          double expectedAmount, double cashCollected, double variance) {
}
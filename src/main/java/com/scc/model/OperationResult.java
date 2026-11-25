package com.scc.model;

public record OperationResult(boolean success, String message, int id) {
    public static OperationResult ok(String message, int id) {
        return new OperationResult(true, message, id);
    }

    public static OperationResult ok(String message) {
        return new OperationResult(true, message, -1);
    }

    public static OperationResult fail(String message) {
        return new OperationResult(false, message, -1);
    }
}
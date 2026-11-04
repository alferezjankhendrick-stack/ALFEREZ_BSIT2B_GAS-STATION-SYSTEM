package com.scc.model;

public record ActivityLog(
        int id,
        int userId,
        String username,
        String actionType,
        String description,
        String timestamp
) {

    public String action() {
        return actionType + ": " + description;
    }

    @Override
    public String toString() {
        return String.format("[%s] User '%s' (ID: %d) -> Action: %s", timestamp, username, userId, action());
    }
}
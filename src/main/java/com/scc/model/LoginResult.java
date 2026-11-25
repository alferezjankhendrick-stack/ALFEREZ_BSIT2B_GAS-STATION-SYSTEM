package com.scc.model;

public record LoginResult(Outcome outcome, User user) {
    public enum Outcome {SUCCESS, INVALID_CREDENTIALS, NOT_APPROVED}
}
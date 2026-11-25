package com.scc.util;

import java.util.List;

public final class Roles {
    public static final String ADMIN = "ADMIN";
    public static final String STAFF = "STAFF";
    public static final String CASHIER = "CASHIER";
    public static final String INVENTORY_MANAGER = "INVENTORY_MANAGER";
    public static final String CUSTOMER = "CUSTOMER";
    public static final List<String> ALL = List.of(ADMIN, STAFF, CASHIER, INVENTORY_MANAGER, CUSTOMER);
    public static final List<String> SELF_REGISTRATION = List.of(STAFF, CASHIER, INVENTORY_MANAGER, CUSTOMER);

    private Roles() {
    }

    public static String normalize(String role) {
        if (role == null) {
            return "";
        }
        String r = role.trim().toUpperCase().replace(' ', '_');
        return r.equals("USER") ? CUSTOMER : r;
    }
}
package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.LoginResult;
import com.scc.model.User;
import com.scc.util.InputUtil;
import com.scc.util.Roles;

import java.util.List;

public class Authentication {
    private final InputUtil in;
    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final FuelActions fuelActions;

    public Authentication(InputUtil in, UserDao userDao, ActivityLogDao activityLogDao, FuelActions fuelActions) {
        this.in = in;
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
        this.fuelActions = fuelActions;
    }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("          GAS STATION SYSTEM            ");
            System.out.println("========================================");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");

            switch (in.readInt("Choose an option: ")) {
                case 1 -> handleLogin();
                case 2 -> handleRegister();
                case 3 -> {
                    System.out.println("Exiting the system. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void handleRegister() {
        System.out.println("\n--- REGISTER ---");
        String username = in.readUsername("Enter a new username (blank to cancel): ");
        if (username == null) {
            System.out.println("Registration cancelled.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("Username '" + username + "' is already taken.");
            return;
        }
        String password = in.readPassword("Enter a new password (blank to cancel): ");
        if (password == null) {
            System.out.println("Registration cancelled.");
            return;
        }

        List<String> roles = Roles.SELF_REGISTRATION;
        System.out.println("Select role:");
        for (int i = 0; i < roles.size(); i++) {
            System.out.println((i + 1) + ". " + roles.get(i));
        }
        System.out.println("0. Cancel");
        int choice = in.readIntInRange("Choose role: ", 0, roles.size());
        if (choice == 0) {
            System.out.println("Registration cancelled.");
            return;
        }

        if (userDao.registerUser(username, password, roles.get(choice - 1)) > 0) {
            System.out.println("Registration submitted successfully! Waiting for admin approval.");
        } else {
            System.out.println("Registration failed. Please try again.");
        }
    }

    private void handleLogin() {
        System.out.println("\n--- LOGIN ---");
        String username = in.readLine("Enter username: ");
        String password = in.readLine("Enter password: ");

        LoginResult result = userDao.authenticate(username, password);
        switch (result.outcome()) {
            case INVALID_CREDENTIALS ->
                    System.out.println("Login failed. Invalid username or password.");
            case NOT_APPROVED -> System.out.println("Access denied. Your account is "
                    + result.user().status() + " - please wait for admin approval.");
            case SUCCESS -> openMenuFor(result.user());
        }
    }

    private void openMenuFor(User user) {
        System.out.println("Login successful! Welcome, " + user.username() + " | ROLE: " + user.roles());

        switch (Roles.normalize(user.roles())) {
            case Roles.ADMIN -> new AdminMenu(userDao, activityLogDao, in, fuelActions).display(user);
            case Roles.STAFF -> new StaffMenu(in, fuelActions).display(user);
            case Roles.CASHIER -> new CashierMenu(in, fuelActions).display(user);
            case Roles.INVENTORY_MANAGER -> new InventoryMenu(in, fuelActions).display(user);
            case Roles.CUSTOMER -> new UserMenu(userDao, in, fuelActions).display(user);
            default -> {
                System.out.println("Unknown role. Access denied.");
                return;
            }
        }

        activityLogDao.logAction(user.id(), "LOGOUT",
                "User " + user.username() + " (ID: " + user.id() + ") logged out.");
    }
}
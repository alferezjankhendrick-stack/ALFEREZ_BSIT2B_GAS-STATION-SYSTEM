package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.util.InputUtil;

/** CUSTOMER role: read-only access to prices and the customer's own history, plus password change. */
public class UserMenu {
    private final UserDao userDao;
    private final InputUtil in;
    private final FuelActions actions;

    public UserMenu(UserDao userDao, InputUtil in, FuelActions actions) {
        this.userDao = userDao;
        this.in = in;
        this.actions = actions;
    }

    public void display(User user) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n========================================");
            System.out.println("            CUSTOMER / USER MENU        ");
            System.out.println("========================================");
            System.out.println("1. View Current Fuel Prices");
            System.out.println("2. View Loyalty Points / Rewards");
            System.out.println("3. View Purchase History");
            System.out.println("4. Update Account Details");
            System.out.println("0. Logout");
            System.out.println("----------------------------------------");

            switch (in.readInt("Choose an option (0-4): ")) {
                case 1 -> actions.viewFuelPrices();
                case 2 -> actions.viewLoyaltyPoints(user);
                case 3 -> actions.viewPurchaseHistory(user);
                case 4 -> updateAccount(user);
                case 0 -> {
                    System.out.println("\nYou have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("\nInvalid choice. Please select an option between 0 and 4.");
            }
        }
    }

    private void updateAccount(User user) {
        System.out.println("\n--- ACCOUNT DETAILS ---");
        System.out.println("Username: " + user.username());
        System.out.println("Role:     " + user.roles());
        System.out.println("Status:   " + user.status());
        System.out.println("\n1. Change Password");
        System.out.println("0. Back");
        if (in.readIntInRange("Choose an option (0-1): ", 0, 1) == 0) {
            return;
        }

        String current = in.readLine("Current password: ");
        User fresh = userDao.findById(user.id());
        if (fresh == null || !fresh.password().equals(current)) {
            System.out.println("Current password is incorrect. Password not changed.");
            return;
        }
        String newPassword = in.readPassword("New password (blank to cancel): ");
        if (newPassword == null) {
            System.out.println("Password not changed.");
            return;
        }
        if (userDao.changePassword(user.id(), newPassword)) {
            System.out.println("Password updated successfully.");
        } else {
            System.out.println("Failed to update password.");
        }
    }
}
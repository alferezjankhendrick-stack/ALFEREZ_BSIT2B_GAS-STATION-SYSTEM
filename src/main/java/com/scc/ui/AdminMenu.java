package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class AdminMenu {
    private final UserDao userDao;
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void display(User admin) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n=====================================");
            System.out.println("           ADMIN DASHBOARD           "); 
            System.out.println("=====================================");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role / Status");
            System.out.println("6. Delete User Account");
            System.out.println("7. Logout");
            System.out.print("Choose an option (1-7): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.println("\nView All Users");
                    viewAllUsers();
                    break;
                case "2":
                    System.out.println("\nView Pending User Approvals");
                    viewPendingUsers();
                    break;
                case "3":
                    System.out.println("\nApprove User Registration");
                    approveUserAccount();
                    break;
                case "4":
                    System.out.println("\nAdd New User");
                    addNewUser();
                    break;
                case "5":
                    System.out.println("\nUpdate User Role / Status");
                    updateUserRoleOrStatus(admin);
                    break;
                case "6":
                    System.out.println("\nDelete User Account");
                    deleteUserAccount(admin);
                    break;
                case "7":
                    System.out.println("\nYou have been logged out.");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("\nInvalid choice. Please choose between 1 and 7.");
            }
        }
    }


    private void viewAllUsers() {
        List<User> users = userDao.getAllUsers();
        printUserTable(users);
    }


    private void viewPendingUsers() {
        List<User> pending = userDao.getPendingUsers();
        if (pending == null || pending.isEmpty()) {
            System.out.println("No pending registrations.");
            return;
        }
        printUserTable(pending);
    }


    private void approveUserAccount() {
        List<User> pending = userDao.getPendingUsers();
        if (pending == null || pending.isEmpty()) {
            System.out.println("No pending registrations to approve.");
            return;
        }

        printUserTable(pending);
        System.out.print("Enter Target User ID to approve (0 to cancel): ");
        String input = scanner.nextLine().trim();

        int userId;
        try {
            userId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a numeric User ID.");
            return;
        }

        if (userId == 0) {
            System.out.println("Approval cancelled.");
            return;
        }

        boolean inPendingList = false;
        for (User u : pending) {
            if (u.id() == userId) {
                inPendingList = true;
                break;
            }
        }

        if (!inPendingList) {
            System.out.println("User ID " + userId + " is not in the pending list.");
            return;
        }

        if (userDao.approveUser(userId)) {
            System.out.println("User ID " + userId + " has been approved.");
        } else {
            System.out.println("Failed to approve user ID " + userId + ".");
        }
    }


    private void addNewUser() {
        System.out.print("Enter username (leave blank to cancel): ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            System.out.println("Add user cancelled.");
            return;
        }

        if (userDao.usernameExists(username)) {
            System.out.println("Username '" + username + "' already exists.");
            return;
        }

        System.out.print("Enter password (leave blank to cancel): ");
        String password = scanner.nextLine().trim();
        if (password.isEmpty()) {
            System.out.println("Add user cancelled.");
            return;
        }

        String role = chooseRole();
        if (role == null) {
            System.out.println("User not created.");
            return;
        }

        if (userDao.addUser(username, password, role, "APPROVED")) {
            System.out.println("User '" + username + "' added successfully as " + role + ".");
        } else {
            System.out.println("Failed to add user.");
        }
    }


    private void updateUserRoleOrStatus(User admin) {
        List<User> users = userDao.getAllUsers();
        if (users == null || users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        printUserTable(users);
        Integer userId = readUserId("Enter Target User ID to update (0 to cancel): ");
        if (userId == null) {
            return;
        }

        User target = userDao.findById(userId);
        if (target == null) {
            System.out.println("No user found with ID " + userId + ".");
            return;
        }

        if (Objects.equals(target.id(), admin.id())) {
            System.out.println("You cannot change your own role or status.");
            return;
        }

        System.out.println("\nUser: " + target.username());
        System.out.println("Current Role:   " + target.roles());
        System.out.println("Current Status: " + target.status());
        System.out.println("\n1. Change Role");
        System.out.println("2. Change Status");
        System.out.println("0. Cancel");
        System.out.print("Choose an option: ");

        String option = scanner.nextLine().trim();

        switch (option) {
            case "1":
                String newRole = chooseRole();
                if (newRole == null) {
                    System.out.println("No changes made.");
                    return;
                }
                if (userDao.updateUserRole(userId, newRole)) {
                    System.out.println("Role updated to " + newRole + ".");
                } else {
                    System.out.println("Failed to update role.");
                }
                break;

            case "2":
                System.out.println("Statuses: 1. APPROVED  2. PENDING  0. Cancel");
                System.out.print("Choose new status: ");
                String statusChoice = scanner.nextLine().trim();
                String newStatus;
                switch (statusChoice) {
                    case "1": newStatus = "APPROVED"; break;
                    case "2": newStatus = "PENDING";  break;
                    case "0":
                        System.out.println("Update cancelled.");
                        return;
                    default:
                        System.out.println("Invalid status choice. No changes made.");
                        return;
                }
                if (userDao.updateUserStatus(userId, newStatus)) {
                    System.out.println("Status updated to " + newStatus + ".");
                } else {
                    System.out.println("Failed to update status.");
                }
                break;

            case "0":
                System.out.println("Update cancelled.");
                break;

            default:
                System.out.println("Invalid option. No changes made.");
        }
    }


    private void deleteUserAccount(User admin) {
        List<User> users = userDao.getAllUsers();
        if (users == null || users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        printUserTable(users);
        Integer userId = readUserId("Enter Target User ID to delete (0 to cancel): ");
        if (userId == null) {
            return;
        }

        User target = userDao.findById(userId);
        if (target == null) {
            System.out.println("No user found with ID " + userId + ".");
            return;
        }

        if (Objects.equals(target.id(), admin.id())) {
            System.out.println("You cannot delete your own account.");
            return;
        }

        System.out.print("Delete user '" + target.username() + "'? This cannot be undone (y/n): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("y")) {
            System.out.println("Deletion cancelled.");
            return;
        }

        if (userDao.deleteUser(userId)) {
            System.out.println("User '" + target.username() + "' has been deleted.");
        } else {
            System.out.println("Failed to delete user.");
        }
    }


    private Integer readUserId(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();

        int userId;
        try {
            userId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a numeric User ID.");
            return null;
        }

        if (userId == 0) {
            System.out.println("Action cancelled.");
            return null;
        }
        return userId;
    }

    // Returns the chosen role, or null if cancelled or invalid.
    private String chooseRole() {
        System.out.println("Roles:");
        System.out.println("1. ADMIN");
        System.out.println("2. STAFF");
        System.out.println("3. CASHIER");
        System.out.println("4. INVENTORY_MANAGER");
        System.out.println("0. Cancel");
        System.out.print("Choose role: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1": return "ADMIN";
            case "2": return "STAFF";
            case "3": return "CASHIER";
            case "4": return "INVENTORY_MANAGER";
            case "0":
                System.out.println("Cancelled.");
                return null;
            default:
                System.out.println("Invalid role choice.");
                return null;
        }
    }


    private void printUserTable(List<User> users) {
        if (users == null || users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        String format = "| %-5s | %-15s | %-12s | %-10s |%n";
        String border = "+-------+-----------------+--------------+------------+";

        System.out.println(border);
        System.out.printf(format, "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(border);
        for (User u : users) {
            System.out.printf(format,
                    fit(String.valueOf(u.id()), 5),
                    fit(u.username(), 15),
                    fit(u.roles(), 12),
                    fit(u.status(), 10));
        }
        System.out.println(border);
        System.out.println("Total: " + users.size() + " user(s)");
    }


    private String fit(String value, int width) {
        if (value == null) {
            return "";
        }
        return value.length() <= width ? value : value.substring(0, width - 1) + "~";
    }
}

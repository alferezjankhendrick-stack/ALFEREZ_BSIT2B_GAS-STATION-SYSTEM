package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;
import com.scc.util.InputUtil;
import com.scc.util.Roles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AdminMenu {
    private static final int DESC_WIDTH = 35;

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final InputUtil in;
    private final FuelActions fuelActions;

    public AdminMenu(UserDao userDao, ActivityLogDao activityLogDao, InputUtil in, FuelActions fuelActions) {
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
        this.in = in;
        this.fuelActions = fuelActions;
    }

    public void display(User admin) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n=====================================");
            System.out.println("           ADMIN DASHBOARD            ");
            System.out.println("=====================================");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role / Status");
            System.out.println("6. Delete User Account");
            System.out.println("7. View System Audit Logs");
            System.out.println("8. Fuel Inventory & Sales Reports");
            System.out.println("0. Logout");

            switch (in.readInt("Choose an option (0-8): ")) {
                case 1 -> {
                    System.out.println("\nView All Users");
                    viewAllUsers();
                }
                case 2 -> {
                    System.out.println("\nView Pending User Approvals");
                    viewPendingUsers();
                }
                case 3 -> {
                    System.out.println("\nApprove User Registration");
                    approveUserAccount(admin);
                }
                case 4 -> {
                    System.out.println("\nAdd New User");
                    addNewUser(admin);
                }
                case 5 -> {
                    System.out.println("\nUpdate User Role / Status");
                    updateUserRoleOrStatus(admin);
                }
                case 6 -> {
                    System.out.println("\nDelete User Account");
                    deleteUserAccount(admin);
                }
                case 7 -> handleAuditLogs();
                case 8 -> handleFuelReports();
                case 0 -> {
                    System.out.println("\nYou have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("\nInvalid choice. Please choose between 0 and 8.");
            }
        }
    }

    private void handleFuelReports() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- FUEL INVENTORY & SALES REPORTS ---");
            System.out.println("1. Fuel Tank Levels");
            System.out.println("2. All Fuel Sales");
            System.out.println("3. Delivery History");
            System.out.println("4. Fuel Prices");
            System.out.println("0. Back");
            switch (in.readInt("Choose an option (0-4): ")) {
                case 1 -> fuelActions.viewTankLevels();
                case 2 -> fuelActions.viewAllSales();
                case 3 -> fuelActions.viewDeliveries();
                case 4 -> fuelActions.viewFuelPrices();
                case 0 -> back = true;
                default -> System.out.println("Invalid choice. Please choose between 0 and 4.");
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


    private void approveUserAccount(User admin) {
        List<User> pending = userDao.getPendingUsers();
        if (pending == null || pending.isEmpty()) {
            System.out.println("No pending registrations to approve.");
            return;
        }

        printUserTable(pending);
        int userId = in.readInt("Enter Target User ID to approve (0 to cancel): ");

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

        if (userDao.approveUser(admin.id(), userId)) {
            System.out.println("User ID " + userId + " has been approved.");
        } else {
            System.out.println("Failed to approve user ID " + userId + ".");
        }
    }


    private void addNewUser(User admin) {
        String username = in.readUsername("Enter username (leave blank to cancel): ");
        if (username == null) {
            System.out.println("Add user cancelled.");
            return;
        }

        if (userDao.usernameExists(username)) {
            System.out.println("Username '" + username + "' already exists.");
            return;
        }

        String password = in.readPassword("Enter password (leave blank to cancel): ");
        if (password == null) {
            System.out.println("Add user cancelled.");
            return;
        }

        String role = chooseRole();
        if (role == null) {
            System.out.println("User not created.");
            return;
        }

        if (userDao.addUser(admin.id(), username, password, role, "APPROVED")) {
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

        switch (in.readIntInRange("Choose an option (0-2): ", 0, 2)) {
            case 1 -> {
                String newRole = chooseRole();
                if (newRole == null) {
                    System.out.println("No changes made.");
                    return;
                }
                if (userDao.updateUserRole(admin.id(), userId, newRole)) {
                    System.out.println("Role updated to " + newRole + ".");
                } else {
                    System.out.println("Failed to update role.");
                }
            }
            case 2 -> {
                System.out.println("Statuses: 1. APPROVED  2. PENDING  3. DISABLED  0. Cancel");
                String newStatus;
                switch (in.readIntInRange("Choose new status (0-3): ", 0, 3)) {
                    case 1 -> newStatus = "APPROVED";
                    case 2 -> newStatus = "PENDING";
                    case 3 -> newStatus = "DISABLED";
                    default -> {
                        System.out.println("Update cancelled.");
                        return;
                    }
                }
                if (userDao.updateUserStatus(admin.id(), userId, newStatus)) {
                    System.out.println("Status updated to " + newStatus + ".");
                } else {
                    System.out.println("Failed to update status.");
                }
            }
            default -> System.out.println("Update cancelled.");
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

        if (!in.confirm("Delete user '" + target.username() + "'? This cannot be undone (y/n): ")) {
            System.out.println("Deletion cancelled.");
            return;
        }

        if (userDao.deleteUser(admin.id(), userId, target.username())) {
            System.out.println("User '" + target.username() + "' has been deleted.");
        } else {
            System.out.println("Failed to delete user. They may still own sales or delivery records - "
                    + "set their status to DISABLED instead.");
        }
    }


    private Integer readUserId(String prompt) {
        int userId = in.readInt(prompt);
        if (userId == 0) {
            System.out.println("Action cancelled.");
            return null;
        }
        return userId;
    }


    private String chooseRole() {
        System.out.println("Roles:");
        for (int i = 0; i < Roles.ALL.size(); i++) {
            System.out.println((i + 1) + ". " + Roles.ALL.get(i));
        }
        System.out.println("0. Cancel");
        int choice = in.readIntInRange("Choose role: ", 0, Roles.ALL.size());
        if (choice == 0) {
            System.out.println("Cancelled.");
            return null;
        }
        return Roles.ALL.get(choice - 1);
    }


    private void printUserTable(List<User> users) {
        if (users == null || users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        String format = "| %-5s | %-15s | %-17s | %-10s |%n";
        String border = "+-------+-----------------+-------------------+------------+";

        System.out.println(border);
        System.out.printf(format, "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(border);
        for (User u : users) {
            System.out.printf(format,
                    fit(String.valueOf(u.id()), 5),
                    fit(u.username(), 15),
                    fit(u.roles(), 17),
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


    private void handleAuditLogs() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- SYSTEM AUDIT LOGS ---");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("0. Back");

            switch (in.readInt("Choose an option: ")) {
                case 1 -> printLogTable(activityLogDao.getAllLogs());
                case 2 -> handleFilterLogsByUser();
                case 0 -> back = true;
                default -> System.out.println("Invalid choice. Please choose 0, 1 or 2.");
            }
        }
    }

    private void handleFilterLogsByUser() {
        int userId = in.readInt("Enter User ID to filter (or 0 to cancel): ");
        if (userId == 0) {
            return;
        }
        printLogTable(activityLogDao.getLogsByUserId(userId));
    }

    private void printLogTable(List<ActivityLog> logs) {
        if (logs.isEmpty()) {
            System.out.println("No audit log entries found.");
            return;
        }

        String format = "%-5s | %-8s | %-20s | %-35s | %-20s%n";
        String border = "-".repeat(5 + 3 + 8 + 3 + 20 + 3 + DESC_WIDTH + 3 + 20);

        System.out.println(border);
        System.out.printf(format, "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println(border);

        for (ActivityLog log : logs) {
            List<String> lines = wrap(log.description(), DESC_WIDTH);
            System.out.printf(format, log.id(), log.userId(), log.actionType(), lines.get(0), log.timestamp());
            for (int i = 1; i < lines.size(); i++) {
                System.out.printf(format, "", "", "", lines.get(i), "");
            }
        }
        System.out.println(border);
        System.out.println(logs.size() + " log entr" + (logs.size() == 1 ? "y" : "ies") + " shown.");
    }

    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            while (word.length() > width) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= width) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0 || lines.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }
}
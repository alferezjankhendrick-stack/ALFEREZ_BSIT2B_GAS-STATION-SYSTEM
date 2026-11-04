package com.scc.ui;

import com.scc.dao.FuelSaleDao;
import com.scc.dao.UserDao;
import com.scc.model.User;
import java.util.Scanner;

public class StaffMenu {
    private final UserDao userDao;
    private final FuelSaleDao fuelSaleDao;
    private final Scanner scanner;

    public StaffMenu(UserDao userDao, FuelSaleDao fuelSaleDao, Scanner scanner) {
        this.userDao = userDao;
        this.fuelSaleDao = fuelSaleDao;
        this.scanner = scanner;
    }

    public void display(User staff) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n========================================");
            System.out.println("         GAS STATION STAFF MENU         ");
            System.out.println("========================================");
            System.out.println("1. Process Fuel Sale / Dispense");
            System.out.println("2. Check Fuel Tank Levels");
            System.out.println("3. Log Fuel Delivery / Restock");
            System.out.println("4. Record Shift Sales & Cash Collection");
            System.out.println("5. View Daily Transaction History");
            System.out.println("6. Update Fuel Sale Status (Void / Complete)");
            System.out.println("0. Logout");
            System.out.println("----------------------------------------");
            System.out.print("Choose an option (0-6): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleProcessFuelSale(staff);
                    break;
                case "2":
                    System.out.println("\nCheck Fuel Tank Levels");
                    break;
                case "3":
                    System.out.println("\nLog Fuel Delivery / Restock");
                    break;
                case "4":
                    System.out.println("\nRecord Shift Sales & Cash Collection");
                    break;
                case "5":
                    System.out.println("\nView Daily Transaction History");
                    break;
                case "6":
                    handleUpdateSaleStatus(staff);
                    break;
                case "0":
                    System.out.println("\nYou have been logged out.");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("\nInvalid choice. Please select an option between 0 and 6.");
            }
        }
    }

    private void handleProcessFuelSale(User staff) {
        System.out.println("\n--- PROCESS FUEL SALE ---");
        System.out.printf("Fuel type: [1] DIESEL (P%.2f/L)  [2] UNLEADED (P%.2f/L)  [3] PREMIUM (P%.2f/L): ",
                FuelSaleDao.priceFor("DIESEL"), FuelSaleDao.priceFor("UNLEADED"), FuelSaleDao.priceFor("PREMIUM"));
        String fuelType = switch (scanner.nextLine().trim()) {
            case "1" -> "DIESEL";
            case "2" -> "UNLEADED";
            case "3" -> "PREMIUM";
            default -> null;
        };
        if (fuelType == null) {
            System.out.println("Invalid fuel type selection.");
            return;
        }

        System.out.print("Liters dispensed: ");
        double liters;
        try {
            liters = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number of liters.");
            return;
        }
        if (liters <= 0) {
            System.out.println("Liters must be greater than 0.");
            return;
        }

        int saleId = fuelSaleDao.processSale(staff.id(), fuelType, liters);
        if (saleId > 0) {
            System.out.printf("Sale #%d recorded. Total: P%.2f%n",
                    saleId, FuelSaleDao.priceFor(fuelType) * liters);
        } else {
            System.out.println("Failed to process fuel sale.");
        }
    }

    private void handleUpdateSaleStatus(User staff) {
        System.out.println("\n--- UPDATE FUEL SALE STATUS ---");
        System.out.print("Sale ID: ");
        int saleId;
        try {
            saleId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a numeric Sale ID.");
            return;
        }

        System.out.print("New status [1] COMPLETED [2] VOIDED: ");
        String status = switch (scanner.nextLine().trim()) {
            case "1" -> "COMPLETED";
            case "2" -> "VOIDED";
            default -> null;
        };
        if (status == null) {
            System.out.println("Invalid status selection.");
            return;
        }

        if (fuelSaleDao.updateSaleStatus(staff.id(), saleId, status)) {
            System.out.println("Sale #" + saleId + " status set to " + status + ".");
        } else {
            System.out.println("Sale not found or update failed.");
        }
    }
}
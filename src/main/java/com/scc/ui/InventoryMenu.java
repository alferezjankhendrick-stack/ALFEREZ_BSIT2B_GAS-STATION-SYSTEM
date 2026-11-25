package com.scc.ui;

import com.scc.model.User;
import com.scc.util.InputUtil;


public class InventoryMenu {
    private final InputUtil in;
    private final FuelActions actions;

    public InventoryMenu(InputUtil in, FuelActions actions) {
        this.in = in;
        this.actions = actions;
    }

    public void display(User manager) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n========================================");
            System.out.println("         INVENTORY MANAGER MENU         ");
            System.out.println("========================================");
            System.out.println("1. Check Fuel Tank Levels");
            System.out.println("2. Log Fuel Delivery / Restock");
            System.out.println("3. View Delivery History");
            System.out.println("4. Update Fuel Price");
            System.out.println("0. Logout");
            System.out.println("----------------------------------------");

            switch (in.readInt("Choose an option (0-4): ")) {
                case 1 -> actions.viewTankLevels();
                case 2 -> actions.logDelivery(manager);
                case 3 -> actions.viewDeliveries();
                case 4 -> actions.updateFuelPrice(manager);
                case 0 -> {
                    System.out.println("\nYou have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("\nInvalid choice. Please select an option between 0 and 4.");
            }
        }
    }
}
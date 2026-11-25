package com.scc.ui;

import com.scc.model.User;
import com.scc.util.InputUtil;

public class StaffMenu {
    private final InputUtil in;
    private final FuelActions actions;

    public StaffMenu(InputUtil in, FuelActions actions) {
        this.in = in;
        this.actions = actions;
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

            switch (in.readInt("Choose an option (0-6): ")) {
                case 1 -> actions.processFuelSale(staff);
                case 2 -> actions.viewTankLevels();
                case 3 -> actions.logDelivery(staff);
                case 4 -> actions.recordShift(staff);
                case 5 -> actions.viewDailyHistory();
                case 6 -> actions.updateSaleStatus(staff);
                case 0 -> {
                    System.out.println("\nYou have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("\nInvalid choice. Please select an option between 0 and 6.");
            }
        }
    }
}
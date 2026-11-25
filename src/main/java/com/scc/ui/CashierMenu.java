package com.scc.ui;

import com.scc.model.User;
import com.scc.util.InputUtil;

public class CashierMenu {
    private final InputUtil in;
    private final FuelActions actions;

    public CashierMenu(InputUtil in, FuelActions actions) {
        this.in = in;
        this.actions = actions;
    }

    public void display(User cashier) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n========================================");
            System.out.println("            CASHIER MENU                ");
            System.out.println("========================================");
            System.out.println("1. Process Fuel Sale");
            System.out.println("2. View Current Fuel Prices");
            System.out.println("3. View Daily Transaction History");
            System.out.println("4. Record Shift Sales & Cash Collection");
            System.out.println("5. Update Fuel Sale Status (Void / Complete)");
            System.out.println("0. Logout");
            System.out.println("----------------------------------------");

            switch (in.readInt("Choose an option (0-5): ")) {
                case 1 -> actions.processFuelSale(cashier);
                case 2 -> actions.viewFuelPrices();
                case 3 -> actions.viewDailyHistory();
                case 4 -> actions.recordShift(cashier);
                case 5 -> actions.updateSaleStatus(cashier);
                case 0 -> {
                    System.out.println("\nYou have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("\nInvalid choice. Please select an option between 0 and 5.");
            }
        }
    }
}
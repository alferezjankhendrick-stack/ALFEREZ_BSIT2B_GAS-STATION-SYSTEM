package com.scc.ui;

import com.scc.dao.FuelDeliveryDao;
import com.scc.dao.FuelSaleDao;
import com.scc.dao.FuelTypeDao;
import com.scc.dao.ShiftReportDao;
import com.scc.dao.UserDao;
import com.scc.model.FuelDelivery;
import com.scc.model.FuelSale;
import com.scc.model.FuelType;
import com.scc.model.OperationResult;
import com.scc.model.ShiftReport;
import com.scc.model.User;
import com.scc.util.ConsoleTable;
import com.scc.util.InputUtil;
import com.scc.util.MoneyUtil;
import com.scc.util.Roles;

import java.util.ArrayList;
import java.util.List;

public class FuelActions {
    private final InputUtil in;
    private final UserDao userDao;
    private final FuelTypeDao fuelTypeDao;
    private final FuelSaleDao fuelSaleDao;
    private final FuelDeliveryDao deliveryDao;
    private final ShiftReportDao shiftReportDao;

    public FuelActions(InputUtil in, UserDao userDao, FuelTypeDao fuelTypeDao, FuelSaleDao fuelSaleDao,
                       FuelDeliveryDao deliveryDao, ShiftReportDao shiftReportDao) {
        this.in = in;
        this.userDao = userDao;
        this.fuelTypeDao = fuelTypeDao;
        this.fuelSaleDao = fuelSaleDao;
        this.deliveryDao = deliveryDao;
        this.shiftReportDao = shiftReportDao;
    }


    public void viewFuelPrices() {
        System.out.println("\n--- CURRENT FUEL PRICES ---");
        List<String[]> rows = new ArrayList<>();
        for (FuelType f : fuelTypeDao.getAll()) {
            rows.add(new String[]{String.valueOf(f.id()), f.name(), String.format("P%.2f", f.pricePerLiter())});
        }
        ConsoleTable.print(new String[]{"ID", "FUEL", "PRICE / LITER"}, rows);
    }

    public void viewTankLevels() {
        System.out.println("\n--- FUEL TANK LEVELS ---");
        List<String[]> rows = new ArrayList<>();
        for (FuelType f : fuelTypeDao.getAll()) {
            rows.add(new String[]{String.valueOf(f.id()), f.name(),
                    String.format("%.2f", f.stockLiters()), String.format("%.2f", f.capacityLiters()),
                    String.format("%.1f%%", f.stockLiters() / f.capacityLiters() * 100),
                    f.isLow() ? "LOW - REORDER" : "OK"});
        }
        ConsoleTable.print(new String[]{"ID", "FUEL", "STOCK (L)", "CAPACITY (L)", "FILL", "STATUS"}, rows);
    }

    public void updateFuelPrice(User user) {
        System.out.println("\n--- UPDATE FUEL PRICE ---");
        FuelType fuel = chooseFuelType(true);
        if (fuel == null) {
            return;
        }
        double price = in.readPositiveDouble("New price per liter for " + fuel.name() + ": P");
        System.out.println(fuelTypeDao.updatePrice(user.id(), fuel.id(), price).message());
    }

    public void logDelivery(User user) {
        System.out.println("\n--- LOG FUEL DELIVERY / RESTOCK ---");
        FuelType fuel = chooseFuelType(true);
        if (fuel == null) {
            return;
        }
        System.out.printf("%s tank: %.2f L / %.2f L (free space %.2f L)%n",
                fuel.name(), fuel.stockLiters(), fuel.capacityLiters(), fuel.freeSpace());
        double liters = in.readPositiveDouble("Liters delivered: ");
        String supplier = in.readRequired("Supplier name: ");
        System.out.println(deliveryDao.logDelivery(user.id(), fuel.id(), liters, supplier).message());
    }

    public void viewDeliveries() {
        System.out.println("\n--- RECENT FUEL DELIVERIES ---");
        List<FuelDelivery> deliveries = deliveryDao.getRecentDeliveries(20);
        if (deliveries.isEmpty()) {
            System.out.println("No deliveries recorded yet.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (FuelDelivery d : deliveries) {
            rows.add(new String[]{String.valueOf(d.id()), d.fuelType(), String.format("%.2f", d.liters()),
                    d.supplier(), d.receivedBy(), d.createdAt()});
        }
        ConsoleTable.print(new String[]{"ID", "FUEL", "LITERS", "SUPPLIER", "RECEIVED BY", "DATE"}, rows);
    }


    public void processFuelSale(User staff) {
        System.out.println("\n--- PROCESS FUEL SALE ---");
        FuelType fuel = chooseFuelType(true);
        if (fuel == null) {
            return;
        }
        double liters = in.readPositiveDouble("Liters dispensed: ");
        Integer customerId = chooseCustomer();
        if (customerId != null && customerId == -1) {
            return; // cancelled
        }

        double estimate = MoneyUtil.round2(fuel.pricePerLiter() * MoneyUtil.round2(liters));
        System.out.printf("Total due: %.2f L x P%.2f = P%.2f%n", liters, fuel.pricePerLiter(), estimate);
        if (!in.confirm("Confirm sale? (y/n): ")) {
            System.out.println("Sale cancelled.");
            return;
        }
        OperationResult result = fuelSaleDao.processSale(staff.id(), customerId, fuel.id(), liters);
        System.out.println(result.message());
    }

    private Integer chooseCustomer() {
        while (true) {
            String name = in.readLine("Customer username for loyalty points (blank = walk-in, 0 = cancel sale): ");
            if (name.isEmpty()) {
                return null;
            }
            if (name.equals("0")) {
                System.out.println("Sale cancelled.");
                return -1;
            }
            User customer = userDao.findByUsername(name);
            if (customer == null || !Roles.CUSTOMER.equals(Roles.normalize(customer.roles()))
                    || !"APPROVED".equalsIgnoreCase(customer.status())) {
                System.out.println("No approved customer account named '" + name + "'. Try again.");
                continue;
            }
            return customer.id();
        }
    }

    public void updateSaleStatus(User staff) {
        System.out.println("\n--- UPDATE FUEL SALE STATUS ---");
        int saleId = in.readInt("Sale ID (0 to cancel): ");
        if (saleId == 0) {
            System.out.println("Action cancelled.");
            return;
        }
        System.out.println("New status:  1. COMPLETED   2. VOIDED   0. Cancel");
        int choice = in.readIntInRange("Choose (0-2): ", 0, 2);
        if (choice == 0) {
            System.out.println("Action cancelled.");
            return;
        }
        String status = choice == 1 ? "COMPLETED" : "VOIDED";
        System.out.println(fuelSaleDao.updateSaleStatus(staff.id(), saleId, status).message());
    }

    public void viewDailyHistory() {
        System.out.println("\n--- TODAY'S TRANSACTIONS ---");
        printSales(fuelSaleDao.getTodaySales(), true);
    }

    public void viewAllSales() {
        System.out.println("\n--- ALL FUEL SALES ---");
        printSales(fuelSaleDao.getAllSales(), true);
    }

    public void viewPurchaseHistory(User customer) {
        System.out.println("\n--- MY PURCHASE HISTORY ---");
        printSales(fuelSaleDao.getSalesByCustomer(customer.id()), false);
    }

    public void viewLoyaltyPoints(User customer) {
        int points = fuelSaleDao.getLoyaltyPoints(customer.id());
        System.out.println("\n--- LOYALTY POINTS ---");
        System.out.printf("Customer: %s%n", customer.username());
        System.out.printf("Points earned: %d  (1 point for every P%.0f spent on completed sales)%n",
                points, FuelSaleDao.PESOS_PER_POINT);
    }

    private void printSales(List<FuelSale> sales, boolean showPeople) {
        if (sales.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        double liters = 0;
        for (FuelSale s : sales) {
            if (s.status().equals("COMPLETED")) {
                total += s.totalAmount();
                liters += s.liters();
            }
            if (showPeople) {
                rows.add(new String[]{String.valueOf(s.id()), s.fuelType(), String.format("%.2f", s.liters()),
                        String.format("%.2f", s.pricePerLiter()), String.format("%.2f", s.totalAmount()), s.status(),
                        s.staffName(), s.customerName() == null ? "WALK-IN" : s.customerName(), s.createdAt()});
            } else {
                rows.add(new String[]{String.valueOf(s.id()), s.fuelType(), String.format("%.2f", s.liters()),
                        String.format("%.2f", s.pricePerLiter()), String.format("%.2f", s.totalAmount()),
                        s.status(), s.createdAt()});
            }
        }
        if (showPeople) {
            ConsoleTable.print(new String[]{"SALE", "FUEL", "LITERS", "PRICE/L", "TOTAL", "STATUS", "STAFF",
                    "CUSTOMER", "DATE"}, rows);
        } else {
            ConsoleTable.print(new String[]{"SALE", "FUEL", "LITERS", "PRICE/L", "TOTAL", "STATUS", "DATE"}, rows);
        }
        System.out.printf("%d transaction(s). Completed: %.2f L, %s%n", sales.size(), liters, MoneyUtil.peso(total));
    }


    public void recordShift(User staff) {
        System.out.println("\n--- RECORD SHIFT SALES & CASH COLLECTION ---");
        ShiftReport today = shiftReportDao.getTodaySummary(staff.id());
        if (today == null) {
            System.out.println("Could not load today's sales.");
            return;
        }
        System.out.printf("Today's completed sales for %s: %d sale(s), %.2f L, expected cash %s%n",
                staff.username(), today.salesCount(), today.totalLiters(), MoneyUtil.peso(today.expectedAmount()));
        double cash = in.readNonNegativeDouble("Cash collected: P");
        System.out.println(shiftReportDao.recordShift(staff.id(), cash).message());
    }

    private FuelType chooseFuelType(boolean showStock) {
        List<FuelType> types = fuelTypeDao.getAll();
        if (types.isEmpty()) {
            System.out.println("No fuel types are configured.");
            return null;
        }
        List<String[]> rows = new ArrayList<>();
        for (FuelType f : types) {
            rows.add(new String[]{String.valueOf(f.id()), f.name(), String.format("P%.2f", f.pricePerLiter()),
                    showStock ? String.format("%.2f", f.stockLiters()) : ""});
        }
        ConsoleTable.print(new String[]{"ID", "FUEL", "PRICE / LITER", "STOCK (L)"}, rows);
        while (true) {
            int id = in.readInt("Enter fuel ID (0 to cancel): ");
            if (id == 0) {
                System.out.println("Action cancelled.");
                return null;
            }
            for (FuelType f : types) {
                if (f.id() == id) {
                    return f;
                }
            }
            System.out.println("No fuel type with ID " + id + ".");
        }
    }
}
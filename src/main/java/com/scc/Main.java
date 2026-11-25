package com.scc;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DatabaseInitializer;
import com.scc.dao.FuelDeliveryDao;
import com.scc.dao.FuelSaleDao;
import com.scc.dao.FuelTypeDao;
import com.scc.dao.ShiftReportDao;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;
import com.scc.ui.FuelActions;
import com.scc.util.InputUtil;

import java.util.NoSuchElementException;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initializeDatabase();

        InputUtil in = new InputUtil(new Scanner(System.in));
        UserDao userDao = new UserDao();
        ActivityLogDao activityLogDao = new ActivityLogDao();
        FuelActions fuelActions = new FuelActions(in, userDao, new FuelTypeDao(), new FuelSaleDao(),
                new FuelDeliveryDao(), new ShiftReportDao());

        try {
            new Authentication(in, userDao, activityLogDao, fuelActions).run();
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Exiting the system.");
        }
    }
}
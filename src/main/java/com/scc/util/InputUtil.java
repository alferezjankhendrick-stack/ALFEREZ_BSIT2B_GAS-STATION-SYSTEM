package com.scc.util;

import java.util.Scanner;
import java.util.regex.Pattern;

public class InputUtil {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,20}");
    private static final int MIN_PASSWORD = 4;

    private final Scanner scanner;

    public InputUtil(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Input cannot be empty. Please try again.");
        }
    }

    public int readInt(String prompt) {
        while (true) {
            String raw = readLine(prompt);
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.printf("Invalid choice. Please enter a number between %d and %d.%n", min, max);
        }
    }

    public double readPositiveDouble(String prompt) {
        while (true) {
            double value = readDouble(prompt);
            if (value > 0) {
                return value;
            }
            System.out.println("Value must be greater than 0.");
        }
    }

    public double readNonNegativeDouble(String prompt) {
        while (true) {
            double value = readDouble(prompt);
            if (value >= 0) {
                return value;
            }
            System.out.println("Value cannot be negative.");
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            String raw = readLine(prompt);
            try {
                double value = Double.parseDouble(raw);
                if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                    return value;
                }
            } catch (NumberFormatException e) {

            }
            System.out.println("Invalid input. Please enter a number (example: 25.5).");
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String answer = readLine(prompt).toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            System.out.println("Please answer y or n.");
        }
    }

    public String readUsername(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value.isEmpty()) {
                return null;
            }
            if (USERNAME.matcher(value).matches()) {
                return value;
            }
            System.out.println("Username must be 3-20 characters: letters, numbers or underscore.");
        }
    }

    public String readPassword(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value.isEmpty()) {
                return null;
            }
            if (value.length() >= MIN_PASSWORD) {
                return value;
            }
            System.out.println("Password must be at least " + MIN_PASSWORD + " characters.");
        }
    }
}
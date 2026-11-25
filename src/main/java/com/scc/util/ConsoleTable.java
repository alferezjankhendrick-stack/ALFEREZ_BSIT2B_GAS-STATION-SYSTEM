package com.scc.util;

import java.util.List;

public final class ConsoleTable {
    private ConsoleTable() {
    }

    public static void print(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < headers.length; i++) {
                widths[i] = Math.max(widths[i], cell(row, i).length());
            }
        }

        StringBuilder border = new StringBuilder("+");
        StringBuilder format = new StringBuilder("|");
        for (int w : widths) {
            border.append("-".repeat(w + 2)).append("+");
            format.append(" %-").append(w).append("s |");
        }
        format.append("%n");

        System.out.println(border);
        System.out.printf(format.toString(), (Object[]) headers);
        System.out.println(border);
        for (String[] row : rows) {
            Object[] cells = new Object[headers.length];
            for (int i = 0; i < headers.length; i++) {
                cells[i] = cell(row, i);
            }
            System.out.printf(format.toString(), cells);
        }
        System.out.println(border);
    }

    private static String cell(String[] row, int i) {
        return i < row.length && row[i] != null ? row[i] : "";
    }
}
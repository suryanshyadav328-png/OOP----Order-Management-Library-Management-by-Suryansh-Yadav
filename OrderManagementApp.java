package com.college.ordermanagement;

import java.sql.*;
import java.util.Scanner;

public class OrderManagementApp {
    private static final Scanner SC = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection connection = DBConnection.getConnection()) {
            System.out.println("Connected to ecommerce_db successfully.");
            createTableIfNotExists(connection);
            insertThreeSampleOrders(connection);

            int choice;
            do {
                displayMenu();
                choice = readInt("Enter your choice: ");

                try {
                    switch (choice) {
                        case 1 -> insertOrder(connection);
                        case 2 -> searchOrdersByStatus(connection);
                        case 3 -> increaseOrderQuantity(connection);
                        case 4 -> updateOrderStatus(connection);
                        case 5 -> displayTotalOrderValue(connection);
                        case 6 -> deleteCancelledOrder(connection);
                        case 0 -> System.out.println("Exiting Order Management System.");
                        default -> System.out.println("Invalid choice. Please try again.");
                    }
                } catch (SQLException ex) {
                    System.out.println("Database operation failed: " + ex.getMessage());
                }
            } while (choice != 0);

        } catch (SQLException ex) {
            System.out.println("Unable to connect to MySQL: " + ex.getMessage());
            System.out.println("Check that MySQL is running and DBConnection.java has the correct password.");
        }
    }

    private static void displayMenu() {
        System.out.println("\n========== ORDER MANAGEMENT SYSTEM ==========");
        System.out.println("1. Insert a new order");
        System.out.println("2. Search orders by status (quantity > 2)");
        System.out.println("3. Increase quantity of an order");
        System.out.println("4. Update order status");
        System.out.println("5. Display total order value");
        System.out.println("6. Delete an order (only if status is Cancelled)");
        System.out.println("0. Exit");
    }

    private static void createTableIfNotExists(Connection con) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS Orders (
                    order_id INT NOT NULL PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    product_name VARCHAR(100) NOT NULL,
                    quantity INT NOT NULL,
                    unit_price DOUBLE NOT NULL,
                    status VARCHAR(30) NOT NULL
                )
                """;
        try (Statement statement = con.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    // Inserts at least three sample orders at startup.
    // Existing IDs are detected first and duplicate-key errors are also handled safely.
    private static void insertThreeSampleOrders(Connection con) throws SQLException {
        Object[][] sampleOrders = {
                {1001, "Aarav Sharma", "Wireless Mouse", 3, 599.00, "Pending"},
                {1002, "Diya Verma", "Keyboard", 2, 1299.00, "Shipped"},
                {1003, "Kabir Singh", "USB-C Hub", 4, 1899.00, "Cancelled"}
        };

        for (Object[] order : sampleOrders) {
            int orderId = (Integer) order[0];
            if (orderExists(con, orderId)) {
                System.out.println("Sample order ID " + orderId + " already exists; skipped.");
            } else {
                insertOrderRecord(con, order);
                System.out.println("Sample order " + orderId + " inserted.");
            }
        }
    }

    private static boolean orderExists(Connection con, int orderId) throws SQLException {
        String sql = "SELECT order_id FROM Orders WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void insertOrder(Connection con) throws SQLException {
        int orderId = readInt("Order ID: ");

        // Friendly duplicate check before attempting INSERT.
        if (orderExists(con, orderId)) {
            System.out.println("Duplicate order ID! Order ID " + orderId + " already exists. Record not inserted.");
            return;
        }

        System.out.print("Customer name: ");
        String customerName = SC.nextLine().trim();
        System.out.print("Product name: ");
        String productName = SC.nextLine().trim();
        int quantity = readInt("Quantity: ");
        double unitPrice = readDouble("Unit price: ");
        System.out.print("Status (Pending/Shipped/Delivered/Cancelled): ");
        String status = SC.nextLine().trim();

        String sql = """
                INSERT INTO Orders
                (order_id, customer_name, product_name, quantity, unit_price, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, customerName);
            ps.setString(3, productName);
            ps.setInt(4, quantity);
            ps.setDouble(5, unitPrice);
            ps.setString(6, status);
            ps.executeUpdate();
            System.out.println("Order inserted successfully.");
        } catch (SQLException ex) {
            // MySQL error code 1062 = duplicate entry for a PRIMARY KEY/UNIQUE key.
            if (ex.getErrorCode() == 1062) {
                System.out.println("Duplicate order ID! Record not inserted.");
            } else {
                throw ex;
            }
        }
    }

    private static void insertOrderRecord(Connection con, Object[] order) throws SQLException {
        String sql = "INSERT INTO Orders VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, (Integer) order[0]);
            ps.setString(2, (String) order[1]);
            ps.setString(3, (String) order[2]);
            ps.setInt(4, (Integer) order[3]);
            ps.setDouble(5, (Double) order[4]);
            ps.setString(6, (String) order[5]);
            ps.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getErrorCode() != 1062) {
                throw ex;
            }
            System.out.println("Duplicate sample order ID skipped.");
        }
    }

    private static void searchOrdersByStatus(Connection con) throws SQLException {
        System.out.print("Enter order status to search: ");
        String status = SC.nextLine().trim();

        String sql = """
                SELECT * FROM Orders
                WHERE status = ? AND quantity > 2
                ORDER BY unit_price DESC
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                printOrders(rs);
            }
        }
    }

    private static void increaseOrderQuantity(Connection con) throws SQLException {
        int orderId = readInt("Order ID: ");
        int additionalQuantity = readInt("Additional quantity: ");

        if (additionalQuantity <= 0) {
            System.out.println("Additional quantity must be greater than zero.");
            return;
        }

        String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, additionalQuantity);
            ps.setInt(2, orderId);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                System.out.println("Order does not exist.");
            } else {
                System.out.println("Quantity updated successfully.");
                displayOrderById(con, orderId);
            }
        }
    }

    private static void updateOrderStatus(Connection con) throws SQLException {
        int orderId = readInt("Order ID: ");
        System.out.print("New status (Pending/Shipped/Delivered): ");
        String newStatus = SC.nextLine().trim();

        if (!isValidStatus(newStatus)) {
            System.out.println("Invalid status. Allowed values: Pending, Shipped, Delivered.");
            return;
        }

        if (!orderExists(con, orderId)) {
            System.out.println("Order does not exist.");
            return;
        }

        String sql = "UPDATE Orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, normalizeStatus(newStatus));
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }

        System.out.println("Order status updated. Updated order details:");
        displayOrderById(con, orderId);
    }

    private static boolean isValidStatus(String status) {
        return status.equalsIgnoreCase("Pending")
                || status.equalsIgnoreCase("Shipped")
                || status.equalsIgnoreCase("Delivered");
    }

    private static String normalizeStatus(String status) {
        if (status.equalsIgnoreCase("Pending")) return "Pending";
        if (status.equalsIgnoreCase("Shipped")) return "Shipped";
        return "Delivered";
    }

    private static void displayTotalOrderValue(Connection con) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity * unit_price), 0) AS total_value FROM Orders";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                System.out.printf("Total order value: %.2f%n", rs.getDouble("total_value"));
            }
        }
    }

    private static void deleteCancelledOrder(Connection con) throws SQLException {
        int orderId = readInt("Order ID to delete: ");
        String sql = "DELETE FROM Orders WHERE order_id = ? AND status = 'Cancelled'";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            int rows = ps.executeUpdate();

            if (rows == 1) {
                System.out.println("Cancelled order deleted successfully.");
            } else {
                System.out.println("Order cannot be deleted: it does not exist or its status is not Cancelled.");
            }
        }
    }

    private static void displayOrderById(Connection con, int orderId) throws SQLException {
        String sql = "SELECT * FROM Orders WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                printOrders(rs);
            }
        }
    }

    private static void printOrders(ResultSet rs) throws SQLException {
        boolean found = false;
        System.out.printf("%-10s %-20s %-20s %-10s %-12s %-15s%n",
                "Order ID", "Customer", "Product", "Quantity", "Unit Price", "Status");
        System.out.println("--------------------------------------------------------------------------------------");

        while (rs.next()) {
            found = true;
            System.out.printf("%-10d %-20s %-20s %-10d %-12.2f %-15s%n",
                    rs.getInt("order_id"),
                    rs.getString("customer_name"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("unit_price"),
                    rs.getString("status"));
        }

        if (!found) {
            System.out.println("No matching orders found.");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(SC.nextLine().trim());
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = Double.parseDouble(SC.nextLine().trim());
                if (value < 0) throw new NumberFormatException();
                return value;
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid non-negative number.");
            }
        }
    }
}

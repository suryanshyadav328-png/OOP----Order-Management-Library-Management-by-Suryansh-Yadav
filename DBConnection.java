package com.college.ordermanagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL =
            "jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";

    // IMPORTANT: Replace this with your own MySQL password.
    // If your local root account has no password, use "".
    private static final String PASSWORD = "suru";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

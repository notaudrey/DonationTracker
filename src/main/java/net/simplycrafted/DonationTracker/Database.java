package net.simplycrafted.DonationTracker;

import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.UUID;

/**
 * Copyright © Brian Ronald
 * 28/06/14
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
public class Database {
    // MySQL database handler.
    // No other class should be talking to the database.
    // No other class should be constructing SQL statements.

    static private Connection db_conn;
    static private DonationTracker donationtracker;
    private String prefix = "";

    // We call this from the constructor, and whenever we find that the database has gone away
    public void connect() {
        donationtracker.getLogger().info("Opening MySQL database connection");
        String hostname = donationtracker.getConfig().getString("mysql.hostname");
        int port = donationtracker.getConfig().getInt("mysql.port");
        String database = donationtracker.getConfig().getString("mysql.database");
        String user = donationtracker.getConfig().getString("mysql.user");
        String password = donationtracker.getConfig().getString("mysql.password");
        try {
            db_conn = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s", hostname, String.valueOf(port), database, user, password));
        } catch (Exception e) {
            donationtracker.getLogger().info("DonationTracker requires a MySQL database. Couldn't get connected.");
            donationtracker.getLogger().info(e.toString());
        }
    }

    public boolean connectionIsDead() {
        try {
            db_conn.createStatement().executeQuery("SELECT 1").close();
        } catch (Exception e) {
            donationtracker.getLogger().info("Database connection went wrong");
            return true;
        }
        return false;
    }

    // Call this once, when the plugin is being disabled. Called statically from onDisable()
    public static void disconnect() {
        try {
            donationtracker.getLogger().info("Closing MySQL database connection");
            db_conn.close();
        } catch (Exception e) {
            donationtracker.getLogger().info("Tried to close the connection, but failed (possibly because it's already closed)");
        }
    }

    // Create any tables we need (if they don't exist)
    private void createTables() {
        Statement sql;
        ResultSet result;
        try {
            sql = db_conn.createStatement();
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS `"+prefix+"settings` (" +
                    "setting VARCHAR(10) PRIMARY KEY," +
                    "stringvalue VARCHAR(50)," +
                    "numericvalue DECIMAL(10,2)" +
                    ")");
            // Check if the database has been marked with a version by this plugin
            result = sql.executeQuery("SELECT numericvalue FROM `"+prefix+"settings` WHERE setting LIKE 'version'");
            if (!result.next()) {
                // The version setting doesn't exist; we probably just made the table, so mark it with our version.
                sql.executeUpdate("INSERT INTO `"+prefix+"settings` (setting,numericvalue) VALUES ('version','"+ donationtracker.getDescription().getVersion() +"')");
            }
            result.close();
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS `" + prefix + "donations` (" +
                    "donationtime TIMESTAMP," +
                    "uuid CHAR(36)," +
                    "amount DECIMAL(10,2)" +
                    ")");
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS `"+prefix+"goalsreached` (" +
                    "goal VARCHAR(50) PRIMARY KEY," +
                    "reached ENUM('Y','N') DEFAULT 'N'" +
                    ")");
            sql.close();
        } catch (SQLException e) {
            donationtracker.getLogger().info(e.toString());
        }
    }

    public void initialiseGoals() {
        ConfigurationSection config = donationtracker.getConfig();
        PreparedStatement sql;
        for (String goal : config.getConfigurationSection("goals").getKeys(false)) {
            try {
                sql = db_conn.prepareStatement("INSERT IGNORE INTO `"+prefix+"goalsreached` (goal) VALUES (?)");
                sql.setString(1,goal);
                sql.executeUpdate();
            } catch (SQLException e) {
                donationtracker.getLogger().info(e.toString());
            }
        }
    }

    // Constructor
    public Database () {
        // Set the DonationTracker instance variable
        donationtracker = DonationTracker.getInstance();
        // Get the table prefix (if there is one)
        if (donationtracker.getConfig().isSet("mysql.prefix")) {
            prefix = donationtracker.getConfig().getString("mysql.prefix");
        } else {
            prefix = "";
        }
        // Automatically call connect() when class is instantiated, and
        // create tables. We assume that if the database connection is
        // already present, then the tables are there too.
        if (db_conn == null) {
            connect();
            createTables();
            initialiseGoals();
        }
    }

    public void recordDonation(UUID uuid, Double amount) {
        // Reconnect the database if necessary
        if (connectionIsDead()) connect();
        PreparedStatement sql;
        try {
            sql = db_conn.prepareStatement("INSERT INTO `"+prefix+"donations` (donationtime,uuid,amount) VALUES (NOW(),?,?)");
            sql.setString(1,uuid.toString());
            sql.setBigDecimal(2, BigDecimal.valueOf(amount));
            sql.executeUpdate();
            sql.close();
        } catch (SQLException e) {
            donationtracker.getLogger().info(e.toString());
        }
    }
}

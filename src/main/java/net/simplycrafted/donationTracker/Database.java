package net.simplycrafted.donationTracker;

import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.UUID;

/**
 * Copyright © Brian Ronald
 * 28/06/14
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
@SuppressWarnings("SqlResolve")
public class Database {
    // MySQL database handler.
    // No other class should be talking to the database.
    // No other class should be constructing SQL statements.

    private static Connection db_conn;
    private static DonationTracker donationtracker;
    private String prefix = "";

    // Constructor
    public Database(final DonationTracker plugin) {
        // Set the donationTracker instance variable
        donationtracker = plugin;
        // Get the table prefix (if there is one)
        if(donationtracker.getConfig().isSet("mysql.prefix")) {
            prefix = donationtracker.getConfig().getString("mysql.prefix");
        } else {
            prefix = "";
        }
        // Automatically call connect() when class is instantiated, and
        // create tables. We assume that if the database connection is
        // already present, then the tables are there too.
        if(db_conn == null) {
            connect();
            createTables();
            initialiseGoals();
        }
    }

    // Call this once, when the plugin is being disabled. Called statically from onDisable()
    public static void disconnect() {
        try {
            donationtracker.getLogger().info("Closing MySQL database connection");
            db_conn.close();
        } catch(final Exception e) {
            donationtracker.getLogger().info("Tried to close the connection, but failed (possibly because it's already closed)");
        }
    }

    // We call this from the constructor, and whenever we find that the database has gone away
    public void connect() {
        donationtracker.getLogger().info("Opening MySQL database connection");
        final String hostname = donationtracker.getConfig().getString("mysql.hostname");
        final int port = donationtracker.getConfig().getInt("mysql.port");
        final String database = donationtracker.getConfig().getString("mysql.database");
        final String user = donationtracker.getConfig().getString("mysql.user");
        final String password = donationtracker.getConfig().getString("mysql.password");
        try {
            db_conn = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s", hostname, String.valueOf(port), database, user, password));
        } catch(final Exception e) {
            donationtracker.getLogger().info("donationTracker requires a MySQL database. Couldn't get connected.");
            donationtracker.getLogger().info(e.toString());
        }
    }

    public boolean connectionIsDead() {
        try {
            db_conn.createStatement().executeQuery("SELECT 1").close();
        } catch(final Exception e) {
            donationtracker.getLogger().info("Database connection went wrong");
            return true;
        }
        return false;
    }

    // Create any tables we need (if they don't exist)
    private void createTables() {
        final Statement sql;
        final ResultSet result;
        try {
            sql = db_conn.createStatement();
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS `" + prefix + "settings` (" +
                    "setting VARCHAR(10) PRIMARY KEY," +
                    "stringvalue VARCHAR(50)," +
                    "numericvalue DECIMAL(10,2)" +
                    ')');
            // Check if the database has been marked with a version by this plugin
            result = sql.executeQuery("SELECT numericvalue FROM `" + prefix + "settings` WHERE setting LIKE 'version'");
            if(!result.next()) {
                // The version setting doesn't exist; we probably just made the table, so mark it with our version.
                sql.executeUpdate("INSERT INTO `" + prefix + "settings` (setting,numericvalue) VALUES ('version','" + donationtracker.getDescription().getVersion() + "')");
            }
            result.close();
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS `" + prefix + "donation` (" +
                    "donationtime TIMESTAMP," +
                    "uuid CHAR(36)," +
                    "amount DECIMAL(10,2)" +
                    ')');
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS `" + prefix + "goalsreached` (" +
                    "goal VARCHAR(50) PRIMARY KEY," +
                    "reached ENUM('Y','N') DEFAULT 'N'" +
                    ')');
            sql.close();
        } catch(final SQLException e) {
            donationtracker.getLogger().info(e.toString());
        }
    }

    public void initialiseGoals() {
        final ConfigurationSection config = donationtracker.getConfig();
        PreparedStatement sql;
        for(final String goal : config.getConfigurationSection("goals").getKeys(false)) {
            try {
                sql = db_conn.prepareStatement("INSERT IGNORE INTO `" + prefix + "goalsreached` (goal) VALUES (?)");
                sql.setString(1, goal);
                sql.executeUpdate();
                sql.close();
            } catch(final SQLException e) {
                donationtracker.getLogger().info(e.toString());
            }
        }
    }

    public void recordDonation(final UUID uuid, final Double amount) {
        // Reconnect the database if necessary
        if(connectionIsDead()) {
            connect();
        }
        final PreparedStatement sql;
        try {
            sql = db_conn.prepareStatement("INSERT INTO `" + prefix + "donation` (donationtime,uuid,amount) VALUES (NOW(),?,?)");
            sql.setString(1, uuid.toString());
            sql.setBigDecimal(2, BigDecimal.valueOf(amount));
            sql.executeUpdate();
            sql.close();
        } catch(final SQLException e) {
            donationtracker.getLogger().info(e.toString());
        }
    }

    public boolean isGoalReached(final int days, final int money) {
        // Reconnect the database if necessary
        if(connectionIsDead()) {
            connect();
        }
        Boolean returnval = false;
        final PreparedStatement sql;
        try {
            sql = db_conn.prepareStatement("SELECT SUM(amount) " +
                    "FROM `" + prefix + "donation` " +
                    // Add WHERE clause only if days > 0. Zero now means "ever", and would ordinarily be meaningless.
                    (days > 0 ? "WHERE donationtime >= DATE_SUB(NOW(),INTERVAL ? DAY)" : ""));
            if(days > 0) {
                sql.setInt(1, days);
            }
            final ResultSet resultSet = sql.executeQuery();
            if(resultSet.next()) {
                returnval = resultSet.getInt(1) >= money;
            }
            resultSet.close();
            sql.close();
        } catch(final SQLException e) {
            donationtracker.getLogger().info(e.toString());
        }
        return returnval;
    }

    public double getPool(final int days) {
        // Reconnect the database if necessary
        if(connectionIsDead()) {
            connect();
        }
        final PreparedStatement sql;
        double returnval = 0.0;
        try {
            sql = db_conn.prepareStatement("SELECT SUM(amount) " +
                    "FROM `" + prefix + "donation` " +
                    // Add WHERE clause only if days > 0. Zero now means "ever", and would ordinarily be meaningless.
                    (days > 0 ? "WHERE donationtime >= DATE_SUB(NOW(),INTERVAL ? DAY)" : ""));
            if(days > 0) {
                sql.setInt(1, days);
            }
            final ResultSet resultSet = sql.executeQuery();
            if(resultSet.next()) {
                returnval += resultSet.getDouble(1);
            }
            resultSet.close();
            sql.close();
        } catch(final SQLException e) {
            donationtracker.getLogger().info(e.toString());
        }
        return returnval;
    }

    public boolean rewardsAreEnabled(final String name) {
        if(connectionIsDead()) {
            connect();
        }
        Boolean returnval = false;
        final PreparedStatement sql;
        try {
            sql = db_conn.prepareStatement("SELECT reached " +
                    "FROM `" + prefix + "goalsreached` " +
                    "WHERE goal LIKE ?");
            sql.setString(1, name);
            final ResultSet resultset = sql.executeQuery();
            if(resultset.next()) {
                returnval = resultset.getString(1).equals("Y");
            }
            resultset.close();
            sql.close();
        } catch(final SQLException e) {
            donationtracker.getLogger().info(e.toString());
        }
        return returnval;
    }

    public void recordReward(final String goalname, final boolean reached) {
        if(connectionIsDead()) {
            connect();
        }
        final PreparedStatement sql;
        try {
            sql = db_conn.prepareStatement("UPDATE `" + prefix + "goalsreached` " +
                    "SET reached = ?" +
                    "WHERE goal LIKE ?");
            sql.setString(1, reached ? "Y" : "N");
            sql.setString(2, goalname);
            sql.executeUpdate();
            sql.close();
        } catch(final SQLException e) {
            donationtracker.getLogger().info(e.toString());
        }
    }

    public void debugAdvance(final Integer days) {
        if(connectionIsDead()) {
            connect();
        }
        final PreparedStatement sql;
        try {
            sql = db_conn.prepareStatement("UPDATE `" + prefix + "donation` " +
                    "SET donationtime = DATE_SUB(donationtime,INTERVAL ? DAY) ");
            sql.setInt(1, days);
            sql.executeUpdate();
            sql.close();
        } catch(final SQLException e) {
            donationtracker.getLogger().info(e.toString());
        }
    }
}

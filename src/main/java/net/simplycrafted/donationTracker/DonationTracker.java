package net.simplycrafted.donationTracker;

import lombok.Getter;
import net.simplycrafted.donationTracker.cmd.CommandDGAdmin;
import net.simplycrafted.donationTracker.cmd.CommandDonation;
import net.simplycrafted.donationTracker.cmd.CommandDonationPool;
import net.simplycrafted.donationTracker.cmd.CommandDonorGoal;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * GENERAL TO-DO LIST:
 * -------------------
 * * Proper encapsulation
 * * Remove all those extra DB connections
 * * Test to make sure that changes all work
 * <p>
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
public class DonationTracker extends JavaPlugin {
    // List of instantiated goals
    @Getter
    private Map<String, Goal> goals;
    @Getter
    private Map<String, Goal> goalsBackwards;

    // Determine which goals need to be rewarded or otherwise
    public void assess(final boolean atDonationTime) {
        for(final Entry<String, Goal> stringGoalEntry : goals.entrySet()) {
            final Goal goal = stringGoalEntry.getValue();
            if(goal.reached()) {
                if(atDonationTime) {
                    goal.ondonate();
                }
                goal.enable();
            }
        }
        for(final Entry<String, Goal> stringGoalEntry : goalsBackwards.entrySet()) {
            final Goal goal = stringGoalEntry.getValue();
            if(!goal.reached()) {
                goal.abandon();
            }
        }
    }

    // Withdraw all rewards (used when closing down the plugin).
    public void withdraw() {
        for(final Entry<String, Goal> stringGoalEntry : goalsBackwards.entrySet()) {
            final Goal goal = stringGoalEntry.getValue();
            if(goal.reached()) {
                getLogger().info("Abandoning " + goal.getName());
                goal.abandon();
            }
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        goals = new TreeMap<>();
        goalsBackwards = new TreeMap<>(Collections.reverseOrder());
        // Bail out now if we have no database
        // TODO: Potentially extraneous DB connection
        final Database dbtest = new Database(this);
        if(dbtest.connectionIsDead()) {
            return;
        }
        getCommand("donation").setExecutor(new CommandDonation(this));
        getCommand("donorgoal").setExecutor(new CommandDonorGoal(this));
        getCommand("donationpool").setExecutor(new CommandDonationPool(this));
        getCommand("dgadmin").setExecutor(new CommandDGAdmin(this));

        // Load the goals from the config file
        ConfigurationSection goalConfig;
        final ConfigurationSection goalsConfig = getConfig().getConfigurationSection("goals");
        for(final String key : goalsConfig.getKeys(false)) {
            goalConfig = goalsConfig.getConfigurationSection(key);
            final Goal goal = new Goal(this, goalConfig);
            if(goal.reached()) {
                getLogger().info("...reached");
            }
            goals.put(key, goal);
            goalsBackwards.put(key, goal);
        }

        // Schedule a checker to examine these goals periodically
        final BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, () -> assess(false), 100, getConfig().getInt("period"));
    }

    @Override
    public void onDisable() {
        withdraw();
        Database.disconnect();
    }

    public void reload() {
        // Withdraw all goal rewards
        withdraw();
        // Re-initialise the goal hashes
        goals = new TreeMap<>();
        goalsBackwards = new TreeMap<>(Collections.reverseOrder());
        // Grab a new config
        reloadConfig();
        // Re-populate the goal hashes
        ConfigurationSection goalConfig;
        final ConfigurationSection goalsConfig = getConfig().getConfigurationSection("goals");
        for(final String key : goalsConfig.getKeys(false)) {
            goalConfig = goalsConfig.getConfigurationSection(key);
            final Goal goal = new Goal(this, goalConfig);
            if(goal.reached()) {
                getLogger().info("...reached");
            }
            goals.put(key, goal);
            goalsBackwards.put(key, goal);
        }
        // re-assess all goals, and reward
        assess(false);
    }
}
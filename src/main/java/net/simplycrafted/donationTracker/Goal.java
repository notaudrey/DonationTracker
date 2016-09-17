package net.simplycrafted.donationTracker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashSet;

/**
 * Copyright © Brian Ronald
 * 13/07/14
 * <p>
 * Goal class - one is instantiated for each donation goal. A donation
 * goal has a number of days and an amount; if the amount is reached
 * within the number of days, the effects of the goal are enabled. If
 * the number falls short, the effects are abandoned.
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
public class Goal {
    private static DonationTracker plugin;
    @Getter
    private final String name;
    private final Collection<Command> commandsOnEnabled = new HashSet<>();
    private final Collection<Command> commandsOnDisabled = new HashSet<>();
    private final Collection<Command> commandsOnDonate = new HashSet<>();
    @SuppressWarnings("FieldMayBeFinal")
    @Getter
    @Setter
    private int days;
    @SuppressWarnings("FieldMayBeFinal")
    @Getter
    @Setter
    private int money;
    // Constructor
    public Goal(final DonationTracker plugin, final ConfigurationSection goalConfig) {
        // Set the donationTracker instance variable
        Goal.plugin = plugin;

        // Re-usable Command variable
        //Command command;

        days = goalConfig.getInt("days");
        money = goalConfig.getInt("amount");
        name = goalConfig.getName();

        Goal.plugin.getLogger().info("Instantiating goal: " + name);

        // TODO: Test this properly
        /*// Build lists of Commands to be run when enabled
        for(final String commandString : goalConfig.getStringList("enable")) {
            command = new Command();
            if(commandString.indexOf(' ') > 0) {

                command.arg0 = commandString.substring(0, commandString.indexOf(' '));
                command.args = commandString.substring(commandString.indexOf(' ') + 1).split(" ");
            } else {
                command.arg0 = commandString;
                command.args = null;
            }
            commandsOnEnabled.add(command);
        }
        // Build lists of Commands to be run when disabled
        for(final String commandString : goalConfig.getStringList("disable")) {
            if(commandString.indexOf(' ') > 0) {
                command = new Command(commandString.substring(0, commandString.indexOf(' ')),
                        commandString.substring(commandString.indexOf(' ') + 1).split(" "));
            } else {
                command = new Command(commandString, null);
            }
            commandsOnDisabled.add(command);
        }
        // Build lists of Commands to be run only on donation, not reload, etc
        for(final String commandString : goalConfig.getStringList("atdonate")) {
            command = new Command();
            if(commandString.indexOf(' ') > 0) {
                command.arg0 = commandString.substring(0, commandString.indexOf(' '));
                command.args = commandString.substring(commandString.indexOf(' ') + 1).split(" ");
            } else {
                command.arg0 = commandString;
                command.args = null;
            }
            commandsOnDonate.add(command);
        }*/
        buildList(goalConfig, commandsOnEnabled, "enable");
        buildList(goalConfig, commandsOnDisabled, "disable");
        buildList(goalConfig, commandsOnDonate, "atdonate");
    }

    private void buildList(final ConfigurationSection goalConfig, final Collection<Command> commandList,
                           final String yamlLocation) {
        for(final String commandString : goalConfig.getStringList(yamlLocation)) {
            final Command command;
            if(commandString.indexOf(' ') > 0) {
                command = new Command(commandString.substring(0, commandString.indexOf(' ')),
                        commandString.substring(commandString.indexOf(' ') + 1).split(" "));
            } else {
                command = new Command(commandString, null);
            }
            commandList.add(command);
        }
    }

    public void enable() {
        // TODO: Connections...
        final Database database = new Database(plugin);
        // Check whether rewards have been enabled
        if(database.rewardsAreEnabled(name)) {
            return;
        }
        // Enable rewards
        plugin.getLogger().info("Enabling rewards: " + name);
        for(final Command command : commandsOnEnabled) {
            final PluginCommand pluginCommand = plugin.getServer().getPluginCommand(command.arg0);
            if(pluginCommand != null) {
                pluginCommand.execute(plugin.getServer().getConsoleSender(), command.arg0, command.args);
            } else {
                plugin.getLogger().info("Invalid command: " + command.arg0);
            }
        }
        // Record that rewards have been enabled
        database.recordReward(name, true);
    }

    public void ondonate() {
        // TODO: Connections...
        final Database database = new Database(plugin);
        if(database.rewardsAreEnabled(name)) {
            return;
        }
        // Enable one-off rewards
        for(final Command command : commandsOnDonate) {
            final PluginCommand pluginCommand = plugin.getServer().getPluginCommand(command.arg0);
            if(pluginCommand != null) {
                pluginCommand.execute(plugin.getServer().getConsoleSender(), command.arg0, command.args);
            } else {
                plugin.getLogger().info("Invalid command: " + command.arg0);
            }
        }
    }

    public void abandon() {
        // TODO: Connections...
        final Database database = new Database(plugin);
        // Check whether rewards have been disabled
        if(!database.rewardsAreEnabled(name)) {
            return;
        }
        // Disable rewards
        plugin.getLogger().info("Disabling rewards: " + name);
        for(final Command command : commandsOnDisabled) {
            final PluginCommand pluginCommand = plugin.getServer().getPluginCommand(command.arg0);
            if(pluginCommand != null) {
                pluginCommand.execute(plugin.getServer().getConsoleSender(), command.arg0, command.args);
            } else {
                plugin.getLogger().info("Invalid command: " + command.arg0);
            }
        }
        // Record that rewards have been disabled
        database.recordReward(name, false);
    }

    public boolean reached() {
        // Ask Database whether this goal has been reached
        // TODO: Connections...
        final Database database = new Database(plugin);
        return database.isGoalReached(days, money);
    }

    /**
     * Simple container for a command's name and its arguments
     */
    @Getter
    @AllArgsConstructor
    private class Command {
        private String arg0;
        private String[] args;
    }
}
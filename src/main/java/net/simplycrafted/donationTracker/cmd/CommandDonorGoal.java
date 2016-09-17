package net.simplycrafted.donationTracker.cmd;

import net.simplycrafted.donationTracker.DonationTracker;
import net.simplycrafted.donationTracker.Goal;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * @author audrey
 * @since 9/16/16.
 */
public class CommandDonorGoal implements CommandExecutor {
    // TODO: Configurable
    private final String chatPrefix = "" + ChatColor.BOLD + ChatColor.GOLD + "[DC] " + ChatColor.RESET;
    private final DonationTracker plugin;

    public CommandDonorGoal(final DonationTracker plugin) {
        this.plugin = plugin;
    }

    // TODO: Gotta clean the everliving FUCK out of this ;^;
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String s, final String[] args) {
        final Configuration config = plugin.getConfig();
        if(args.length == 0) {
            // List the goals
            sender.sendMessage(chatPrefix + "Donation goals:");
            for(final String goal : config.getConfigurationSection("goals").getKeys(false)) {
                sender.sendMessage(chatPrefix + String.format("Goal %s: $%.2f in %d days",
                        goal,
                        config.getDouble("goals." + goal + ".amount"),
                        config.getInt("goals." + goal + ".days"))
                );
            }
        } else if(args.length == 1) {
            // Show details of a specific goal
            final String goalName = args[0];
            // Get its details from the config
            final ConfigurationSection goalConfig = config.getConfigurationSection("goals." + goalName);
            if(goalConfig != null) {
                // Goal is in the config ; get details from there (ignoring actual loaded goal)
                sender.sendMessage(chatPrefix + String.format("Goal %s: $%.2f in %d days",
                        goalName,
                        goalConfig.getDouble("amount"),
                        goalConfig.getInt("days"))
                );
                for(final String commandToEnable : goalConfig.getStringList("enable")) {
                    sender.sendMessage(chatPrefix + "Reward: " + ChatColor.WHITE + commandToEnable);
                }
                for(final String commandToDisable : goalConfig.getStringList("disable")) {
                    sender.sendMessage(chatPrefix + "Withdraw: " + ChatColor.WHITE + commandToDisable);
                }
            } else {
                sender.sendMessage(chatPrefix + "Goal " + goalName + " not found. You can create it by setting days or amount");
            }
        } else if(args.length > 1 && args[1].toLowerCase().matches("^(amount|days|enable|disable|clear)$")) {
            // Given a further argument, matched to a valid command
            final String goalName = args[0];
            // This command can implicitly create goals, so if there's no config, create it
            ConfigurationSection goalConfig = config.getConfigurationSection("goals." + goalName);
            if(goalConfig == null) {
                goalConfig = config.createSection("goals." + goalName);
            }
            if(args.length > 2) {
                if(args[1].equalsIgnoreCase("amount")) {
                    // Given "amount" and a further arg, adjust the amount
                    try {
                        final int amount;
                        amount = Integer.parseInt(args[2]);
                        if(amount <= 0.0) {
                            sender.sendMessage(chatPrefix + "Amount must be a positive number");
                        } else {
                            goalConfig.set("amount", amount);
                            plugin.saveConfig();
                            Goal goal = plugin.getGoals().get(goalName);
                            if(goal == null) {
                                // goal object doesn't exist, so create a new one from scratch
                                sender.sendMessage(chatPrefix + "Creating new goal: " + goalName);
                                sender.sendMessage(chatPrefix + "Amount: $" + amount);
                                // use default value for days
                                final int days = config.getInt("defaultgoaldays");
                                sender.sendMessage(chatPrefix + "Days: " + days);
                                goalConfig.set("days", days);
                                goal = new Goal(plugin, goalConfig);
                                // insert new goal into the global Maps
                                plugin.getGoals().put(goalName, goal);
                                plugin.getGoalsBackwards().put(goalName, goal);
                            } else {
                                sender.sendMessage(chatPrefix + "Amount: $" + amount);
                                goal.setMoney(amount);
                            }
                        }
                    } catch(final IllegalArgumentException e) {
                        sender.sendMessage(chatPrefix + "You must specify a valid number");
                    }
                } else if(args[1].equalsIgnoreCase("days")) {
                    // given "days" and a further arg, adjust the days
                    try {
                        final Integer days = Integer.parseInt(args[2]);
                        if(days < 0) {
                            sender.sendMessage(chatPrefix + "Days must be a positive number, or 0 for \"forever\"");
                        } else {
                            goalConfig.set("days", days);
                            plugin.saveConfig();
                            Goal goal = plugin.getGoals().get(goalName);
                            if(goal == null) {
                                // goal object doesn't exist, so create a new one from scratch
                                sender.sendMessage(chatPrefix + "Creating new goal: " + goalName);
                                sender.sendMessage(chatPrefix + "Days: " + days);
                                goal = new Goal(plugin, goalConfig);
                                // insert new goal into the global Maps
                                plugin.getGoals().put(goalName, goal);
                                plugin.getGoalsBackwards().put(goalName, goal);
                            } else {
                                sender.sendMessage(chatPrefix + "Days: " + days);
                                goal.setDays(days);
                            }
                        }
                    } catch(final IllegalArgumentException e) {
                        sender.sendMessage(chatPrefix + "You must specify a valid number");
                    }
                } else if(args[1].equalsIgnoreCase("enable")) {
                    // given "enable" and further args, which are a command and its args
                    // Storing that as a string in the config, so StringBuilder to build it from args
                    final StringBuilder stringBuilder = new StringBuilder();
                    for(int i = 2; i < args.length; i++) {
                        // looping from 2 because the first two arguments are the goal's name and "enable"
                        stringBuilder.append(args[i]);
                        if(i < args.length) {
                            stringBuilder.append(' ');
                        }
                    }
                    // This is how to add to a list in config. Instantiate a list of strings,
                    // add to that instance, and then replace the stored list with that new one.
                    final List<String> enableCommands = goalConfig.getStringList("enable");
                    enableCommands.add(stringBuilder.toString());
                    goalConfig.set("enable", enableCommands);
                    plugin.saveConfig();
                    sender.sendMessage(chatPrefix + "Command added to goal's enable list in config. Check days and amount, then reload config to enact changes.");
                } else if(args[1].equalsIgnoreCase("disable")) {
                    // given "disable" and further args, which are a command and its args
                    // Storing that as a string in the config, so StringBuilder to build it from args
                    final StringBuilder stringBuilder = new StringBuilder();
                    for(int i = 2; i < args.length; i++) {
                        // looping from 2 because the first two arguments are the goal's name and "disable"
                        stringBuilder.append(args[i]);
                        if(i < args.length) {
                            stringBuilder.append(' ');
                        }
                    }
                    // This is how to add to a list in config. Instantiate a list of strings,
                    // add to that instance, and then replace the stored list with that new one.
                    final List<String> disableCommands = goalConfig.getStringList("disable");
                    disableCommands.add(stringBuilder.toString());
                    goalConfig.set("disable", disableCommands);
                    plugin.saveConfig();
                    sender.sendMessage(chatPrefix + "Command added to goal's disable list in config. Check days and amount, then reload config to enact changes.");
                } else if(args[1].equalsIgnoreCase("clear")) {
                    // Future: Allow parts of a goal to be selectively cleared. Until then, edit config.yml and reload.
                    sender.sendMessage(chatPrefix + "The clear command doesn't have any options yet.");
                }
            } else {
                // Sub-commands were given but no further arguments; mostly return usage help
                if(args[1].equalsIgnoreCase("amount")) {
                    sender.sendMessage(chatPrefix + "You must specify an amount in dollars");
                } else if(args[1].equalsIgnoreCase("days")) {
                    sender.sendMessage(chatPrefix + "You must specify a period in whole days, with 0 meaning \"forever\"");
                } else if(args[1].equalsIgnoreCase("enable")) {
                    sender.sendMessage(chatPrefix + "You must specify a command to run when enabled");
                } else if(args[1].equalsIgnoreCase("disable")) {
                    sender.sendMessage(chatPrefix + "You must specify a command to run when disabled");
                } else if(args[1].equalsIgnoreCase("clear")) {
                    // "donorgoal <goal> clear" will totally eradicate a goal. Takes five steps:
                    // 1. Cancel all rewards granted by this goal.
                    plugin.getGoals().get(goalName).abandon();
                    // 2,3. Remove goal from the global Maps.
                    plugin.getGoals().remove(goalName);
                    plugin.getGoalsBackwards().remove(goalName);
                    // 4. Delete its configuration section.
                    plugin.getConfig().set("goals." + goalName, null);
                    // 5. Save the config.
                    plugin.saveConfig();
                    sender.sendMessage(chatPrefix + "Cleared goal from config, and erased it from memory.");
                }
            }
        } else {
            return false;
        }
        return true;
    }
}
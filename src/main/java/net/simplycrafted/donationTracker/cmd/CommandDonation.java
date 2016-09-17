package net.simplycrafted.donationTracker.cmd;

import net.simplycrafted.donationTracker.Database;
import net.simplycrafted.donationTracker.DonationTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import java.util.UUID;

/**
 * @author audrey
 * @since 9/16/16.
 */
public class CommandDonation implements CommandExecutor {
    // TODO: Configurable
    private final String chatPrefix = "" + ChatColor.BOLD + ChatColor.GOLD + "[DC] " + ChatColor.RESET;
    private final DonationTracker plugin;

    public CommandDonation(final DonationTracker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String s, final String[] args) {
        // TODO: Extraneous DB connection... Will prob. kill performance and stuff every time it's run ._.
        // Note: Depends on how fast the MySQL server is, so...
        final Database database = new Database(plugin);

        if(command.getName().equalsIgnoreCase("donation")) {
            if(args.length == 2) {
                UUID uuid = null;
                Double amount;
                try {
                    // See if the argument is a UUID
                    uuid = UUID.fromString(args[0]);
                } catch(final IllegalArgumentException e) {
                    // If that threw an error, assume it's a name
                    for(final OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                        // Search all the known players
                        if(player.getName().equalsIgnoreCase(args[0])) {
                            uuid = player.getUniqueId();
                        }
                    }
                }
                try {
                    amount = Double.parseDouble(args[1]);
                } catch(final IllegalArgumentException e) {
                    amount = 0.0;
                }
                if(uuid == null || amount <= 0.0) {
                    if(uuid == null) {
                        sender.sendMessage(chatPrefix + "Could not get UUID for " + args[0]);
                    }
                    if(amount <= 0.0) {
                        sender.sendMessage(chatPrefix + args[1] + " is not a valid positive number");
                    }
                } else {
                    database.recordDonation(uuid, amount);
                    sender.sendMessage(String.format(chatPrefix + "Logged $%.2f against UUID %s", amount, uuid.toString()));

                    // Get the donation thanks command from the config and, um, run it...
                    String thankCommand = plugin.getConfig().getString("donationthanks");
                    thankCommand = thankCommand.replaceFirst("PLAYER", plugin.getServer().getOfflinePlayer(uuid).getName()).replaceFirst("AMOUNT", amount.toString());
                    final String tcarg0 = thankCommand.substring(0, thankCommand.indexOf(' '));
                    final String[] tcargs = thankCommand.substring(thankCommand.indexOf(' ') + 1).split(" ");
                    // Have the plugin re-test all goals now
                    final PluginCommand pluginCommand = plugin.getServer().getPluginCommand(tcarg0);
                    if(pluginCommand != null) {
                        pluginCommand.execute(plugin.getServer().getConsoleSender(), tcarg0, tcargs);
                    } else {
                        plugin.getLogger().info("Invalid command: " + tcarg0);
                    }
                    plugin.assess(true);
                }
            } else {
                return false;
            }
        }
        return true;
    }
}

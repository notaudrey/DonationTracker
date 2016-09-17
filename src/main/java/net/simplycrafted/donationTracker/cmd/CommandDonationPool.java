package net.simplycrafted.donationTracker.cmd;

import net.simplycrafted.donationTracker.Database;
import net.simplycrafted.donationTracker.DonationTracker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * @author audrey
 * @since 9/16/16.
 */
public class CommandDonationPool implements CommandExecutor {
    private final String chatPrefix = "" + ChatColor.BOLD + ChatColor.GOLD + "[DC] " + ChatColor.RESET;
    private final DonationTracker plugin;

    public CommandDonationPool(final DonationTracker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String s, final String[] args) {
        // TODO: Extraneous DB connection... Will prob. kill performance and stuff every time it's run ._.
        // Note: Depends on how fast the MySQL server is, so...
        final Database database = new Database(plugin);

        Integer days;
        if(args.length == 0) {
            // Get the default, since no number was specified
            days = plugin.getConfig().getInt("defaultgoaldays");
        } else {
            try {
                // Use the number specified
                days = Integer.parseInt(args[0]);
                plugin.getLogger().info(days.toString());
            } catch(final IllegalArgumentException e) {
                // Number specified was useless; use default instead
                sender.sendMessage(chatPrefix + "Not a valid number ; using default");
                days = plugin.getConfig().getInt("defaultgoaldays");
            }
        }
        if(days == 0) {
            sender.sendMessage(chatPrefix + String.format("Donation pool for all time: %.00f", database.getPool(0)));
        } else {
            sender.sendMessage(chatPrefix + String.format("Donation pool for past %d days: %.00f", days, database.getPool(days)));
        }
        return true;
    }
}

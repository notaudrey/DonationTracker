package net.simplycrafted.donationTracker.cmd;

import net.simplycrafted.donationTracker.Database;
import net.simplycrafted.donationTracker.DonationTracker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * @author audrey
 * @since 9/16/16.
 */
public class CommandDGAdmin implements CommandExecutor {
    private final DonationTracker plugin;

    public CommandDGAdmin(final DonationTracker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String s, final String[] args) {
        // TODO: Extraneous DB connection... Will prob. kill performance and stuff every time it's run ._.
        // Note: Depends on how fast the MySQL server is, so...
        final Database database = new Database(plugin);

        // Debugging and testing only - to be removed before release
        if(args.length > 0) {
            if(args[0].equalsIgnoreCase("advance")) {
                if(args.length > 1) {
                    try {
                        database.debugAdvance(Integer.valueOf(args[1]));
                    } catch(final Exception e) {
                        sender.sendMessage("Bad number.");
                    }
                    return true;
                } else {
                    return true;
                }
            } else if(args[0].equalsIgnoreCase("assess")) {
                // Iterate over all the goals
                plugin.assess(false);
                return true;
            } else if(args[0].equalsIgnoreCase("withdraw")) {
                // Iterate over all the goals
                plugin.withdraw();
                return true;
            } else if(args[0].equalsIgnoreCase("reload")) {
                // Reload the plugin
                plugin.reload();
                return true;
            } else if(args[0].equalsIgnoreCase("save")) {
                // Explicitly save config.
                plugin.saveConfig();
                return true;
            } else {
                sender.sendMessage("Unknown admin command.");
            }
        } else {
            return false;
        }
        return true;
    }
}

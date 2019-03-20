package net.okocraft.renamedplayers.command;

import lombok.NonNull;
import lombok.val;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.okocraft.renamedplayers.RenamedPlayers;
import net.okocraft.renamedplayers.database.Database;

public class RenamedPlayersCommand implements CommandExecutor {
    private Database database;

    public RenamedPlayersCommand(Database database) {
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // insufficient permission
        if (!sender.hasPermission("renamedplayers.admin")) {
            sender.sendMessage(":PERM_INSUFFICIENT");
            return true;
        }

        // only /rp
        if (args.length == 0) {
            sender.sendMessage(":PARAM_INSUFFICIENT");
            return false;
        }

        @NonNull
        val subCommand = args[0];

        // rp version
        if (subCommand.equalsIgnoreCase("version")) {
             sender.sendMessage(RenamedPlayers.getInstance().getVersion());

             return true;
        }

        // rp write
        if (subCommand.equalsIgnoreCase("write")) {

            if(!(sender instanceof Player)) return true;
            // NOTE: For testing
            val uuid = ((Player) sender).getUniqueId();
            val name = ((Player) sender).getName();

            database.addRecord(uuid, name);
            sender.sendMessage(":ADDED_PLAYER");

            return true;
        }

        // rp read <uuid> <column>
        if (subCommand.equalsIgnoreCase("read")) {
            if (args.length < 3) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val entry = args[1];
            val column = args[2];

            sender.sendMessage(database.readRecord(entry, column));

            return true;
        }

        // rp set <column> <player> <value>
        if (subCommand.equalsIgnoreCase("set")) {
            if (args.length < 4) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val column = args[1];
            val player = args[2];
            val value = args[3];

            database.setRecord(player, column, value);
            

            return true;
        }

        sender.sendMessage(":PARAM_UNKNOWN");
        return true;
    }

    public static String checkEntryType(String entry){
        return entry.matches("([a-z]|\\d){8}-([a-z]|\\d){4}-([a-z]|\\d){4}-([a-z]|\\d){4}-([a-z]|\\d){12}") ? "uuid" : "player";
    }
}

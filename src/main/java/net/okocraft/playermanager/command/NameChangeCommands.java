package net.okocraft.playermanager.command;

import net.okocraft.playermanager.PlayerManager;
import net.okocraft.playermanager.database.Database;
import net.okocraft.playermanager.database.PlayerTable;
import net.okocraft.playermanager.utilities.ConfigManager;
import org.bukkit.command.CommandSender;

class NameChangeCommands {

    private static final PlayerManager instance = PlayerManager.getInstance();
    private static final ConfigManager configManager = instance.getConfigManager();
    private static final Database database = instance.getDatabase();
    private static final PlayerTable playerTable = database.getPlayerTable();

    static boolean nameChangeCommands(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }

        final String subNameChangeCommand = args[1].toLowerCase();

        switch (subNameChangeCommand) {
            case "previousname":
            case "previous":
                if (!Commands.hasPermission(sender, "playermanager.namechange.previousname")) {
                    return false;
                }
                return previousNameCommand(sender, args);
        }

        return true;
    }

    private static boolean previousNameCommand(CommandSender sender, String[] args) {
        if (args.length <= 2) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }
        if (!playerTable.existPlayer(args[2])) {
            return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
        }
        String previousName = playerTable.getPlayerData("previous", args[2]);
        if (previousName.equals("")) {
            previousName = args[2];
        }
        sender.sendMessage(previousName);
        return true;
    }
}
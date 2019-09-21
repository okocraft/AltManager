package net.okocraft.playermanager.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import net.okocraft.playermanager.PlayerManager;
import net.okocraft.playermanager.database.Database;
import net.okocraft.playermanager.database.PlayerTable;
import net.okocraft.playermanager.utilities.ConfigManager;

class DatabaseCommands {
    
    private static final PlayerManager instance = PlayerManager.getInstance();
    private static final ConfigManager configManager = instance.getConfigManager();
    private static final Database database = instance.getDatabase();
    private static final PlayerTable playerTable = database.getPlayerTable();
    
    static boolean databaseCommands(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }

        final String subDatabaseCommand = args[1].toLowerCase();
        if (subDatabaseCommand.equalsIgnoreCase("gettablemap")) {
            if (!Commands.hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            sender.sendMessage("テーブルリスト");
            database.getTableMap().forEach((tableName, tableType) -> {
                sender.sendMessage(tableName + " - " + tableType);
                sender.sendMessage(database.getPrimaryKeyColumnName(tableName));
            });
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("resetconnection")) {
            if (!Commands.hasPermission(sender, "playermanager.database." + subDatabaseCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }

            database.resetConnection();
            sender.sendMessage("§eデータベースへの接続をリセットしました。");
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("addplayer")) {
            if (!Commands.hasPermission(sender, "playermanager.database." + subDatabaseCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }

            if (args.length == 2) {
                return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
            }

            @SuppressWarnings("deprecation")
            OfflinePlayer player = Bukkit.getOfflinePlayer(args[2]);
            if (!player.hasPlayedBefore())
                return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));

            String uuid = player.getUniqueId().toString();
            String name = player.getName();
            if (name != null) {
                playerTable.addPlayer(uuid, name, true);
                sender.sendMessage(configManager.getDatabaseAddPlayerSuccessMsg().replaceAll("%uuid%", uuid)
                        .replaceAll("%player%", name));
            } else {
                sender.sendMessage("&c* プレイヤーの名前を取得できませんでした。");
            }
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("removeplayer")) {
            if (!Commands.hasPermission(sender, "playermanager.database." + subDatabaseCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }

            if (args.length == 2) {
                return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
            }

            if (!playerTable.existPlayer(args[2])) {
                return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
            }

            String uuid = playerTable.getPlayerData("uuid", args[2]);
            String name = playerTable.getPlayerData("player", args[2]);

            playerTable.removePlayer(args[2]);
            sender.sendMessage(configManager.getDatabaseRemovePlayerSuccessMsg().replaceAll("%uuid%", uuid)
                    .replaceAll("%player%", name));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("existplayer")) {
            if (!Commands.hasPermission(sender, "playermanager.database." + subDatabaseCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }
            if (args.length == 2) {
                return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
            }

            sender.sendMessage(String.valueOf(playerTable.existPlayer(args[2])));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("set")) {
            if (!Commands.hasPermission(sender, "playermanager.database." + subDatabaseCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }

            if (args.length < 5) {
                return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
            }

            if (!playerTable.existPlayer(args[3])) {
                return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[3]));
            }

            String uuid = playerTable.getPlayerData("uuid", args[3]);
            String name = playerTable.getPlayerData("player", args[3]);

            if (!database.getColumnMap(playerTable.getPlayerTableName()).containsKey(args[2]))
                return Commands.errorOccurred(sender, configManager.getDatabaseNoColumnFoundMsg());

            playerTable.setPlayerData(args[2], args[3], args[4]);

            sender.sendMessage(configManager.getDatabaseSetValueSuccessMsg().replaceAll("%uuid%", uuid)
                    .replaceAll("%player%", name).replaceAll("%column%", args[2]).replaceAll("%Value%", args[4]));

            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("get")) {
            if (!Commands.hasPermission(sender, "playermanager.database." + subDatabaseCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }

            if (args.length < 4) {
                return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
            }

            sender.sendMessage(playerTable.getPlayerData(args[2], args[3]));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("addcolumn")) {
            if (!Commands.hasPermission(sender, "playermanager.database." + subDatabaseCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }

            if (args.length < 5) {
                return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
            }

            if (args[4].equalsIgnoreCase("null"))
                database.addColumn(playerTable.getPlayerTableName(), args[2], args[3], null, true);
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("dropcolumn")) {
            if (!Commands.hasPermission(sender, "playermanager.database." + subDatabaseCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }

            if (args.length == 2) {
                return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
            }

            database.dropColumn(playerTable.getPlayerTableName(), args[2]);
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("getcolumnmap")) {
            if (!Commands.hasPermission(sender, "playermanager.database." + subDatabaseCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }

            sender.sendMessage("列リスト");
            database.getColumnMap(playerTable.getPlayerTableName()).forEach((columnName, columnType) -> sender.sendMessage(columnName + " - " + columnType));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("getplayersmap")) {
            if (!Commands.hasPermission(sender, "playermanager.database." + subDatabaseCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }

            sender.sendMessage("記録されているプレイヤー");
            playerTable.getPlayersMap().forEach((uuidStr, name) -> sender.sendMessage(uuidStr + " - " + name));
            return true;
        }
        return Commands.errorOccurred(sender, configManager.getInvalidArgMsg());
    }

}
package net.okocraft.playermanager.command;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.common.primitives.Ints;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.okocraft.playermanager.PlayerManager;
import net.okocraft.playermanager.database.Database;
import net.okocraft.playermanager.database.PlayerTable;
import net.okocraft.playermanager.utilities.ConfigManager;
import net.okocraft.playermanager.utilities.InventoryUtil;

public class Commands implements CommandExecutor {
    private PlayerManager instance;
    private ConfigManager configManager;
    private Database database;
    private PlayerTable playerTable;

    public Commands(Plugin plugin, Database database) {
        this.instance = (PlayerManager) plugin;
        this.configManager = instance.getConfigManager();
        this.database = database;
        this.playerTable = database.getPlayerTable();

        instance.getCommand("playermanager").setExecutor(this);
        instance.getCommand("playermanager").setTabCompleter(new PlayerManagerTabCompleter(database));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0)
            return errorOccurred(sender, configManager.getNoEnoughArgMsg());

        final String subCommand = args[0].toLowerCase();

        switch (subCommand) {
        case "inventory":
            if (!hasPermission(sender, "playermanager." + subCommand))
                return false;
            return inventoryCommand(sender, command, label, args, false);
        case "enderchest":
            if (!hasPermission(sender, "playermanager." + subCommand))
                return false;
            return inventoryCommand(sender, command, label, args, true);
        case "database":
            if (!hasPermission(sender, "playermanager." + subCommand))
                return false;
            return databaseCommand(sender, command, label, args);
        case "alt":
            if (!hasPermission(sender, "playermanager." + subCommand))
                return false;
            return altCommand(sender, command, label, args);
        case "namechange":
            if (!hasPermission(sender, "playermanager." + subCommand))
                return false;
            return nameChangeCommand(sender, command, label, args);
        }
        return errorOccurred(sender, configManager.getCommandNotExistMsg().replaceAll("%command%", subCommand));
    }

    private boolean inventoryCommand(CommandSender sender, Command command, String label, String[] args,
            boolean isEnderChest) {
        String type = isEnderChest ? "enderchest" : "inventory";
        if (args.length == 1)
            return errorOccurred(sender, configManager.getNoEnoughArgMsg());
        String subInventoryCommand = args[1].toLowerCase();

        // /pman inventory searchbackup <player>
        if (subInventoryCommand.equals("searchbackup")) {
            if (!hasPermission(sender, "playermanager." + type + "." + subInventoryCommand))
                return false;
            if (args.length <= 2)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());
            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);

            if (!offlinePlayer.hasPlayedBefore())
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));

            Integer page = null;
            if (args.length >= 4)
                page = Ints.tryParse(args[3]);
            if (args.length == 3 || page == null)
                page = 1;

            sender.sendMessage(configManager.getPageFooter().replaceAll("%page%", page.toString()));
            InventoryUtil
                    .searchFile(instance.getDataFolder().toPath().resolve(type).toFile(),
                            offlinePlayer.getUniqueId().toString() + ".log")
                    .stream()
                    .map(path -> LocalDateTime.parse(path.getParent().toFile().getName(), InventoryUtil.getFormat()))
                    .sorted(Comparator.reverseOrder()).skip((page - 1) * 9).limit(9)
                    .forEach(date -> sender.sendMessage(date.format(InventoryUtil.getFormat())));
            return true;
        }

        // /pman inventory showbackup <player> [year] [month] [day] [hour] [minute]
        // [second]
        if (subInventoryCommand.equals("showbackup")) {
            if (!hasPermission(sender, "playermanager." + type + "." + subInventoryCommand))
                return false;

            if (!(sender instanceof Player))
                return errorOccurred(sender, configManager.getSenderMustBePlayerMsg());

            if (args.length <= 2)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);
            if (!offlinePlayer.hasPlayedBefore())
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));

            if (!sender.hasPermission("playermanager." + type + ".showbackup.other")
                    && !sender.getName().equalsIgnoreCase(args[2]))
                return errorOccurred(sender, configManager.getNoPermMsg());

            if (args.length == 3) {
                ((Player) sender)
                        .openInventory(InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest)));
            }

            Integer year = null;
            if (args.length >= 4) {
                year = Ints.tryParse(args[3]);
                if (year == null) {
                    ((Player) sender).openInventory(
                            InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest)));
                    return true;
                }
                if (args.length == 4) {
                    ((Player) sender).openInventory(
                            InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year)));
                    return true;
                }
            }

            Integer month = null;
            if (args.length >= 5) {
                month = Ints.tryParse(args[4]);
                if (month == null) {
                    ((Player) sender).openInventory(
                            InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year)));
                    return true;
                }
                if (args.length == 5)
                    ((Player) sender).openInventory(InventoryUtil
                            .fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year, month)));
            }

            Integer day = null;
            if (args.length >= 6) {
                day = Ints.tryParse(args[5]);
                if (day == null) {
                    ((Player) sender).openInventory(InventoryUtil
                            .fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year, month)));
                    return true;
                }
                if (args.length == 6)
                    ((Player) sender).openInventory(InventoryUtil
                            .fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year, month, day)));
            }

            Integer hour = null;
            if (args.length >= 7) {
                hour = Ints.tryParse(args[6]);
                if (hour == null) {
                    ((Player) sender).openInventory(InventoryUtil
                            .fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year, month, day)));
                    return true;
                }
                if (args.length == 7)
                    ((Player) sender).openInventory(InventoryUtil
                            .fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year, month, day, hour)));
            }

            Integer minute = null;
            if (args.length >= 8) {
                minute = Ints.tryParse(args[7]);
                if (minute == null) {
                    ((Player) sender).openInventory(InventoryUtil
                            .fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year, month, day, hour)));
                    return true;
                }
                if (args.length == 8)
                    ((Player) sender).openInventory(InventoryUtil.fromBase64(
                            InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year, month, day, hour, minute)));
            }

            Integer second = null;
            if (args.length == 9) {
                second = Ints.tryParse(args[8]);
                if (second == null) {
                    ((Player) sender).openInventory(InventoryUtil.fromBase64(
                            InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year, month, day, hour, minute)));
                    return true;
                }
                ((Player) sender).openInventory(InventoryUtil.fromBase64(
                        InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year, month, day, hour, minute, second)));
                return true;
            }

            return true;
        }

        // rollback player's inventory from speficied backup.
        if (subInventoryCommand.equals("rollback")) {
            if (!hasPermission(sender, "playermanager." + type + "." + subInventoryCommand))
                return false;

            if (args.length <= 6)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            Player player = Bukkit.getPlayer(args[2]);
            if (player == null)
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));

            Integer year = Ints.tryParse(args[3]);
            Integer month = Ints.tryParse(args[4]);
            Integer day = Ints.tryParse(args[5]);
            Integer hour = Ints.tryParse(args[6]);
            Integer minute = Ints.tryParse(args[7]);
            Integer second = Ints.tryParse(args[8]);

            if (year == null || month == null || day == null || hour == null || minute == null || second == null)
                return errorOccurred(sender, configManager.getInvalidArgMsg());
            Optional<File> backup = InventoryUtil.getBackupFile((OfflinePlayer) player, isEnderChest, year, month, day,
                    hour, minute, second);
            if (backup.isPresent()) {
                InventoryUtil.restoreInventory(player, isEnderChest, backup.get());
            } else {
                return errorOccurred(sender, configManager.getInvalidBackupFileMsg());
            }
            return true;
        }

        // create backup file.
        if (subInventoryCommand.equals("backup")) {
            if (!hasPermission(sender, "playermanager." + type + "." + subInventoryCommand))
                return false;
            if (args.length <= 2) {
                if (!(sender instanceof Player))
                    return errorOccurred(sender, configManager.getInvalidArgMsg());
                InventoryUtil.backupInventory((Player) sender, isEnderChest);
                sender.sendMessage(
                        configManager.getInventoryBackupSuccessMsg().replaceAll("%player%", sender.getName()));
            } else {
                Player player = Bukkit.getPlayer(args[2]);
                if (player == null)
                    return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
                InventoryUtil.backupInventory(player, false);
                sender.sendMessage(configManager.getInventoryBackupSuccessMsg().replaceAll("%player%", args[2]));
            }
            return true;
        }

        return errorOccurred(sender, configManager.getInvalidArgMsg());
    }

    private boolean altCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }

        final String subNameChangeCommand = args[1].toLowerCase();

        if (subNameChangeCommand.equals("getip")) {
            if (!hasPermission(sender, "playermanager.alt." + subNameChangeCommand))
                return false;
            if (args.length == 2) {
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());
            }
            if (!playerTable.existPlayer(args[2])) {
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
            }
            String address = playerTable.getPlayerData("address", args[2]);
            sender.sendMessage(address);
            return true;
        }

        if (subNameChangeCommand.equals("search")) {
            if (!hasPermission(sender, "playermanager.alt." + subNameChangeCommand))
                return false;
            if (args.length == 2) {
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());
            }
            if (!playerTable.existPlayer(args[2])) {
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
            }
            String address = playerTable.getPlayerData("address", args[2]);

            List<String> alts = database.get(playerTable.getPlayerTableName(), "player", "address", address);

            if (alts.size() == 1) {
                sender.sendMessage(
                        configManager.getShowAltsOnJoinMsg().replaceAll("%player%", args[2]).replaceAll("%alts%", ""));
                return true;
            }

            StringBuilder sb = new StringBuilder();
            alts.forEach(playerName -> {
                if (!playerName.equals(args[2]))
                    sb.append(playerName + ", ");
            });
            sender.sendMessage(configManager.getShowAltsOnJoinMsg().replaceAll("%player%", args[2]).replaceAll("%alts%",
                    sb.substring(0, sb.length() - 2).toString()));
            return true;
        }

        return true;
    }

    private boolean nameChangeCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }

        final String subNameChangeCommand = args[1].toLowerCase();
        if (subNameChangeCommand.equals("previousname")) {
            if (!hasPermission(sender, "playermanager.namechange." + subNameChangeCommand))
                return false;
            if (args.length == 2) {
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());
            }
            if (!playerTable.existPlayer(args[2])) {
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
            }
            String previousName = playerTable.getPlayerData("previous", args[2]);
            if (previousName.equals("")) previousName = args[2];
            sender.sendMessage(previousName);
            return true;
        }

        return true;
    }

    private boolean databaseCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1)
            return errorOccurred(sender, configManager.getNoEnoughArgMsg());

        final String subDatabaseCommand = args[1].toLowerCase();
        if (subDatabaseCommand.equalsIgnoreCase("gettablemap")) {
            if (!hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            sender.sendMessage("テーブルリスト");
            database.getTableMap().forEach((tableName, tableType) -> {
                sender.sendMessage(tableName + " - " + tableType);
                sender.sendMessage(database.getPrimaryKeyColumnName(tableName));
            });
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("resetconnection")) {
            if (!hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            database.resetConnection();
            sender.sendMessage("§eデータベースへの接続をリセットしました。");
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("addplayer")) {
            if (!hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            if (args.length == 2)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            @SuppressWarnings("deprecation")
            OfflinePlayer player = Bukkit.getOfflinePlayer(args[2]);
            if (!player.hasPlayedBefore())
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));

            String uuid = player.getUniqueId().toString();
            String name = player.getName();
            playerTable.addPlayer(uuid, name, true);
            sender.sendMessage(configManager.getDatabaseAddPlayerSuccessMsg().replaceAll("%uuid%", uuid)
                    .replaceAll("%player%", name));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("removeplayer")) {
            if (!hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            if (args.length == 2)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            if (!playerTable.existPlayer(args[2]))
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
            String uuid = playerTable.getPlayerData("uuid", args[2]);
            String name = playerTable.getPlayerData("player", args[2]);

            playerTable.removePlayer(args[2]);
            sender.sendMessage(configManager.getDatabaseRemovePlayerSuccessMsg().replaceAll("%uuid%", uuid)
                    .replaceAll("%player%", name));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("existplayer")) {
            if (!hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            if (args.length == 2)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            sender.sendMessage(String.valueOf(playerTable.existPlayer(args[2])));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("set")) {
            if (!hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            if (args.length < 5)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            if (!playerTable.existPlayer(args[3]))
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[3]));

            String uuid = playerTable.getPlayerData("uuid", args[3]);
            String name = playerTable.getPlayerData("player", args[3]);

            if (!database.getColumnMap(playerTable.getPlayerTableName()).containsKey(args[2]))
                return errorOccurred(sender, configManager.getDatabaseNoColumnFoundMsg());

            playerTable.setPlayerData(args[2], args[3], args[4]);

            sender.sendMessage(configManager.getDatabaseSetValueSuccessMsg().replaceAll("%uuid%", uuid)
                    .replaceAll("%player%", name).replaceAll("%column%", args[2]).replaceAll("%Value%", args[4]));

            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("get")) {
            if (!hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            if (args.length < 4)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            sender.sendMessage(playerTable.getPlayerData(args[2], args[3]));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("addcolumn")) {
            if (!hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            if (args.length < 5)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            if (args[4].equalsIgnoreCase("null"))
                database.addColumn(playerTable.getPlayerTableName(), args[2], args[3], null, true);
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("dropcolumn")) {
            if (!hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            if (args.length == 2)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            database.dropColumn(playerTable.getPlayerTableName(), args[2]);
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("getcolumnmap")) {
            if (!hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            sender.sendMessage("列リスト");
            database.getColumnMap(playerTable.getPlayerTableName()).forEach((columnName, columnType) -> {
                sender.sendMessage(columnName + " - " + columnType);
            });
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("getplayersmap")) {
            if (!hasPermission(sender, "playermanager.database." + subDatabaseCommand))
                return false;
            sender.sendMessage("記録されているプレイヤー");
            playerTable.getPlayersMap().forEach((uuidStr, name) -> {
                sender.sendMessage(uuidStr + " - " + name);
            });
            return true;
        }
        return errorOccurred(sender, configManager.getInvalidArgMsg());
    }

    /**
     * If entry is form of UUID, this returns string "uuid". Otherwise, this returns
     * string "player".
     * 
     * @param entry
     * @return "uuid" or "player"
     */
    public static String checkEntryType(String entry) {
        try {
            UUID.fromString(entry);
            return "uuid";
        } catch (IllegalArgumentException e) {
            return "player";
        }
    }

    public static boolean errorOccurred(CommandSender sender, String msg) {
        sender.sendMessage(msg);
        return false;
    }

    public static boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission))
            return errorOccurred(sender, PlayerManager.getInstance().getConfigManager().getNoPermMsg());
        return true;
    }
}

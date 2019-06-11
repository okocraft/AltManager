package net.okocraft.playermanager.command;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Comparator;
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
import net.okocraft.playermanager.utilities.ConfigManager;
import net.okocraft.playermanager.utilities.InventoryUtil;

public class Commands implements CommandExecutor {
    private PlayerManager instance;
    private ConfigManager configManager;
    private Database database;

    public Commands(Plugin plugin, Database database) {
        this.instance = (PlayerManager) plugin;
        this.configManager = instance.getConfigManager();
        this.database = database;

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
            return inventoryCommand(sender, command, label, args);
        case "database":
            return databaseCommand(sender, command, label, args);
        case "namechange":
            return nameChangeCommand(sender, command, label, args);
        default:
            return errorOccurred(sender, configManager.getCommandNotExistMsg().replaceAll("%command%", subCommand));
        }
    }

    private boolean inventoryCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1)
            return errorOccurred(sender, configManager.getNoEnoughArgMsg());
        String subInventoryCommand = args[1].toLowerCase();

        // /pman inventory searchbackup <player>
        if (subInventoryCommand.equals("searchbackup")) {
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
                    .searchFile(instance.getDataFolder().toPath().resolve("inventory").toFile(),
                            offlinePlayer.getUniqueId().toString() + ".log")
                    .stream()
                    .map(path -> LocalDateTime.parse(path.getParent().toFile().getName(), InventoryUtil.getFormat()))
                    .sorted(Comparator.reverseOrder()).skip((page - 1) * 9).limit(9)
                    .forEach(date -> sender.sendMessage(date.format(InventoryUtil.getFormat())));
            return true;
        }

        // /pman inventory showbackup <player> [year] [month] [day] [hour] [minute] [second]
        if (subInventoryCommand.equals("showbackup")) {

            if (!(sender instanceof Player))
                return errorOccurred(sender, configManager.getSenderMustBePlayerMsg());

            if (args.length <= 2)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);
            if (!offlinePlayer.hasPlayedBefore())
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));

            if (!sender.hasPermission("playermanager.inventory.showbackup.other") && !sender.getName().equalsIgnoreCase(args[2]))
                return errorOccurred(sender, configManager.getNoPermMsg());

            if (args.length == 3) {
                ((Player) sender).openInventory(InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer)));
            }

            Integer year = null;
            if (args.length >= 4) {
                year = Ints.tryParse(args[3]);
                if (year == null) {
                    ((Player) sender).openInventory(InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer)));
                    return true;
                }
                if (args.length == 4) {
                    ((Player) sender)
                            .openInventory(InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, year)));
                    return true;
                }
            }

            Integer month = null;
            if (args.length >= 5) {
                month = Ints.tryParse(args[4]);
                if (month == null) {
                    ((Player) sender)
                            .openInventory(InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, year)));
                    return true;
                }
                if (args.length == 5)
                    ((Player) sender).openInventory(
                            InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, year, month)));
            }

            Integer day = null;
            if (args.length >= 6) {
                day = Ints.tryParse(args[5]);
                if (day == null) {
                    ((Player) sender).openInventory(
                            InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, year, month)));
                    return true;
                }
                if (args.length == 6)
                    ((Player) sender).openInventory(
                            InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, year, month, day)));
            }

            Integer hour = null;
            if (args.length >= 7) {
                hour = Ints.tryParse(args[6]);
                if (hour == null) {
                    ((Player) sender).openInventory(
                            InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, year, month, day)));
                    return true;
                }
                if (args.length == 7)
                    ((Player) sender).openInventory(
                            InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, year, month, day, hour)));
            }

            Integer minute = null;
            if (args.length >= 8) {
                minute = Ints.tryParse(args[7]);
                if (minute == null) {
                    ((Player) sender).openInventory(
                            InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, year, month, day, hour)));
                    return true;
                }
                if (args.length == 8)
                    ((Player) sender).openInventory(InventoryUtil
                            .fromBase64(InventoryUtil.fromBackup(offlinePlayer, year, month, day, hour, minute)));
            }

            Integer second = null;
            if (args.length == 9) {
                second = Ints.tryParse(args[8]);
                if (second == null) {
                    ((Player) sender).openInventory(InventoryUtil
                            .fromBase64(InventoryUtil.fromBackup(offlinePlayer, year, month, day, hour, minute)));
                    return true;
                }
                ((Player) sender).openInventory(InventoryUtil
                        .fromBase64(InventoryUtil.fromBackup(offlinePlayer, year, month, day, hour, minute, second)));
                return true;
            }

            return true;
        }

        // rollback player's inventory from speficied backup.
        if (subInventoryCommand.equals("rollback")) {

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
            Optional<File> backup = InventoryUtil.getBackupFile((OfflinePlayer) player, year, month, day, hour, minute,
                    second);
            if (backup.isPresent()) {
                InventoryUtil.restoreInventory(player, backup.get());
            } else {
                return errorOccurred(sender, configManager.getInvalidBackupFileMsg());
            }
            return true;
        }

        // create backup file.
        if (subInventoryCommand.equals("backup")) {
            if (args.length <= 2) {
                if (!(sender instanceof Player))
                    return errorOccurred(sender, configManager.getInvalidArgMsg());
                InventoryUtil.backupInventory((Player) sender);
                sender.sendMessage(
                        configManager.getInventoryBackupSuccessMsg().replaceAll("%player%", sender.getName()));
            } else {
                Player player = Bukkit.getPlayer(args[2]);
                if (player == null)
                    return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
                InventoryUtil.backupInventory(player);
                sender.sendMessage(configManager.getInventoryBackupSuccessMsg().replaceAll("%player%", args[2]));
            }
            return true;
        }

        return errorOccurred(sender, configManager.getInvalidArgMsg());
    }

    private boolean nameChangeCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }

        final String subNameChangeCommand = args[1].toLowerCase();
        if (subNameChangeCommand.equals("previousname")) {
            if (args.length == 1) {
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());
            }
            if (!database.existPlayer(args[2])) {
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
            }
            String previousName = database.get("previous", args[2]);
            if (previousName.equals("null")) {
                sender.sendMessage("");
            } else {
                sender.sendMessage(previousName);
            }
        }

        return true;
    }

    private boolean databaseCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1)
            return errorOccurred(sender, configManager.getNoEnoughArgMsg());

        final String subDatabaseCommand = args[1].toLowerCase();
        if (subDatabaseCommand.equalsIgnoreCase("resetconnection")) {
            database.resetConnection();
            sender.sendMessage("§eデータベースへの接続をリセットしました。");
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("addplayer")) {
            if (args.length == 2)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            @SuppressWarnings("deprecation")
            OfflinePlayer player = Bukkit.getOfflinePlayer(args[2]);
            if (!player.hasPlayedBefore())
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));

            String uuid = player.getUniqueId().toString();
            String name = player.getName();
            database.addPlayer(uuid, name, true);
            sender.sendMessage(configManager.getDatabaseAddPlayerSuccessMsg().replaceAll("%uuid%", uuid)
                    .replaceAll("%player%", name));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("removeplayer")) {
            if (args.length == 2)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            if (!database.existPlayer(args[2]))
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));

            String uuid = database.get("uuid", args[2]);
            String name = database.get("player", args[2]);

            database.removePlayer(args[2]);
            sender.sendMessage(configManager.getDatabaseRemovePlayerSuccessMsg().replaceAll("%uuid%", uuid)
                    .replaceAll("%player%", name));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("existplayer")) {
            if (args.length == 2)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            sender.sendMessage(String.valueOf(database.existPlayer(args[2])));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("set")) {
            if (args.length < 5)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            if (!database.existPlayer(args[3]))
                return errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[3]));

            String uuid = database.get("uuid", args[3]);
            String name = database.get("player", args[3]);

            if (!database.getColumnMap().containsKey(args[2]))
                return errorOccurred(sender, configManager.getDatabaseNoColumnFoundMsg());

            database.set(args[2], args[3], args[4]);

            sender.sendMessage(configManager.getDatabaseSetValueSuccessMsg().replaceAll("%uuid%", uuid)
                    .replaceAll("%player%", name).replaceAll("%column%", args[2]).replaceAll("%Value%", args[4]));

            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("get")) {
            if (args.length < 4)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            sender.sendMessage(database.get(args[2], args[3]));
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("addcolumn")) {
            if (args.length < 5)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            if (args[4].equalsIgnoreCase("null"))
                database.addColumn(args[2], args[3], null, true);
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("dropcolumn")) {
            if (args.length == 2)
                return errorOccurred(sender, configManager.getNoEnoughArgMsg());

            database.dropColumn(args[2]);
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("getcolumnmap")) {
            sender.sendMessage("列リスト");
            database.getColumnMap().forEach((columnName, columnType) -> {
                sender.sendMessage(columnName + " - " + columnType);
            });
            return true;
        }
        if (subDatabaseCommand.equalsIgnoreCase("getplayersmap")) {
            sender.sendMessage("記録されているプレイヤー");
            database.getPlayersMap().forEach((uuidStr, name) -> {
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

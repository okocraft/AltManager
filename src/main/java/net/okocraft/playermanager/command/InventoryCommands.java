package net.okocraft.playermanager.command;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

import com.google.common.primitives.Ints;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.okocraft.playermanager.PlayerManager;
import net.okocraft.playermanager.utilities.ConfigManager;
import net.okocraft.playermanager.utilities.InventoryUtil;

class InventoryCommands {

    private static final PlayerManager instance = PlayerManager.getInstance();
    private static final ConfigManager configManager = instance.getConfigManager();

    static boolean inventoryCommands(CommandSender sender, String[] args,
                                     boolean isEnderChest) {
        if (args.length == 1) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }

        String subInventoryCommand = args[1].toLowerCase();
        String type = isEnderChest ? "enderchest" : "inventory";

        switch (subInventoryCommand) {

        // /pman inventory searchbackup <player>
        case "searchbackup":
            if (!Commands.hasPermission(sender, "playermanager." + type + "." + subInventoryCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }
            return searchBackupCommand(sender, isEnderChest, args);


        // /pman inventory showbackup <player> [year] [month] [day] [hour] [minute]
        // [second]
        case "showbackup":
            if (!Commands.hasPermission(sender, "playermanager." + type + "." + subInventoryCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }
            return showBackupCommand(sender, isEnderChest, args);

        // rollback player's inventory from speficied backup.
        case "rollback":
            if (!Commands.hasPermission(sender, "playermanager." + type + "." + subInventoryCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }
            return rollbackCommand(sender, isEnderChest, args);

        // create backup file.
        case "backup":
            if (!Commands.hasPermission(sender, "playermanager." + type + "." + subInventoryCommand)) {
                return Commands.errorOccurred(sender, configManager.getNoPermMsg());
            }
            return backupCommand(sender, isEnderChest, args);
        }

        return Commands.errorOccurred(sender, configManager.getInvalidArgMsg());
    }

    private static boolean searchBackupCommand(CommandSender sender, boolean isEnderChest, String[] args) {
        String type = isEnderChest ? "enderchest" : "inventory";

        if (args.length <= 2) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);

        if (!offlinePlayer.hasPlayedBefore()) {
            return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
        }

        Integer page = null;
        if (args.length >= 4) {
            page = Ints.tryParse(args[3]);
        }

        if (args.length == 3 || page == null) {
            page = 1;
        }

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

    private static boolean showBackupCommand(CommandSender sender, boolean isEnderChest, String[] args) {
        String type = isEnderChest ? "enderchest" : "inventory";

        if (!(sender instanceof Player)) {
            return Commands.errorOccurred(sender, configManager.getSenderMustBePlayerMsg());
        }

        if (args.length <= 2) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);
        if (!offlinePlayer.hasPlayedBefore())
            return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));

        if (!sender.hasPermission("playermanager." + type + ".showbackup.other")
                && !sender.getName().equalsIgnoreCase(args[2]))
            return Commands.errorOccurred(sender, configManager.getNoPermMsg());

        if (args.length == 3) {
            ((Player) sender)
                    .openInventory(InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest)));
        }

        Integer year = null;
        if (args.length >= 4) {
            year = Ints.tryParse(args[3]);
            if (year == null) {
                ((Player) sender)
                        .openInventory(InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest)));
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
                ((Player) sender).openInventory(
                        InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year, month)));
        }

        Integer day = null;
        if (args.length >= 6) {
            day = Ints.tryParse(args[5]);
            if (day == null) {
                ((Player) sender).openInventory(
                        InventoryUtil.fromBase64(InventoryUtil.fromBackup(offlinePlayer, isEnderChest, year, month)));
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

    private static boolean rollbackCommand(CommandSender sender, boolean isEnderChest, String[] args) {
        if (args.length <= 6) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }

        Player player = Bukkit.getPlayer(args[2]);
        if (player == null) {
            return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
        }

        Integer year = Ints.tryParse(args[3]);
        Integer month = Ints.tryParse(args[4]);
        Integer day = Ints.tryParse(args[5]);
        Integer hour = Ints.tryParse(args[6]);
        Integer minute = Ints.tryParse(args[7]);
        Integer second = Ints.tryParse(args[8]);

        if (year == null || month == null || day == null || hour == null || minute == null || second == null)
            return Commands.errorOccurred(sender, configManager.getInvalidArgMsg());
        Optional<File> backup = InventoryUtil.getBackupFile(player, isEnderChest, year, month, day,
                hour, minute, second);
        if (backup.isPresent()) {
            InventoryUtil.restoreInventory(player, isEnderChest, backup.get());
        } else {
            return Commands.errorOccurred(sender, configManager.getInvalidBackupFileMsg());
        }
        return true;
    }

    private static boolean backupCommand(CommandSender sender, boolean isEnderChest, String[] args) {

        if (args.length <= 2) {
            if (!(sender instanceof Player))
                return Commands.errorOccurred(sender, configManager.getInvalidArgMsg());
            InventoryUtil.backupInventory((Player) sender, isEnderChest);
            sender.sendMessage(configManager.getInventoryBackupSuccessMsg().replaceAll("%player%", sender.getName()));
        } else {
            Player player = Bukkit.getPlayer(args[2]);
            if (player == null)
                return Commands.errorOccurred(sender,
                        configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
            InventoryUtil.backupInventory(player, false);
            sender.sendMessage(configManager.getInventoryBackupSuccessMsg().replaceAll("%player%", args[2]));
        }
        return true;

    }
}
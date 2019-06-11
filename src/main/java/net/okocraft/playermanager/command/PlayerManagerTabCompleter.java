package net.okocraft.playermanager.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import net.okocraft.playermanager.PlayerManager;
import net.okocraft.playermanager.database.Database;
import net.okocraft.playermanager.utilities.InventoryUtil;

public class PlayerManagerTabCompleter implements TabCompleter {

    private PlayerManager instance;
    private Database database;

    public PlayerManagerTabCompleter(Database database) {
        this.instance = PlayerManager.getInstance();
        this.database = database;
        this.instance.getCommand("playermanager").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> resultList = new ArrayList<>();

        List<String> subCommands = new ArrayList<>();
        if (sender.hasPermission("playermanager.inventory"))
            subCommands.add("inventory");
        if (sender.hasPermission("playermanager.database"))
            subCommands.add("database");
        if (sender.hasPermission("playermanager.namechange"))
            subCommands.add("namechange");

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], subCommands, resultList);
        }

        final String subCommand = args[0].toLowerCase();
        if (!subCommands.contains(subCommand))
            return resultList;

        switch (subCommand) {
        case "namechange":
            return StringUtil.copyPartialMatches(args[1], new ArrayList<String>(database.getPlayersMap().values()), resultList);
        case "inventory":
            return onTabCompleteInventory(sender, resultList, args);
        case "database":
            return onTabCompleteDatabase(sender, resultList, args);
        default:
            return resultList;
        }
    }

    private List<String> onTabCompleteInventory(CommandSender sender, List<String> resultList, String[] args) {

        List<String> inventorySubCommands = new ArrayList<>();

        if (sender.hasPermission("playermanager.inventory.showbackup"))
            inventorySubCommands.add("showbackup");
        if (sender.hasPermission("playermanager.inventory.searchbackup"))
            inventorySubCommands.add("searchbackup");
        if (sender.hasPermission("playermanager.inventory.backup"))
            inventorySubCommands.add("backup");
        if (sender.hasPermission("playermanager.inventory.rollback"))
            inventorySubCommands.add("rollback");

        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], inventorySubCommands, resultList);
        }

        final String inventorySubCommand = args[1].toLowerCase();
        if (!inventorySubCommands.contains(inventorySubCommand))
            return resultList;

        List<String> playerList = new ArrayList<String>(database.getPlayersMap().values());

        if (args.length == 3) {
            switch (inventorySubCommand) {
            case "showbackup":
                return StringUtil.copyPartialMatches(args[2], playerList, resultList);
            case "searchbackup":
                return StringUtil.copyPartialMatches(args[2], playerList, resultList);
            case "backup":
                return null;
            case "rollback":
                return StringUtil.copyPartialMatches(args[2], playerList, resultList);
            default:
                return resultList;
            }
        }

        if (!playerList.contains(args[2]))
            return resultList;

        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);
        List<String> years = InventoryUtil.getBackupYears(offlinePlayer);

        if (args.length == 4) {
            switch (inventorySubCommand) {
            case "searchbackup":
                int maxpage = (InventoryUtil
                        .searchFile(instance.getDataFolder().toPath().resolve("inventory").toFile(), args[2]).size()
                        / 9) + 1;
                return StringUtil.copyPartialMatches(args[3],
                        IntStream.rangeClosed(1, maxpage).boxed().map(String::valueOf).collect(Collectors.toList()),
                        resultList);
            case "showbackup":
            case "rollback":
                return StringUtil.copyPartialMatches(args[3], years, resultList);
            default:
                return resultList;
            }
        }

        if (!years.contains(args[3]))
            return resultList;

        int year = Integer.parseInt(args[3]);
        List<String> months = InventoryUtil.getBackupMonth(offlinePlayer, year);

        if (args.length == 5) {
            switch (inventorySubCommand) {
            case "showbackup":
            case "rollback":
                return StringUtil.copyPartialMatches(args[4], months, resultList);
            default:
                return resultList;
            }
        }

        if (!months.contains(args[4]))
            return resultList;

        int month = Integer.parseInt(args[4]);
        List<String> days = InventoryUtil.getBackupDay(offlinePlayer, year, month);
        if (days.isEmpty()) System.out.println("days is empty");

        if (args.length == 6) {
            switch (inventorySubCommand) {
            case "showbackup":
            case "rollback":
                return StringUtil.copyPartialMatches(args[5], days, resultList);
            default:
                return resultList;
            }
        }

        if (!days.contains(args[5]))
            return resultList;
        
        int day = Integer.parseInt(args[5]);
        List<String> hours = InventoryUtil.getBackupHour(offlinePlayer, year, month, day);

        if (args.length == 7) {
            switch (inventorySubCommand) {
            case "showbackup":
            case "rollback":
                return StringUtil.copyPartialMatches(args[6], hours, resultList);
            default:
                return resultList;
            }
        }

        if (!hours.contains(args[6]))
            return resultList;

        int hour = Integer.parseInt(args[6]);
        List<String> minutes = InventoryUtil.getBackupMinute(offlinePlayer, year, month, day, hour);

        if (args.length == 8) {
            switch (inventorySubCommand) {
            case "showbackup":
            case "rollback":
                return StringUtil.copyPartialMatches(args[7], minutes, resultList);
            default:
                return resultList;
            }
        }

        if (!minutes.contains(args[7]))
            return resultList;

        int minute = Integer.parseInt(args[7]);
        List<String> seconds = InventoryUtil.getBackupSecond(offlinePlayer, year, month, day, hour, minute);

        if (args.length == 9) {
            switch (inventorySubCommand) {
            case "showbackup":
            case "rollback":
                return StringUtil.copyPartialMatches(args[8], seconds, resultList);
            default:
                return resultList;
            }
        }

        return resultList;
    }

    private List<String> onTabCompleteDatabase(CommandSender sender, List<String> resultList, String[] args) {

        List<String> playerList = database.getPlayersMap().values().parallelStream().collect(Collectors.toList());
        List<String> databaseSubCommands = Arrays.asList("addplayer", "removeplayer", "existplayer", "set", "get",
                "addcolumn", "dropcolumn", "getcolumnmap", "getplayersmap", "resetconnection");

        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], databaseSubCommands, resultList);
        }

        if (!databaseSubCommands.contains(args[1].toLowerCase()))
            return resultList;

        List<String> columnList = new ArrayList<>(database.getColumnMap().keySet());
        if (args.length == 3) {
            switch (args[1].toLowerCase()) {
            case "addplayer":
                return null;
            case "removeplayer":
                return StringUtil.copyPartialMatches(args[2], playerList, resultList);
            case "existplayer":
                return StringUtil.copyPartialMatches(args[2], playerList, resultList);
            case "set":
                return StringUtil.copyPartialMatches(args[2], columnList, resultList);
            case "get":
                return StringUtil.copyPartialMatches(args[2], columnList, resultList);
            case "addcolumn":
                return StringUtil.copyPartialMatches(args[2], Arrays.asList("<new_column_name>"), resultList);
            case "dropcolumn":
                return StringUtil.copyPartialMatches(args[2], columnList, resultList);
            }
        }

        if (Arrays.asList("removeplayer", "existplayer").contains(args[1].toLowerCase()))
            if (!playerList.contains(args[2]))
                return resultList;

        if (Arrays.asList("set", "get", "dropcolumn").contains(args[1].toLowerCase()))
            if (!columnList.contains(args[2]))
                return resultList;

        List<String> sqlTypeList = Arrays.asList("TEXT", "INTEGER", "NONE", "NUMERIC", "REAL");
        if (args.length == 4) {
            switch (args[1].toLowerCase()) {
            case "set":
                return StringUtil.copyPartialMatches(args[3], playerList, resultList);
            case "get":
                return StringUtil.copyPartialMatches(args[3], playerList, resultList);
            case "addcolumn":
                return StringUtil.copyPartialMatches(args[3], sqlTypeList, resultList);
            }
        }

        if (Arrays.asList("get", "set").contains(args[1].toLowerCase()))
            if (!playerList.contains(args[3]))
                return resultList;

        if (args[1].equalsIgnoreCase("addcolumn"))
            if (!sqlTypeList.contains(args[3].toUpperCase()))
                return resultList;

        if (args.length == 5) {
            switch (args[1].toLowerCase()) {
            case "set":
                return StringUtil.copyPartialMatches(args[4], Arrays.asList("<value>"), resultList);
            case "addcolumn":
                return StringUtil.copyPartialMatches(args[4], Arrays.asList("<default_value>", "null"), resultList);
            }
        }

        return resultList;
    }

}
package net.okocraft.playermanager.command;

import net.md_5.bungee.api.ChatColor;
import net.okocraft.playermanager.PlayerManager;
import net.okocraft.playermanager.database.Database;
import net.okocraft.playermanager.database.PlayerTable;
import net.okocraft.playermanager.utilities.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.*;

class AltCommands {

    private static final PlayerManager instance = PlayerManager.getInstance();
    private static final ConfigManager configManager = instance.getConfigManager();
    private static final Database database = instance.getDatabase();
    private static final PlayerTable playerTable = database.getPlayerTable();

    static boolean altCommands(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }

        final String subAltCommand = args[1].toLowerCase();

        switch (subAltCommand) {
            case "ip":
                if (!Commands.hasPermission(sender, "playermanager.alt.ip")) {
                    return false;
                }
                return ipCommand(sender, args);
            case "search":
                if (!Commands.hasPermission(sender, "playermanager.alt.search")) {
                    return false;
                }
                return searchCommand(sender, args);
            case "onlinealts":
                if (!Commands.hasPermission(sender, "playermanager.alt.onlinealts")) {
                    return false;
                }
                return onlineAltsCommand(sender);
            case "uniqueaccess":
            case "unique":
                if (!Commands.hasPermission(sender, "playermanager.alt.uniqueaccess")) {
                    return false;
                }
                return uniqueAccessCommand(sender);
            case "authorizedlist":
            case "authlist":
                if (!Commands.hasPermission(sender, "playermanager.alt.authorizedlist")) {
                    return false;
                }
                return authorizedListCommand(sender, args);
            case "authorize":
            case "auth":
                if (!Commands.hasPermission(sender, "playermanager.alt.authorize")) {
                    return false;
                }
                return authorizeCommand(sender, args);
            case "unauthorize":
            case "unauth":
                if (!Commands.hasPermission(sender, "playermanager.alt.unauthorize")) {
                    return false;
                }
                return unauthorizeCommand(sender, args);
        }

        return Commands.errorOccurred(sender, configManager.getInvalidArgMsg());
    }

    private static boolean ipCommand(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }
        if (!playerTable.existPlayer(args[2])) {
            return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
        }
        String address = playerTable.getPlayerData("address", args[2]);
        sender.sendMessage(address);
        return true;
    }

    private static boolean searchCommand(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }

        if (!playerTable.existPlayer(args[2])) {
            return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
        }

        if (args.length >= 4 && !args[3].equalsIgnoreCase("true") && !args[3].equalsIgnoreCase("false")) {
            return Commands.errorOccurred(sender, configManager.getInvalidArgMsg());
        }

        String address = playerTable.getPlayerData("address", args[2]);

        List<String> alts = database.get(playerTable.getPlayerTableName(), "uuid", "address", address);

        if (args.length == 3 || (args.length >= 4 && args[3].equalsIgnoreCase("false"))) {
            Set<String> authorizedAlts = playerTable.getAuthorizedAlts(args[2]);
            alts.removeIf(authorizedAlts::contains);
        }

        if (alts.size() == 1) {
            sender.sendMessage(
                    configManager.getShowAltsOnJoinMsg().replaceAll("%player%", args[2]).replaceAll("%alts%", ""));
            return true;
        }

        StringBuilder sb = new StringBuilder();
        alts.forEach(playerUuid -> {
            String playerName = Bukkit.getOfflinePlayer(UUID.fromString(playerUuid)).getName();
            if (!playerName.equals(args[2])) {
                sb.append(playerName).append(", ");
            }
        });
        sender.sendMessage(configManager.getShowAltsOnJoinMsg().replaceAll("%player%", args[2]).replaceAll("%alts%",
                sb.substring(0, sb.length() - 2)));
        return true;
    }

    private static boolean uniqueAccessCommand(CommandSender sender) {
        sender.sendMessage(String.valueOf(Bukkit.getOnlinePlayers().stream()
                .map(player -> player.getAddress().getAddress().getHostAddress()).distinct().count()));

        return true;
    }

    private static boolean onlineAltsCommand(CommandSender sender) {
        List<String> altIPs = new ArrayList<>();
        List<String> nonAltIPs = new ArrayList<>();
        Bukkit.getOnlinePlayers().stream().map(player -> player.getAddress().getAddress().getHostAddress()).forEach(ip -> {
            if (!nonAltIPs.contains(ip))
                nonAltIPs.add(ip);
            else
                altIPs.add(ip);
        });

        StringBuilder sb = new StringBuilder();
        altIPs.forEach(ip -> {
            Set<String> alts = new HashSet<>(database.get(playerTable.getPlayerTableName(), "uuid", "address", ip));
            Set<String> authorizedAlts = new HashSet<>();
            new HashSet<>(alts).forEach(playerUuid -> {
                if (!playerTable.getAuthorizedAlts(playerUuid).isEmpty()) {
                    alts.remove(playerUuid);
                    authorizedAlts.add(playerUuid);
                }
            });
            if (authorizedAlts.isEmpty()) {
                sb.append(ChatColor.GREEN).append(ip).append("\n");
                alts.forEach(playerUuid -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUuid));
                    if (offlinePlayer.isOnline()) {
                        sb.append(ChatColor.AQUA).append(offlinePlayer.getName()).append(ChatColor.GRAY).append(", ");
                    } else {
                        sb.append(ChatColor.GRAY).append(offlinePlayer.getName()).append(ChatColor.GRAY).append(", ");
                    }
                });
                sb.delete(sb.length() - 2, sb.length());
                sb.append("\n");

            } else {
                authorizedAlts.forEach(playerUuid -> {
                    sb.append(ChatColor.GREEN).append(ip).append("\n");
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUuid));
                    if (offlinePlayer.isOnline()) {
                        sb.append(ChatColor.AQUA).append(offlinePlayer.getName()).append(ChatColor.GRAY).append(", ");
                    } else {
                        sb.append(ChatColor.GRAY).append(offlinePlayer.getName()).append(ChatColor.GRAY).append(", ");
                    }
                    alts.forEach(altPlayerUuid -> {
                        OfflinePlayer offlinePlayerAlt = Bukkit.getOfflinePlayer(UUID.fromString(altPlayerUuid));
                        if (offlinePlayerAlt.isOnline()) {
                            sb.append(ChatColor.AQUA).append(offlinePlayerAlt.getName()).append(ChatColor.GRAY).append(", ");
                        } else {
                            sb.append(ChatColor.GRAY).append(offlinePlayerAlt.getName()).append(ChatColor.GRAY).append(", ");
                        }
                    });
                    sb.delete(sb.length() - 2, sb.length());
                    sb.append("\n");
                });
            }
        });

        sender.sendMessage(sb.toString());

        return true;
    }

    private static boolean authorizedListCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }
        String player = args[2];
        if (!playerTable.existPlayer(player)) {
            return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg());
        }
        Set<String> authorizedAlts = playerTable.getAuthorizedAlts(player);
        if (authorizedAlts.isEmpty()) {
            sender.sendMessage("");
            return true;
        }
        StringBuilder sb = new StringBuilder();
        authorizedAlts.forEach(authorizedAltUuid -> {
            String authorizedAlt = Bukkit.getOfflinePlayer(UUID.fromString(authorizedAltUuid)).getName();
            sb.append(ChatColor.AQUA).append(authorizedAlt).append(ChatColor.GRAY).append(", ");
        });
        sender.sendMessage(sb.substring(0, sb.length() - 4));
        return true;
    }

    private static boolean authorizeCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }
        if (!playerTable.existPlayer(args[2])) {
            return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[2]));
        }
        if (!playerTable.existPlayer(args[3])) {
            return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg().replaceAll("%player%", args[3]));
        }
        if (args[2].equalsIgnoreCase(args[3])) {
            return Commands.errorOccurred(sender, configManager.getInvalidArgMsg());
        }

        String player1 = playerTable.getPlayerData("uuid", args[2]);
        String player2 = playerTable.getPlayerData("uuid", args[3]);

        if (!playerTable.getPlayerData("address", player1).equals(playerTable.getPlayerData("address", player2))) {
            return Commands.errorOccurred(sender, configManager.getPlayerDifferentIpMsg()
                    .replaceAll("%player1%", args[2]).replaceAll("%player2%", args[3]));
        }

        Set<String> authorizedAlts = playerTable.getAuthorizedAlts(player1);
        authorizedAlts.addAll(playerTable.getAuthorizedAlts(player2));

        authorizedAlts.add(player2);
        authorizedAlts.remove(player1);
        playerTable.setAuthorizedAlts(player1, authorizedAlts);

        authorizedAlts.add(player1);
        authorizedAlts.remove(player2);
        playerTable.setAuthorizedAlts(player2, authorizedAlts);

        sender.sendMessage(
                configManager.getAltAuthorizeMsg().replaceAll("%player1%", args[2]).replaceAll("%player2%", args[3]));
        return true;
    }

    private static boolean unauthorizeCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            return Commands.errorOccurred(sender, configManager.getNoEnoughArgMsg());
        }
        if (!playerTable.existPlayer(args[2])) {
            return Commands.errorOccurred(sender, configManager.getNoPlayerFoundMsg());
        }

        String playerUuid = playerTable.getPlayerData("uuid", args[2]);

        Set<String> authorizedAlts = playerTable.getAuthorizedAlts(playerUuid);
        if (authorizedAlts.isEmpty()) {
            return Commands.errorOccurred(sender, ":NO_AUTHOLIZEDPLAYER_FOUND");
        }

        authorizedAlts.forEach(authorizedAltsUuid -> {
            Set<String> authorizedAltsForOthers = playerTable.getAuthorizedAlts(authorizedAltsUuid);
            authorizedAltsForOthers.remove(playerUuid);
            playerTable.setAuthorizedAlts(authorizedAltsUuid, authorizedAltsForOthers);
        });

        playerTable.setAuthorizedAlts(playerUuid, new HashSet<>());
        sender.sendMessage(configManager.getAltUnauthorizeMsg().replaceAll("%player%", args[2]));

        return true;

    }
}
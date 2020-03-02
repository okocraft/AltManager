package net.okocraft.altmanager.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.okocraft.altmanager.Util;

public final class OnlineAltsCommand extends BaseCommand {

    protected OnlineAltsCommand() {
        super(
                "altemanager.onlinealts",
                2,
                false,
                "/altmanager onlinealts",
                "online"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        
        StringBuilder sb = new StringBuilder();
        
        Set<String> altIPs = new HashSet<>();
        Set<String> nonAltIPs = new HashSet<>();
        // 各プレイヤーごとに処理。
        for (Player online : Bukkit.getOnlinePlayers()) {
            String ip = online.getAddress().getAddress().getHostAddress();
            
            // nonAltIPs か altIPs に含まれている時は飛ばす。
            if (nonAltIPs.add(ip) || altIPs.add(ip)) {
                continue;
            }

            Map<String, String> alts = database.getAlts(ip);
            Map<String, String> authorizedAlts = new HashMap<>();
            alts.forEach((uuid, player) -> {
                if (!database.getAuthorizedAlts(uuid).isEmpty()) {
                    authorizedAlts.put(uuid, player);
                }
            });

            if (authorizedAlts.isEmpty()) {
                sb.append(ChatColor.GREEN + ip + "\n");
                alts.forEach((uuid, player) -> {
                    OfflinePlayer offlinePlayer = Util.getOfflinePlayer(uuid);
                    if (offlinePlayer != null) {
                        sb.append((offlinePlayer.isOnline() ? ChatColor.AQUA : ChatColor.GRAY)
                                + player + ChatColor.GRAY + ", ");
                    }
                });
                sb.delete(sb.length() - 2, sb.length());
                sb.append("\n");

            } else {
                authorizedAlts.forEach((uuid, player) -> {
                    sb.append(ChatColor.GREEN + ip + "\n");
                    OfflinePlayer offlinePlayer = Util.getOfflinePlayer(uuid);
                    if (offlinePlayer == null) {
                        return;
                    }
                    sb.append((offlinePlayer.isOnline() ? ChatColor.AQUA : ChatColor.GRAY) + player + ChatColor.GRAY + ", ");
                    
                    alts.forEach((altUuid, altPlayer) -> {
                        OfflinePlayer offlinePlayerAlt = Util.getOfflinePlayer(altUuid);
                        if (offlinePlayerAlt != null) {
                            sb.append((offlinePlayerAlt.isOnline() ? ChatColor.AQUA : ChatColor.GRAY) + altPlayer + ChatColor.GRAY + ", ");
                        }
                    });
                    sb.delete(sb.length() - 2, sb.length());
                    sb.append("\n");
                });
            }

        }

        sender.sendMessage(sb.toString());
        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
} 
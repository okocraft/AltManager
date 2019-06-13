package net.okocraft.playermanager.listener;

import java.net.InetSocketAddress;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.plugin.Plugin;

import net.md_5.bungee.api.ChatColor;
import net.okocraft.playermanager.PlayerManager;
import net.okocraft.playermanager.utilities.ConfigManager;

public class PreLogin implements Listener {

    private ConfigManager configManager;

    public PreLogin(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        configManager = PlayerManager.getInstance().getConfigManager();
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        int maxAccount = configManager.getMaxAccounts();
        boolean isMoreThanMax = Bukkit.getOnlinePlayers().stream().map(Player::getAddress)
                .map(InetSocketAddress::getHostName).filter(address -> event.getAddress().getHostName().equals(address))
                .count() > maxAccount;
        if (isMoreThanMax) {
            event.setLoginResult(Result.KICK_OTHER);
            event.setKickMessage(ChatColor.RED + "Too many alts.");
        }
    }
}
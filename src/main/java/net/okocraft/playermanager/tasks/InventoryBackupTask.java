package net.okocraft.playermanager.tasks;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import net.okocraft.playermanager.PlayerManager;
import net.okocraft.playermanager.utilities.InventoryUtil;

public class InventoryBackupTask extends BukkitRunnable {

    @Override
    public void run() {
        new BukkitRunnable() {

            @Override
            public void run() {

                Bukkit.getOnlinePlayers().forEach(player -> {
                    InventoryUtil.backupInventory(player, false);
                    InventoryUtil.backupInventory(player, true);
                });
            }
        }.runTask(PlayerManager.getInstance());
    }
}
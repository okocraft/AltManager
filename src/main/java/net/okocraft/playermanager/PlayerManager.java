package net.okocraft.playermanager;

import lombok.Getter;
import net.okocraft.playermanager.command.Commands;
import net.okocraft.playermanager.database.Database;
import net.okocraft.playermanager.listener.InventoryClick;
import net.okocraft.playermanager.listener.PlayerJoin;
import net.okocraft.playermanager.listener.PreLogin;
import net.okocraft.playermanager.tasks.InventoryBackupTask;
import net.okocraft.playermanager.utilities.ConfigManager;
import net.okocraft.playermanager.utilities.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * @author OKOCRAFT
 */
public class PlayerManager extends JavaPlugin {

    private static PlayerManager instance;
    @Getter
    private final Logger log;
    @Getter
    private final Database database;
    @Getter
    private ConfigManager configManager;

    public PlayerManager() {
        log = getLogger();
        database = new Database(this);
    }

    @Override
    public void onEnable() {

        configManager = new ConfigManager();

        // Connect to database
        if (!database.connect(getDataFolder().getPath() + "/data.db")) {
            log.severe("Cannot connect to database. Disabling pluing...");
            setEnabled(false);
        }

        try {
            Path temp = getDataFolder().toPath().resolve("inventory");
            if (!Files.exists(temp) || !Files.isDirectory(temp)) {
                Files.createDirectory(temp);
            }
            temp = temp.getParent().resolve("enderchest");
            if (!Files.exists(temp) || !Files.isDirectory(temp)) {
                Files.createDirectory(temp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Commands(this, database);
        int invBackupInterval = configManager.getInvBackupInterval() * 1200;
        new InventoryBackupTask().runTaskTimerAsynchronously(this, invBackupInterval, invBackupInterval);
        new PlayerJoin(this, database);
        new InventoryClick(this);
        new PreLogin(this);
        log.info("PlayerManager has been enabled!");
    }

    @Override
    public void onDisable() {
        InventoryUtil.packDayBackups(true);
        InventoryUtil.packMonthBackups(true);
        InventoryUtil.packYearBackups(true);

        InventoryUtil.packDayBackups(false);
        InventoryUtil.packMonthBackups(false);
        InventoryUtil.packYearBackups(false);

        database.dispose();

        Bukkit.getOnlinePlayers().forEach(Player::closeInventory);
        HandlerList.unregisterAll(this);

        log.info("PlayerManager has been disabled!");
    }

    public static PlayerManager getInstance() {
        if (instance == null) {
            instance = (PlayerManager) Bukkit.getPluginManager().getPlugin("PlayerManager");
        }

        return instance;
    }
}

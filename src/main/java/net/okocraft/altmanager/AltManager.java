package net.okocraft.altmanager;

import net.okocraft.altmanager.command.AltCommand;
import net.okocraft.altmanager.config.Config;
import net.okocraft.altmanager.database.Database;
import net.okocraft.altmanager.listener.PlayerListener;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author OKOCRAFT
 */
public class AltManager extends JavaPlugin {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd_HH:mm:ss");
    private static AltManager instance;
    private Database database;

    @Override
    public void onEnable() {
        Config.getInstance().reloadAllConfigs();

        database = Database.getInstance();
        if (database == null) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        PlayerListener.start();
        AltCommand.init();
        getLogger().info("AltManager has been enabled!");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.dispose();
        }
        HandlerList.unregisterAll(this);

        getLogger().info("AltManager has been disabled!");
    }

    public static AltManager getInstance() {
        if (instance == null) {
            instance = (AltManager) Bukkit.getPluginManager().getPlugin("AltManager");
        }

        return instance;
    }

    public static DateTimeFormatter getTimeFormat() {
        return FORMAT;
    }

    public Database getDatabase() {
        return Objects.requireNonNull(database, "plugin is not loaded yet.");
    }
}

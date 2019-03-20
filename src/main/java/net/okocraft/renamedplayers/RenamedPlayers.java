package net.okocraft.renamedplayers;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.renamedplayers.command.RenamedPlayersCommand;
import net.okocraft.renamedplayers.database.Database;
import net.okocraft.renamedplayers.eventlistener.PlayerJoin;

/**
 * @author OKOCRAFT
 */
public class RenamedPlayers extends JavaPlugin {

    private static final Logger log = LoggerFactory.getLogger("RenamedPlayers");

    /**
     * プラグイン RenamedPlayers のインスタンス。
     */
    private static RenamedPlayers instance;

    /**
     * RenamedPlayers のバージョン。
     */
    @Getter
    private final String version;

    /**
     * データベース。
     */
    private final Database database;

    public RenamedPlayers() {
        version = getClass().getPackage().getImplementationVersion();
        database = new Database();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("languages/ja.yml", false);
        saveResource("languages/en.yml", false);

        new PlayerJoin(this, database);

        // Connect to database
        database.connect(getDataFolder().getPath() + "/data.db");

        // Implementation info
        log.info("Installed in : " + getDataFolder().getPath());
        log.info("Database file: " + database.getDBUrl());

        // Register command /renamedplayers
        getCommand("renamedplayers").setExecutor(new RenamedPlayersCommand(database));

        log.info("RenamedPlayers has been enabled!");
    }

    @Override
    public void onDisable() {
        database.dispose();

        HandlerList.unregisterAll(this);

        log.info("RenamedPlayers has been disabled!");
    }

    public static RenamedPlayers getInstance() {
        if (instance == null) {
            instance = (RenamedPlayers) Bukkit.getPluginManager().getPlugin("RenamedPlayers");
        }

        return instance;
    }
}

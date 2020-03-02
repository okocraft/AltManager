package net.okocraft.altmanager.config;

import java.util.List;

public final class Config extends CustomConfig {

    private static final Config INSTANCE = new Config();

    private Config() {
        super("config.yml");
    }

    public static Config getInstance() {
        return INSTANCE;
    }

    public boolean isLoggingRename() {
        return get().getBoolean("log-player-rename");
    }

    public boolean notifyRename() {
        return get().getBoolean("notify-rename");
    }
    
    public boolean notifyPreviousName() {
        return get().getBoolean("notify-previous-name-on-join");
    }

    public int getNotificationPreviousNameDays() {
        return get().getInt("notify-previous-name-days", 14);
    }

    public List<String> getCommandsOnRename() {
        return get().getStringList("commands-on-rename");
    }

    public int getMaxAccounts() {
        return get().getInt("max-accounts", 3);
    }

    public void reloadAllConfigs() {
        reload();
        Messages.getInstance().reload();
    }
}
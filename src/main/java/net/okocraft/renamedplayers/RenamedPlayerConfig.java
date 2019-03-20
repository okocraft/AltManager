package net.okocraft.renamedplayers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import lombok.Getter;

public class RenamedPlayerConfig {
    /**language file. ex. 'ja.yml', 'en.yml'... */
    private String languageFile;

    /**Whether Name change logging is enabled. */
    private Boolean loggingEnabled;

    /**
     * Whether Namechangelogging is enabled.
     * @return true when name change logging is enabled
     */
    public Boolean isLoggingEnabled(){
        return this.loggingEnabled;
    }

    /**Whether Name change notification is enabled. */
    private Boolean notificationEnabled;

    /**
     * Whether Namechangelogging is enabled.
     * @return true when name change logging is enabled
     */
    public Boolean isNotificationEnabled(){
        return this.notificationEnabled;
    }

    /**Whether command execution is enabled. */
    private Boolean commandExecutionEnabled;

    /**
     * Whether Namechangelogging is enabled.
     * @return true when name change logging is enabled
     */
    public Boolean isCommandExecutionEnabled(){
        return this.commandExecutionEnabled;
    }

    /**Command List which will be executed on Change of Name. */
    @Getter private List<String> commandList;

    /**Whether score migration is enabled. */
    private Boolean scoreMigrationEnabled;

    /**
     * Whether Namechangelogging is enabled.
     * @return true when name change logging is enabled
     */
    public Boolean isScoreMigrationEnabled(){
        return this.scoreMigrationEnabled;
    }

    /**On score migration, whether score of old name should be reset. */
    private Boolean oldScoreResetEnabled;

    /**
     * On migration, whether old entry's score should be reset.
     * @return true when old score should be reset
     */
    public Boolean isOldScoreResetEnabled(){
        return this.oldScoreResetEnabled;
    }

    /**Message on permission denied. */
    @Getter private String messagePermissionDenied;

    /**Message on invalid argument. */
    @Getter private String messageInvalidArgument;

    /**Message on name change notification. */
    @Getter private String messageNameChangeNotification;

    public RenamedPlayerConfig(){
        reloadConfig();
        reloadLanguageFile(languageFile);
    }

    private void reloadConfig(){

        FileConfiguration config = RenamedPlayers.getInstance().getConfig();

        languageFile = Optional.ofNullable(config.getString("LanguageFile")).orElse("en.yml");

        loggingEnabled = Optional.ofNullable(config.getBoolean("LogPlayerNameChange")).orElse(true);

        notificationEnabled = Optional.ofNullable(config.getBoolean("NotifyNameChange")).orElse(true);

        commandExecutionEnabled = Optional.ofNullable(config.getBoolean("ExecuteCommandOnNameChange")).orElse(false);

        commandList = Optional.ofNullable(config.getStringList("Commands")).orElse(new ArrayList<String>());

        scoreMigrationEnabled = Optional.ofNullable(config.getBoolean("ScoreboardMigration")).orElse(false);

        oldScoreResetEnabled = Optional.ofNullable(config.getBoolean("ResetOldScore")).orElse(false);

    }

    private void reloadLanguageFile(String languageFile){

        FileConfiguration selectedLanguage = YamlConfiguration.loadConfiguration(new File(RenamedPlayers.getInstance().getDataFolder().getPath() + "/languages/" + languageFile));

        messagePermissionDenied = Optional.ofNullable(selectedLanguage.getString("PermissionDenied").replaceAll("&", "§")).orElse(":PERMISSION_DENIED");

        messageInvalidArgument = Optional.ofNullable(selectedLanguage.getString("InvalidArgument").replaceAll("&", "§")).orElse(":INVALID_ARGUMENT");

        messageNameChangeNotification = Optional.ofNullable(selectedLanguage.getString("NameChangeNotification").replaceAll("&", "§")).orElse(":PLAYER_%ODLNAME%_BECOMES_%NEWNAME%");

    }

}
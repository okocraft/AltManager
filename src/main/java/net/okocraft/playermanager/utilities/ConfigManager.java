package net.okocraft.playermanager.utilities;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import lombok.Getter;
import net.okocraft.playermanager.PlayerManager;

public class ConfigManager {

    /** PLugin instance */
    private static PlayerManager instance;

    @Getter private FileConfiguration defaultConfig;

    /** language file. ex. 'ja.yml', 'en.yml'... */
    private String languageFile;

    /** Language Custom Config Instance */
    private CustomConfig languageCustomConfig;

    /** Language FileConfiguration */
    @Getter private FileConfiguration languageConfig;

    /** Whether Name change logging is enabled. */
    @Getter private boolean loggingEnabled;

    /** Whether Name change notification is enabled. */
    @Getter private boolean notificationEnabled;

    /** Whether command execution is enabled. */
    @Getter private boolean commandExecutionEnabled;

    /** Command List which will be executed on Change of Name. */
    @Getter private List<String> commandList;

    /** Whether score migration is enabled. */
    @Getter private boolean scoreMigrationEnabled;

    /** On score migration, whether score of old name should be reset. */
    @Getter private boolean oldScoreResetEnabled;

    /** Should plugin notify previous name for the player on join. */
    @Getter private boolean notifyPreviousNameEnabled;

    /** How long the plugin broadcast his previous name on join. In day.*/
    @Getter private int notifyPreviousNameTerm;

    /** Interval of inventory backup. */
    @Getter private int invBackupInterval;

    /** Message on permission denied. */
    @Getter private String noPermMsg;

    /** Message on invalid argument. */
    @Getter private String invalidArgMsg;

    /** Message on specifying no enough arguments. */
    @Getter private String noEnoughArgMsg;

    /** Message on name change notification. */
    @Getter private String nameChangeMsg;
    
    /** Message when no player is found. */
    @Getter private String noPlayerFoundMsg;

    /** Message when sender should. */
    @Getter private String senderMustBePlayerMsg;

    /** Page footer format. */
    @Getter private String pageFooter;

    /** Message on invalid command. */
    @Getter private String commandNotExistMsg;

    /** Message on executed inventory backup successfully. */
    @Getter private String inventoryBackupSuccessMsg;
    
    /** Message on addPlayer success. */
    @Getter private String databaseAddPlayerSuccessMsg;

    /** Message on removePlayer success. */
    @Getter private String databaseRemovePlayerSuccessMsg;

    /** Message on no column is found. */
    @Getter private String databaseNoColumnFoundMsg;

    /** Message on serValue success. */
    @Getter private String databaseSetValueSuccessMsg;

    /** Message on speficying invalid backup file */
    @Getter private String invalidBackupFileMsg;

    /** Message on join player who renamed before. */
    @Getter private String notifyPreviousNameMsg;

    public ConfigManager() {

        instance = PlayerManager.getInstance();
        saveDefaultConfigs();
        defaultConfig = instance.getConfig();
        languageFile = defaultConfig.getString("LanguageFile", "en.yml");
        languageCustomConfig = new CustomConfig(instance, languageFile);
        languageConfig = languageCustomConfig.getConfig();
        loadField();
    }

    private void saveDefaultConfigs() {
        instance.saveDefaultConfig();
        CustomConfig temp = new CustomConfig(instance, "languages/ja.yml");
        temp.saveDefaultConfig();
        temp = new CustomConfig(instance, "languages/en.yml");
        temp.saveDefaultConfig();
        temp = null;
    }

    public void reloadConfig(){
        instance.reloadConfig();
        defaultConfig = instance.getConfig();
        languageFile = defaultConfig.getString("LanguageFile", "en.yml");
        languageCustomConfig.reloadConfig();
        languageConfig = languageCustomConfig.getConfig();
        loadField();
    }

    private void loadField() {

        // On name change field
        loggingEnabled = defaultConfig.getBoolean("LogPlayerNameChange", true);
        notificationEnabled = defaultConfig.getBoolean("NotifyRename", true);
        commandExecutionEnabled = defaultConfig.getBoolean("ExecuteCommandOnNameChange", false);
        commandList = defaultConfig.getStringList("Commands");
        scoreMigrationEnabled = defaultConfig.getBoolean("ScoreboardMigration", false);
        oldScoreResetEnabled = defaultConfig.getBoolean("ResetOldScore", false);
        notifyPreviousNameEnabled = defaultConfig.getBoolean("NotifyPreviousNameOnJoin", false);
        notifyPreviousNameTerm = defaultConfig.getInt("NotifyPreviousNameTerm", 14);
        invBackupInterval = defaultConfig.getInt("InventoryBackupInterval", 30);

        // Language field
        noPermMsg = getString("PermissionDenied");
        invalidArgMsg = getString("InvalidArgument");
        noEnoughArgMsg = getString("NoEnoughArguments");
        nameChangeMsg = getString("NameChangeNotification");
        noPlayerFoundMsg = getString("NoPlayerFound");
        senderMustBePlayerMsg = getString("SenderMustBePlayer");
        pageFooter = getString("PageFooter");
        commandNotExistMsg = getString("CommandNotExist");
        inventoryBackupSuccessMsg = getString("InventoryBackupSuccess");
        databaseAddPlayerSuccessMsg = getString("DatabaseAddPlayerSuccess");
        databaseRemovePlayerSuccessMsg = getString("DatabaseRemovePlayerSuccess");
        databaseNoColumnFoundMsg = getString("DatabaseNoColumnFound");
        databaseSetValueSuccessMsg = getString("DatabaseSetValueSuccess");
        invalidBackupFileMsg = getString("InvalidBackupFile");
        notifyPreviousNameMsg = getString("BroadcastedPreviousName");
    }

    private String getString(String path) {
        String result = languageConfig.getString(path);
        if (result == null) {
            if (languageFile == null) languageFile = "en.yml";
            Reader reader = new InputStreamReader(instance.getResource("languages/" + languageFile));
            YamlConfiguration jarConfig = YamlConfiguration.loadConfiguration(reader);
            result = jarConfig.getString(path, "&cError has occured on loading locale.");
        }
        return result.replaceAll("&([a-f0-9])", "§$1");
    }
}
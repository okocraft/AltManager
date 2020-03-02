package net.okocraft.altmanager.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandSender;

import net.okocraft.altmanager.AltManager;
import net.okocraft.altmanager.config.Config;
import net.okocraft.altmanager.config.Messages;
import net.okocraft.altmanager.database.Database;

public abstract class BaseCommand {

    protected final AltManager plugin = AltManager.getInstance();
    protected final Config config = Config.getInstance();
    protected final Messages messages = Messages.getInstance();

    protected final Database database = plugin.getDatabase();

    private final String permissionNode;
    private final int leastArgLength;
    private final boolean isPlayerOnly;
    private final String usage;
    private final List<String> alias;

    protected BaseCommand(String permissionNode, int leastArgLength, boolean isPlayerOnly, String usage, String ... alias) {
        this.permissionNode = permissionNode;
        this.leastArgLength = leastArgLength;
        this.isPlayerOnly = isPlayerOnly;
        this.usage = usage;
        this.alias = Arrays.asList(alias);
    }

    /**
     * 各コマンドの処理
     *
     * @param sender コマンドの実行者
     * @param args   引数
     * @return コマンドが成功したらtrue
     */
    public abstract boolean runCommand(CommandSender sender, String[] args);

    /**
     * 各コマンドのタブ補完の処理
     *
     * @param sender コマンドの実行者
     * @param args   引数
     * @return その時のタブ補完のリスト
     */
    public abstract List<String> runTabComplete(CommandSender sender, String[] args);

    /**
     * コマンドの名前を取得する。
     *
     * @return コマンドの名前
     */
    public String getName() {
        String className = this.getClass().getSimpleName().toLowerCase(Locale.ROOT).replaceAll("_", "");
        return className.substring(0, className.length() - "command".length());
    }

    /**
     * このコマンドの権限を取得する。
     *
     * @return 権限
     */
    public String getPermissionNode() {
        return permissionNode;
    }

    /**
     * プレイヤーのみが使用可能なコマンドかどうかを取得する
     * 
     * @return
     */
    public boolean isPlayerOnly() {
        return isPlayerOnly;
    }

    /**
     * 最低限必要な引数の長さを取得する。
     *
     * @return 最低限の引数の長さ
     */
    public int getLeastArgLength() {
        return leastArgLength;
    }

    public boolean isValidArgsLength(int argsLength) {
        return getLeastArgLength() <= argsLength;
    }

    /**
     * コマンドの引数の内容を取得する。例: "/box autostoreList [page]"
     *
     * @return 引数の内容
     */
    public String getUsage() {
        return usage;
    }

    public List<String> getAlias() {
        return alias;
    }

    /**
     * コマンドの説明を取得する。例: "アイテムの自動収納の設定をリストにして表示する。"
     *
     * @return コマンドの説明
     */
    public String getDescription() {
        return messages.getMessage("command.help.command-description." + getName());
    }

    /**
     * このコマンドを使う権限があるか調べる。
     * 
     * @param sender
     * @return 権限があればtrue なければfalse
     * @see CommandSender#hasPermission(String)
     */
    public boolean hasPermission(CommandSender sender) {
        if (permissionNode == null || permissionNode.isEmpty()) {
            return true;
        }

        return sender.hasPermission(getPermissionNode());
    }
}
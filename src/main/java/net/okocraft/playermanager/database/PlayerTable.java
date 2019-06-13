package net.okocraft.playermanager.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import net.okocraft.playermanager.PlayerManager;
import net.okocraft.playermanager.command.Commands;
import net.okocraft.playermanager.utilities.InventoryUtil;

public class PlayerTable {

    private final Logger log;
    private final Database database;
    @Getter
    private String playerTableName;

    PlayerTable(Database database) {
        log = PlayerManager.getInstance().getLog();
        this.database = database;
        this.playerTableName = "PlayerManager_Rename";

        boolean isTableCreated = database.createTable(playerTableName, "uuid TEXT PRIMARY KEY NOT NULL",
                "player TEXT NOT NULL");
        if (!isTableCreated) {
            log.severe("Failed to create the table.");
        }

        database.addColumn(playerTableName, "previous", "TEXT", null, false);
        database.addColumn(playerTableName, "renamelogondate", "TEXT", LocalDateTime.MIN.format(InventoryUtil.getFormat()), false);
        database.addColumn(playerTableName, "address", "TEXT", null, false);
    }

    /**
     * 名前変更記録用テーブルにプレイヤーを追加する。 showWarningがtrueで失敗した場合はコンソールにログを出力する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @param uuid        UUID
     * @param name        名前
     * @param showWarning コンソールログを出力するかどうか
     *
     * @return 成功すればtrue 失敗すればfalse
     */
    public boolean addPlayer(@NonNull String uuid, @NonNull String name, boolean showWarning) {

        if (existPlayer(uuid)) {
            if (showWarning)
                log.warning(":PLAYER_" + name + "_UUID_" + uuid + "ALREADY_EXIST");
            return false;
        }

        if (Commands.checkEntryType(uuid).equals("player")) {
            if (showWarning)
                log.warning(":INVALID_UUID");
            return false;
        }

        if (!name.matches("(\\d|[a-zA-Z]|_){3,16}")) {
            if (showWarning)
                log.warning(":INVALID_NAME");
            return false;
        }

        boolean isInserted = database.insert(playerTableName, new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put("uuid", uuid);
                put("player", name);
            }
        });

        if (!isInserted && showWarning) {
            log.warning(":INSERT_FAILURE");
        }
        return isInserted;
    }

    /**
     * 名前変更記録用テーブルからプレイヤーを削除する。 失敗した場合はコンソールにログを出力する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author LazyGon
     *
     * @param entry プレイヤー
     *
     * @return 成功すればtrue 失敗すればfalse
     */
    public boolean removePlayer(@NonNull String entry) {

        if (!existPlayer(entry)) {
            log.warning(":NO_RECORD_FOR_" + entry + "_EXIST");
            return false;
        }
        String entryType = Commands.checkEntryType(entry);
        return database.remove(playerTableName, entryType, entry);
    }

    /**
     * 名前変更記録用テーブルに名前が記録されているか調べる。
     *
     * @since 1.0.0-SNAPSHOT
     * @author LazyGon
     *
     * @param entry uuidでもmcidでも可
     */
    public boolean existPlayer(@NonNull String entry) {
        Map<String, String> playersMap = getPlayersMap();
        return playersMap.containsKey(entry) || playersMap.containsValue(entry);
    }

    /**
     * {@code table}の{@code column}に値をセットする。
     *
     * @since 1.0.0-SNAPSHOT
     * @author LazyGon
     *
     * @param column 更新する列
     * @param entry  プレイヤー。uuidでもmcidでも可
     * @param value  新しい値
     */
    public boolean setPlayerData(String column, String entry, String value) {

        if (!existPlayer(entry)) {
            log.warning(":NO_RECORD_FOR_" + entry + "_EXIST");
            return false;
        }

        String entryType = Commands.checkEntryType(entry);
        return database.set(playerTableName, column, value, entryType, entry);
    }

    /**
     * {@code table} で指定したテーブルの列 {@code column} の値を取得する。
     * テーブル、カラム、レコードのいずれかが存在しない場合は対応するエラー文字列を返す。
     *
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     *
     * @param table
     * @param column
     * @param primaryKey
     * @return 値
     */
    public String getPlayerData(String column, String entry) {

        if (!database.getColumnMap(playerTableName).containsKey(column))
            return ":NO_COLUMN_NAMED_" + column + "_EXIST";

        if (!existPlayer(entry))
            return ":NO_RECORD_FOR_" + entry + "_EXIST";

        if (Commands.checkEntryType(entry).equals("player"))
            entry = database.get(playerTableName, "uuid", "player", entry).stream().findFirst().orElse("");

        return database.get(playerTableName, column, entry);
    }

    /**
     * 名前変更記録用テーブルに登録されているプレイヤーのUUIDとその名前のマップを取得する。
     *
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
     *
     * @return プレイヤー名とそのUUIDのマップ
     */
    public Map<String, String> getPlayersMap() {

        Map<String, String> playersMap = new HashMap<>();

        val statement = database.prepare("SELECT uuid, player FROM " + playerTableName);

        statement.ifPresent(resource -> {
            try (PreparedStatement stmt = resource) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next())
                    playersMap.put(rs.getString("uuid"), rs.getString("player"));
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });

        if (playersMap.isEmpty())
            log.warning(":MAP_IS_EMPTY");
        return playersMap;
    }

    /**
     * エントリーの複数のカラムの値を一気に取得する。 マップはLinkedHashMapで、引数のListの順番を引き継ぐ。
     *
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
     *
     *
     * @return カラムと値のマップ
     */
    public Map<String, String> getPlayerDataMultiValue(List<String> columns, String entry) {
        String entryType = Commands.checkEntryType(entry);

        if (!existPlayer(entryType)) {
            return new HashMap<>();
        }

        if (Commands.checkEntryType(entry).equals("player"))
            entry = getPlayersMap().get(entry);

        return database.getMultiValue(playerTableName, columns, entry);
    }

    /**
     * エントリーの複数のカラムの値を一気に更新する
     *
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
     *
     *
     * @return カラムと値のマップ
     */
    public boolean setPlayerDataMultiValue(Map<String, String> columnValueMap, String entry) {
        String entryType = Commands.checkEntryType(entry);
        return database.setMultiValue(playerTableName, columnValueMap, entry, entryType);
    }
}
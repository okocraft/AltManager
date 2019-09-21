package net.okocraft.playermanager.database;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import net.okocraft.playermanager.PlayerManager;
import net.okocraft.playermanager.command.Commands;
import net.okocraft.playermanager.utilities.InventoryUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class PlayerTable {

    private final Logger log;
    private final Database database;
    @Getter
    private final String playerTableName;

    PlayerTable(Database database) {
        log = PlayerManager.getInstance().getLog();
        this.database = database;
        this.playerTableName = "PlayerManager";

        boolean isTableCreated = database.createTable(playerTableName, "uuid TEXT PRIMARY KEY NOT NULL",
                "player TEXT NOT NULL");
        if (!isTableCreated) {
            log.severe("Failed to create the table.");
        }

        database.addColumn(playerTableName, "previous", "TEXT", null, false);
        database.addColumn(playerTableName, "renamelogondate", "TEXT", LocalDateTime.MIN.format(InventoryUtil.getFormat()), false);
        database.addColumn(playerTableName, "address", "TEXT", null, false);
        database.addColumn(playerTableName, "authorizedalts", "TEXT", null, false);
    }

    /**
     * 名前変更記録用テーブルにプレイヤーを追加する。 showWarningがtrueで失敗した場合はコンソールにログを出力する。
     *
     * @param uuid        UUID
     * @param name        名前
     * @param showWarning コンソールログを出力するかどうか
     * @return 成功すればtrue 失敗すればfalse
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
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
     * @param entry プレイヤー
     * @return 成功すればtrue 失敗すればfalse
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
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
     * @param entry uuidでもmcidでも可
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
     */
    public boolean existPlayer(@NonNull String entry) {
        Map<String, String> playersMap = getPlayersMap();
        return playersMap.containsKey(entry) || playersMap.containsValue(entry);
    }

    /**
     * {@code table}の{@code column}に値をセットする。
     *
     * @param column 更新する列
     * @param entry  プレイヤー。uuidでもmcidでも可
     * @param value  新しい値
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
     */
    public void setPlayerData(String column, String entry, String value) {

        if (!existPlayer(entry)) {
            log.warning(":NO_RECORD_FOR_" + entry + "_EXIST");
            return;
        }

        String entryType = Commands.checkEntryType(entry);
        database.set(playerTableName, column, value, entryType, entry);
    }

    /**
     * {@code table} で指定したテーブルの列 {@code column} の値を取得する。
     * テーブル、カラム、レコードのいずれかが存在しない場合は対応するエラー文字列を返す。
     *
     * @param column カラム
     * @param entry  エントリー
     * @return 値
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     */
    public String getPlayerData(String column, String entry) {

        if (!database.getColumnMap(playerTableName).containsKey(column)) {
            log.warning(":NO_COLUMN_NAMED_" + column + "_EXIST");
            return "";
        }

        if (!existPlayer(entry)) {
            log.warning(":NO_RECORD_FOR_" + entry + "_EXIST");
            return "";
        }

        if (Commands.checkEntryType(entry).equals("player")) {
            entry = database.get(playerTableName, "uuid", "player", entry).stream().findFirst().orElse("");
        }
        return database.get(playerTableName, column, entry);
    }

    public Set<String> getAuthorizedAlts(String entry) {
        String authorizedaltsString = getPlayerData("authorizedalts", entry);
        if (authorizedaltsString.equals("")) {
            log.warning(":NO_AUTHOLIZED_ALTS");
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(authorizedaltsString.split(", ")));
    }

    public void setAuthorizedAlts(String entry, Set<String> authorizedAlts) {
        if (Commands.checkEntryType(entry).equals("player")) {
            entry = getPlayerData("uuid", entry);
        }
        if (authorizedAlts.isEmpty()) {
            database.set(playerTableName, "authorizedalts", "", entry);
            return;
        }

        authorizedAlts.removeIf(uuid -> !Commands.checkEntryType(uuid).equals("uuid"));

        StringBuilder sb = new StringBuilder();
        authorizedAlts.forEach(authorizedAlt -> sb.append(authorizedAlt).append(", "));

        database.set(playerTableName, "authorizedalts", sb.substring(0, sb.length() - 2), entry);
    }

    /**
     * 名前変更記録用テーブルに登録されているプレイヤーのUUIDとその名前のマップを取得する。
     *
     * @return プレイヤー名とそのUUIDのマップ
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
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
     * @return カラムと値のマップ
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
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
     */
    public void setPlayerDataMultiValue(Map<String, String> columnValueMap, String entry) {
        String entryType = Commands.checkEntryType(entry);
        database.setMultiValue(playerTableName, columnValueMap, entry, entryType);
    }
}
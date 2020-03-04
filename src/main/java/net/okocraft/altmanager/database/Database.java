package net.okocraft.altmanager.database;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import net.okocraft.altmanager.AltManager;

/**
 * Bukkit/Spigotサーバーにおける利用を想定したライブラリですが、それ以外でも利用可能です。
 * 利用するときはこのライブラリをshadeしたfatjarを作成してください。
 * このライブラリを利用するのに、HikariCPをshadeする必要はありません。既にこのライブラリに含まれています。
 * <p>
 * 注. このライブラリを使うときは依存に該当するデータベースのJDBCが存在することを確認してください。
 * 例えばBukkitならばSQLiteとMySQLは既にサーバーjar(正確にはBukkitではなくCraftBukkit)に含まれているため
 * 必要ありませんが、Bungeecordではそれらは含まれていないため、このライブラリと一緒にJDBCをshadeして
 * 使えるようにする必要があります。また、BukkitでもSQLiteとMySQL以外のデータベースを使うときは該当するJDBC
 * をshadeする必要があります。
 */
public class Database {

    private static AltManager plugin = AltManager.getInstance();
    private static Database instance;

    /** データベースのコネクションプール。一つの{@code connection}を使いまわしているため、意味があるのか不明。 */
    private final HikariDataSource hikari;

    /** データベースへの接続。 */
    private final Connection connection;

    
    private static final String TABLE_NAME = "altmanager";

    /**
     * 初期設定でSQLiteに接続する。
     * 
     * @param dbPath SQLiteのデータファイルのパス
     * @throws SQLException {@code Connection}の生成中に例外が発生した場合
     */
    public Database(Path dbPath) throws SQLException {
        HikariConfig config = new HikariConfig();
        dbPath.toFile().getParentFile().mkdirs();
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbPath.toFile().getPath());
        hikari = new HikariDataSource(config);
        connection = hikari.getConnection();
        createTable();
    }

    /**
     * 推奨設定でMySQLに接続する。
     * 参照: https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
     * 
     * @param host     ホスト
     * @param port     ポート
     * @param user     ユーザー
     * @param password パスワード
     * @param dbName   データベースの名前
     * @throws SQLException {@code Connection}の生成中に例外が発生した場合
     */
/*    public Database(String host, int port, String user, String password, String dbName) throws SQLException {
        HikariConfig config = new HikariConfig();

        // login data
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?autoReconnect=true&useSSL=false");
        config.setUsername(user);
        config.setPassword(password);

        // general mysql settings
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtsCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("useLocalSessionState", true);
        config.addDataSourceProperty("rewriteBatchedStatements", true);
        config.addDataSourceProperty("cacheResultSetMetadata", true);
        config.addDataSourceProperty("cacheServerConfiguration", true);
        config.addDataSourceProperty("elideSetAutoCommits", true);
        config.addDataSourceProperty("maintainTimeStats", false);
        hikari = new HikariDataSource(config);
        connection = hikari.getConnection();
        createTable();
    }*/

    /**
     * 独自設定でデータベースに接続する。
     * 
     * @param config 設定
     * @throws SQLException {@code Connection}の生成中に例外が発生した場合
     */
/*    public Database(HikariConfig config) throws SQLException {
        hikari = new HikariDataSource(config);
        connection = hikari.getConnection();
        createTable();
    }*/

    private void createTable() {
        execute("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(uuid CHAR(36) NOT NULL, player TEXT NOT NULL, previous TEXT, renamelogondate TEXT DEFAULT '" + LocalDateTime.MIN.format(AltManager.getTimeFormat()) + "', address TEXT, authrozedalts TEXT, PRIMARY KEY (uuid))");
    }

    public static Database getInstance() {
        if (instance == null) {
            try {
                instance = new Database(plugin.getDataFolder().toPath().resolve("data.db"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return instance;
    }

    /**
     * 指定した {@code statement}を実行する。
     * 
     * @param statement 実行するSQL文。メソッド内でPreparedStatementに変換される。
     * @return SQL文の実行に成功したかどうか
     */
    public boolean execute(String SQL) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL)) {
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            System.err.println("Error occurred on executing SQL: " + SQL);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 指定した {@code statement}を実行し、結果を第二引数で処理する。第二引数の処理が終わった後に、ResultSetはクローズされる。
     * 
     * @param queryState 実行するSQL文。メソッド内でPreparedStatementに変換される。
     * @param function 実行結果を受け取る関数。
     * @return fuctionの処理結果
     */
    public <T> T query(String SQL, Function<ResultSet, T> function) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL)) {
            return function.apply(preparedStatement.executeQuery());
        } catch (SQLException e) {
            System.err.println("Error occurred on executing SQL: " + SQL);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 名前変更記録用テーブルに名前が記録されているか調べる。
     *
     * @since 1.0.0-SNAPSHOT
     * @author LazyGon
     *
     * @param entry uuidでもmcidでも可
     */
    public boolean existPlayer(String entry) {
        String entryType = checkEntryType(entry);
         return query("SELECT uuid FROM " + TABLE_NAME + " WHERE " + entryType + " = '" + entry + "'", rs -> {
            try {
                return rs.next();
            } catch (SQLException e) {
                return false;
            }
        });
    }

    
    /**
     * 名前変更記録用テーブルにプレイヤーを追加する。 showWarningがtrueで失敗した場合はコンソールにログを出力する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @param uuid        UUID
     * @param name        名前
     *
     * @return 成功すればtrue 失敗すればfalse
     */
    public boolean addPlayer(String uuid, String name) {
        if (existPlayer(uuid)) {
            return false;
        }

        if (checkEntryType(uuid).equals("player")) {
            return false;
        }

        if (!name.matches("(\\d|[a-zA-Z_]){3,16}")) {
            return false;
        }

        return execute("INSERT INTO " + TABLE_NAME + "(uuid, player) VALUES('" + uuid + "', '" + name + "')");
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
    public boolean removePlayer(String entry) {
        if (!existPlayer(entry)) {
            return false;
        }

        String entryType = checkEntryType(entry);
        return execute("DELETE FROM " + TABLE_NAME + " WHERE " + entryType + " = '" + entry + "'");
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
            return false;
        }

        String entryType = checkEntryType(entry);
        return execute("UPDATE " + TABLE_NAME + " SET " + column + " = '" + value + "' WHERE " + entryType + " = '" + entry + "'");
    }

    
    /**
     * 列 {@code column} の値を取得する。
     * テーブル、カラム、レコードのいずれかが存在しない場合は対応する空文字列を返す。
     *
     * @author LazyGon
     * @since 2.0.0
     *
     * @param column
     * @param entry
     * @return 値
     */
    public String getPlayerData(String column, String entry) {
        String entryType = checkEntryType(entry);
        return query("SELECT " + column + " FROM " + TABLE_NAME + " WHERE " + entryType + " = '" + entry + "'", rs -> {
            try {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString(column)).orElse("");
                }
                return "";
            } catch (SQLException e) {
                e.printStackTrace();
                return "";
            }

        });
    }

    public Set<String> getAuthorizedAlts(String entry) {
        String authorizedaltsString = getPlayerData("authorizedalts", entry);
        if (authorizedaltsString.equals("")) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(authorizedaltsString.split(", ")));
    }
    
    @SuppressWarnings("serial")
    public Map<String, String> getAlts(String address) {
        return query("SELECT uuid, player FROM " + TABLE_NAME + " WHERE address = '" + address + "'", rs -> {
            try {
                return new HashMap<String, String>() {{
                    while (rs.next()) {
                        put(rs.getString("uuid"), rs.getString("player"));
                    }
                }};
            } catch (SQLException e) {
                e.printStackTrace();
                return Map.of();
            }
        });
    }

    public boolean setAuthorizedAlts(String entry, Set<String> authorizedAlts) {
        if (authorizedAlts.isEmpty()) {
            return setPlayerData("authorizedalts", entry, "");
        }

        authorizedAlts.removeIf(uuid -> !checkEntryType(uuid).equals("uuid"));

        StringBuilder sb = new StringBuilder();
        authorizedAlts.forEach(authorizedAlt -> {
            sb.append(authorizedAlt + ", ");
        });

        return setPlayerData("authorizedalts", entry, sb.substring(0, sb.length() - 2));
    }

    public void updateAddress(String oldAddress, String newAddress) {
        execute("UPDATE " + TABLE_NAME + " SET address = '" + newAddress + "' WHERE address = '" + oldAddress + "'");
    }

    public void updateName(String oldName, String newName) {
        execute("UPDATE " + TABLE_NAME + " SET previous = '" + oldName + "', player = '" + newName + "', renamelogondate = '" + LocalDateTime.now(ZoneId.systemDefault()).format(AltManager.getTimeFormat()) + "' WHERE player = '" + oldName + "'");
    }

    /**
     * 名前変更記録用テーブルに登録されているプレイヤーのUUIDとその名前のマップを取得する。
     *
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
     *
     * @return プレイヤー名とそのUUIDのマップ
     */
    @SuppressWarnings("serial")
    public Map<String, String> getPlayersMap() {
        return query("SELECT uuid, player FROM " + TABLE_NAME, rs -> {
            try {
                return new HashMap<String, String>(){{
                    while (rs.next()) {
                        put(rs.getString("uuid"), rs.getString("player"));
                    }
                }};
            } catch (SQLException e) {
                e.printStackTrace();
                return Map.of();
            }
        });
    }

    public void dispose() {
        if (!hikari.isClosed()) {
            hikari.close();
        }
    }

    private String checkEntryType(String entry) {
        try {
            UUID.fromString(entry);
            return "uuid";
        } catch (IllegalArgumentException e) {
            return "player";
        }
    }
}
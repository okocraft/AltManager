package net.okocraft.playermanager.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import net.okocraft.playermanager.PlayerManager;

public class Database {
    /**
     * データベルファイルへの URL 。{@code plugins/PlayerManager/data.db}
     */
    @Getter
    private String fileUrl;

    /**
     * データベースへの URL 。{@code jdbc:sqlite:database}
     */
    @Getter
    private String DBUrl;

    /**
     * データベース接続のプロパティ
     */
    private final Properties DBProps;

    /**
     * データベースの参照用スレッドプール
     */
    private final ExecutorService threadPool;

    /**
     * データベースへの接続。
     */
    private static Optional<Connection> connection = Optional.empty();

    /**
     * ロガー
     */
    private static Logger log;

    /**
     * リネームテーブル操作クラス
     */
    @Getter
    private PlayerTable playerTable;

    public Database(Plugin plugin) {
        // Configure database properties
        DBProps = new Properties();
        DBProps.put("journal_mode", "WAL");
        DBProps.put("synchronous", "NORMAL");

        // Create new thread pool
        threadPool = Executors.newSingleThreadExecutor();

        log = plugin.getLogger();
    }

    /**
     * データベースの初期化を行う。
     *
     * <p>
     * データベースのファイル自体が存在しない場合はファイルを作成する。 ファイル内になんらデータベースが存在しない場合、データベースを新たに生成する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     */
    public boolean connect(String url) {
        // Check if driver exists
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            // log.error("There's no JDBC driver.");
            log.severe("There's no JDBC driver.");
            exception.printStackTrace();

            return false;
        }

        // Set DB URL
        fileUrl = url;
        DBUrl = "jdbc:sqlite:" + url;

        // Check if the database file exists.
        // If not exist, attempt to create the file.
        try {
            val file = Paths.get(fileUrl);

            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException exception) {
            // log.error("Failed to create database file.");
            log.severe("Failed to create database file.");
            exception.printStackTrace();

            return false;
        }

        // Connect to database
        connection = getConnection(DBUrl, DBProps);

        if (!connection.isPresent()) {
            // log.error("Failed to connect the database.");
            log.severe("Failed to connect the database.");

            return false;
        }

        playerTable = new PlayerTable(this);

        return true;
    }

    /**
     * コネクションをリセットし、メモリを開放する。
     */
    public void resetConnection() {
        log.info("Disconnecting.");
        dispose();
        log.info("Getting connection.");
        if (!connect(fileUrl)) {
            log.info("Failed to reset connection. Disabling PlayerManager plugin.");
            Bukkit.getPluginManager().disablePlugin(PlayerManager.getInstance());
        }
        log.info("Database reset complete.");
    }

    public boolean createTable(String table, String... columns) {
        StringBuilder sb = new StringBuilder();
        for (String column : columns) {
            sb.append(column + ", ");
        }
        val statement = prepare(
                "CREATE TABLE IF NOT EXISTS " + table + " (" + sb.substring(0, sb.length() - 2) + ")");
        return statement.map(resource -> {
            try (PreparedStatement stmt = resource) {
                stmt.execute();

                return true;
            } catch (SQLException e) {
                e.printStackTrace();

                return false;
            }
        }).orElse(false);
    }

    /**
     * テーブルを消す。
     * 
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
     * 
     * @param table 削除するテーブルの名前
     */
    public void dropTable(String table) {
        val statement = prepare("DROP TABLE IF EXISTS " + table);
        statement.ifPresent(resource -> {
            try (PreparedStatement stmt = resource) {
                stmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * データベースへの接続を切断する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     */
    public void dispose() {
        connection.ifPresent(connection -> {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        connection = Optional.empty();
    }

    /**
     * 名前変更記録用テーブルにプレイヤーを追加する。
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
    public boolean insert(String table, Map<String, String> defaultValue) {

        if (!getTableMap().containsKey(table)) {
            log.warning(":TABLE_NAMED_" + table + "_NOT_EXIST");
            return false;
        }

        String primaryKeyColumnName = getPrimaryKeyColumnName(table);

        if (!primaryKeyColumnName.equals("") && !defaultValue.containsKey(primaryKeyColumnName)) {
            log.warning(":NEED_PRIMARY_KEY");
            return false;
        }

        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();

        defaultValue.forEach((k, v) -> {
            sb1.append("'" + k + "'" + ", ");
            sb2.append("'" + v + "'" + ", ");
        });

        return prepare("INSERT OR IGNORE INTO " + table + " (" + sb1.substring(0, sb1.length() - 2)
                + ") VALUES (" + sb2.substring(0, sb2.length() - 2) + ")").map(statement -> {
                    try {
                        return statement.execute();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return false;
                    }
                }).orElse(false);
    }

    /**
     * プライマリーキーのみレコードを追加する。
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
    public boolean insert(String table, String primaryKey) {

        String primaryKeyColumnName = getPrimaryKeyColumnName(table);
        return insert(table, new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put(primaryKeyColumnName, primaryKey);
            }
        });

    }

    /**
     * テーブルからプレイヤーを削除する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author LazyGon
     *
     * @param entry プレイヤー
     *
     * @return 成功すればtrue 失敗すればfalse
     */
    public boolean remove(String table, String indexColumn, String indexKey) {

        if (!getTableMap().containsKey(table)) {
            log.warning(":NO_TABLE_NAMED_" + table + "_EXIST");
            return false;
        }

        if (!getColumnMap(table).containsKey(indexColumn)) {
            log.warning(":NO_COLUMN_NAMED_" + indexColumn + "_EXIST");
            return false;
        }

        return prepare("DELETE FROM " + table + " WHERE " + indexColumn + " = ?").map(statement -> {
            try {
                statement.setString(1, indexKey);
                statement.addBatch();

                // Execute this batch
                threadPool.submit(new StatementRunner(statement));
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }).orElse(false);
    }

    /**
     * テーブルからレコードを削除する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author LazyGon
     *
     * @param entry プレイヤー
     *
     * @return 成功すればtrue 失敗すればfalse
     */
    public boolean remove(String table, String primaryKey) {
        String primaryKeyColumnName = getPrimaryKeyColumnName(table);
        return remove(table, primaryKeyColumnName, primaryKey);
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
    public boolean set(String table, String column, String value, String primaryKey) {
        String primaryKeyColumnName = getPrimaryKeyColumnName(table);
        return set(table, column, value, primaryKeyColumnName, primaryKey);
    }

    /**
     * {@code table}の{@code column}に値をセットする。
     * indexColumnに複数の同じindexKeyが含まれる場合は全ての場所に値をセットする。
     *
     * @since 1.0.0-SNAPSHOT
     * @author LazyGon
     *
     * @param column 更新する列
     * @param entry  プレイヤー。uuidでもmcidでも可
     * @param value  新しい値
     * 
     * @return 成功したらtrue 失敗したらfalse
     */
    public boolean set(String table, String column, String value, String indexColumn, String indexKey) {

        if (!getTableMap().containsKey(table)) {
            log.warning(":NO_TABLE_NAMED_" + table + "_EXIST");
            return false;
        }

        if (!getColumnMap(table).containsKey(column)) {
            log.warning(":NO_COLUMN_NAMED_" + column + "_EXIST");
            return false;
        }

        return prepare("UPDATE " + table + " SET " + column + " = ? WHERE " + indexColumn + " = ?").map(statement -> {
            try {
                statement.setString(1, value);
                statement.setString(2, indexKey);
                statement.addBatch();

                // Execute this batch
                threadPool.submit(new StatementRunner(statement));
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }).orElse(false);
    }

    /**
     * {@code table} で指定したテーブルの {@code primaryKey} をインデックスとして列 {@code column}
     * の値を取得する。 テーブル、カラム、レコードのいずれかが存在しない場合は対応するエラー文字列を返す。
     *
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
     *
     * @param table
     * @param column
     * @param primaryKey
     * @return 値
     */
    public String get(String table, String column, String primaryKey) {
        String primaryKeyColumnName = getPrimaryKeyColumnName(table);
        return get(table, column, primaryKeyColumnName, primaryKey).stream().findFirst().orElse("");
    }

    /**
     * {@code table} で指定したテーブルの {@code primaryKey} をインデックスとして列 {@code column}
     * の値を取得する。 テーブル、カラム、レコードのいずれかが存在しない場合は対応するエラー文字列を返す。 nullは弾く。
     *
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     *
     * @param table
     * @param column
     * @param primaryKey
     * 
     * @return 値
     */
    public List<String> get(String table, String column, String indexColumn, String indexKey) {

        if (!getTableMap().containsKey(table)) {
            log.warning(":NO_TAbLE_NAMED_" + table + "_EXIST");
            return new ArrayList<>();
        }

        if (!getColumnMap(table).containsKey(column)) {
            log.warning(":NO_COLUMN_NAMED_" + column + "_EXIST");
            return new ArrayList<>();
        }

        List<String> resultList = new ArrayList<>();
        val statement = prepare("SELECT " + column + " FROM " + table + " WHERE " + indexColumn + " = ?");

        Optional<List<String>> result = statement.map(resource -> {
            try (PreparedStatement stmt = resource) {
                stmt.setString(1, indexKey);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String resultElement = rs.getString(column);
                    if (resultElement != null)
                        resultList.add(resultElement);
                }
                return resultList;
            } catch (SQLException exception) {
                exception.printStackTrace();

                return new ArrayList<>();
            }
        });

        return result.orElse(resultList);
    }

    /**
     * テーブルに新しい列 {@code column} を追加する。
     *
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     *
     * @param column       列の名前。
     * @param type         列の型。
     * @param defaultValue デフォルトの値。必要ない場合はnullを指定する。
     * @param showWarning  同じ列が存在したときにコンソールに警告を表示するかどうか
     *
     * @return 成功したなら {@code true} 、さもなくば {@code false} 。
     */
    public boolean addColumn(String table, String column, String type, String defaultValue, boolean showWarning) {

        if (getColumnMap(table).containsKey(column)) {
            if (showWarning)
                log.warning(":COLUMN_EXIST");
            return false;
        }

        defaultValue = (defaultValue != null) ? " NOT NULL DEFAULT '" + defaultValue + "'" : "";
        val statement = prepare("ALTER TABLE " + table + " ADD " + column + " " + type + defaultValue);

        return statement.map(stmt -> {
            try {
                stmt.addBatch();

                // Execute this batch
                threadPool.submit(new StatementRunner(stmt));
                return true;
            } catch (SQLException exception) {
                exception.printStackTrace();
                return false;
            }
        }).orElse(false);
    }

    /**
     * テーブル {@code table} から列 {@code column} を削除する。
     *
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     *
     * @param table  削除する列があるテーブル
     * @param column 削除する列の名前。
     *
     * @return 成功したなら {@code true} 、さもなくば {@code false} 。
     */
    public boolean dropColumn(String table, String column) {

        Map<String, String> columnMap = getColumnMap(table);

        if (!columnMap.containsKey(column)) {
            log.warning(":NO_COLUMN_NAMED_" + column + "_EXIST");
            return false;
        }

        // 新しいテーブルの列
        StringBuilder columnsBuilder = new StringBuilder();
        // 新しいテーブルの列 (型なし)
        StringBuilder colmunsBuilderExcludeType = new StringBuilder();

        columnMap.forEach((colName, colType) -> {
            if (!column.equals(colName)) {
                columnsBuilder.append(colName + " " + colType + ", ");
                colmunsBuilderExcludeType.append(colName + ", ");
            }
        });
        String columns = columnsBuilder.toString().replaceAll(", $", "");
        String columnsExcludeType = colmunsBuilderExcludeType.toString().replaceAll(", $", "");

        Statement statement = null;

        try {
            statement = connection.get().createStatement();

            statement.addBatch("BEGIN TRANSACTION");
            statement.addBatch("ALTER TABLE " + table + " RENAME TO temp_" + table + "");
            statement.addBatch("CREATE TABLE " + table + " (" + columns + ")");
            statement.addBatch("INSERT INTO " + table + " (" + columnsExcludeType + ") SELECT " + columnsExcludeType
                    + " FROM temp_" + table + "");
            statement.addBatch("DROP TABLE temp_" + table + "");
            statement.addBatch("COMMIT");

            // Execute this batch
            threadPool.submit(new StatementRunner(statement));
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
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
    public Multimap<String, String> getMultiValue(String table, List<String> columns, String indexColumn,
            String indexKey) {

        Multimap<String, String> resultMap = ArrayListMultimap.create();

        if (!getTableMap().containsKey(table)) {
            log.warning(":NO_TABLE_NAMED_" + table + "_EXIST");
            return resultMap;
        }

        StringBuilder sb = new StringBuilder();
        for (String columnName : columns)
            sb.append(columnName + ", ");

        String multipleColumnName = sb.substring(0, sb.length() - 2);

        val statement = prepare("SELECT " + multipleColumnName + " FROM " + table + " WHERE " + indexColumn + " = ?");

        return statement.map(resource -> {
            try (PreparedStatement stmt = resource) {
                stmt.setString(1, indexKey);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    for (String column : columns) {
                        resultMap.put(column, rs.getString(column));
                    }
                }
                return resultMap;
            } catch (SQLException exception) {
                exception.printStackTrace();
                Multimap<String, String> empty = ArrayListMultimap.create();
                return empty;
            }
        }).get();
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
    public Map<String, String> getMultiValue(String table, List<String> columns, String primaryKey) {
        String primaryKeyColumnName = getPrimaryKeyColumnName(table);
        if (!getTableMap().containsKey(table)) {
            log.warning(":NO_TABLE_NAMED_" + table + "_EXIST");
            return new HashMap<>();
        }

        StringBuilder sb = new StringBuilder();
        for (String columnName : columns)
            sb.append(columnName + ", ");

        String multipleColumnName = sb.substring(0, sb.length() - 2);

        val statement = prepare(
                "SELECT " + multipleColumnName + " FROM " + table + " WHERE " + primaryKeyColumnName + " = ?");

        return statement.map(resource -> {
            try (PreparedStatement stmt = resource) {
                stmt.setString(1, primaryKey);
                ResultSet rs = stmt.executeQuery();

                return columns.stream().collect(Collectors.toMap(columnName -> columnName, columnName -> {
                    try {
                        return rs.getString(columnName);
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                        return "";
                    }
                }, (e1, e2) -> e1, LinkedHashMap::new));
            } catch (SQLException exception) {
                exception.printStackTrace();
                return new LinkedHashMap<String, String>();
            }
        }).get();
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
    public boolean setMultiValue(String table, Map<String, String> columnValueMap, String indexKey,
            String indexColumn) {

        StringBuilder sb = new StringBuilder();
        columnValueMap.forEach((columnName, columnValue) -> {
            sb.append(columnName + " = '" + columnValue + "', ");
        });

        val statement = prepare(
                "UPDATE " + table + " SET " + sb.substring(0, sb.length() - 2) + " WHERE " + indexColumn + " = ?");

        return statement.map(resource -> {
            try (PreparedStatement stmt = resource) {
                stmt.setString(1, indexKey);
                stmt.executeUpdate();
                return true;
            } catch (SQLException exception) {
                exception.printStackTrace();
                return false;
            }
        }).orElse(false);
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
    public boolean setMultiValue(String table, Map<String, String> columnValueMap, String primaryKey) {
        String primaryKeyColumnName = getPrimaryKeyColumnName(table);
        return setMultiValue(table, columnValueMap, primaryKey, primaryKeyColumnName);
    }

    /**
     * テーブルに含まれる列 {@code column} のリストを取得する。
     *
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
     *
     *
     * @return テーブルに含まれるcolumnの名前と型のマップ 失敗したら空のマップを返す。
     */
    public Map<String, String> getColumnMap(String table) {

        Map<String, String> columnMap = new HashMap<>();

        val statement = prepare("SELECT * FROM " + table + " WHERE 0=1");

        return statement.map(resource -> {
            try (PreparedStatement stmt = resource) {
                ResultSetMetaData rsmd = stmt.executeQuery().getMetaData();

                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    columnMap.put(rsmd.getColumnName(i), rsmd.getColumnTypeName(i));
                }

                return columnMap;
            } catch (SQLException exception) {
                exception.printStackTrace();
                return new HashMap<String, String>();
            }
        }).orElse(columnMap);
    }

    public String getPrimaryKeyColumnName(String table) {
        if (!getTableMap().containsKey(table)) {
            return "";
        }

        return connection.map(con -> {
            try {
                ResultSet resultSet = con.getMetaData().getPrimaryKeys(con.getCatalog(), null, table);
                if (resultSet.next()) {
                    return resultSet.getString("COLUMN_NAME");
                }
                return "";
            } catch (SQLException e) {
                e.printStackTrace();
                return "";
            }
        }).orElse("");
    }

    /**
     * すべてのテーブル名前と型のマップを取得する。
     *
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
     *
     * @return テーブル名と型のマップ
     */
    public Map<String, String> getTableMap() {

        Map<String, String> tableMap = new HashMap<>();

        return connection.map(con -> {
            try {
                ResultSet resultSet = con.getMetaData().getTables(null, null, null, new String[] { "TABLE" });

                while (resultSet.next()) {
                    tableMap.put(resultSet.getString("TABLE_NAME"), resultSet.getString("TABLE_TYPE"));
                }

                return tableMap;
            } catch (SQLException exception) {
                exception.printStackTrace();
                return new HashMap<String, String>();
            }
        }).orElse(tableMap);
    }

    /**
     * スレッド上で SQL を実行する。
     *
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     *
     * @param statement SQL 準備文。
     *
     * @return {@Code ResultSet}
     */
    public Optional<ResultSet> exec(PreparedStatement statement) {
        val thread = threadPool.submit(new StatementCaller(statement));

        try {
            return thread.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();

            return Optional.empty();
        }
    }

    /**
     * SQL 準備文を構築する。
     *
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     *
     * @param sql SQL 文。
     *
     * @return SQL 準備文
     */
    public Optional<PreparedStatement> prepare(@NonNull String sql) {
        if (connection.isPresent()) {
            try {
                return Optional.of(connection.get().prepareStatement(sql));
            } catch (SQLException e) {
                e.printStackTrace();

                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Connection(String, Properties)} のラッパーメソッド。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @see DriverManager#getConnection(String, Properties)
     *
     * @param url   {@code jdbc:subprotocol:subname} という形式のデータベース URL
     * @param props データベースの取り扱いについてのプロパティ
     *
     * @return 指定されたデータベースへの接続 {@code Connect} 。
     */
    private static Optional<Connection> getConnection(@NonNull String url, Properties props) {
        try {
            return Optional.of(DriverManager.getConnection(url, props));
        } catch (SQLException exception) {
            exception.printStackTrace();

            return Optional.empty();
        }
    }
}

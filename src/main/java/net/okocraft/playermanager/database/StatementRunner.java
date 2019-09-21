package net.okocraft.playermanager.database;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * @since 1.0.0-SNAPSHOT
 * @author akaregi
 */
class StatementRunner implements Runnable {
    private final Statement statement;

    StatementRunner(Statement statement) {
        this.statement = statement;
    }

    @Override
    public void run() {
        try (Statement ignored = statement) {
            statement.executeBatch();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

}

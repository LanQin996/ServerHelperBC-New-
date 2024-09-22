package github.kasuminova.serverhelperbc.database;

import com.google.common.base.Preconditions;
import github.kasuminova.serverhelperbc.ServerHelperBC;
import io.netty.util.internal.ThrowableUtil;

import java.sql.*;

public class WhitelistSQLManager {

    public static final String WHITELIST_TABLE_CREATE_SQL = """
            # 主表
            CREATE TABLE IF NOT EXISTS `whitelist` (
                `id` long PRIMARY KEY NOT NULL COMMENT 'QQ',
                `userName` varchar(18) UNIQUE KEY NOT NULL COMMENT '用户名',
                `lastUpdateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                `deleted` bool NOT NULL DEFAULT 0 COMMENT '已删除'
            );
            # 历史记录
            CREATE TABLE IF NOT EXISTS `history` (
                `order_id` int(4) PRIMARY KEY AUTO_INCREMENT NOT NULL,
                `id` long UNIQUE KEY NOT NULL COMMENT 'QQ',
                `userName` varchar(18) NOT NULL COMMENT '用户名',
                `lastUpdateTime` timestamp NOT NULL COMMENT '修改时间',
                FOREIGN KEY (`id`) REFERENCES `whitelist`(`id`)
            );
            # 更新监听器
            CREATE TRIGGER whitelist_update_handler
            BEFORE UPDATE ON `whitelist`
            FOR EACH ROW
            BEGIN
                -- 插入旧记录到 history 表
                INSERT INTO `history` (id, userName, lastUpdateTime)
                VALUES (OLD.id, OLD.userName, OLD.lastUpdateTime);
            END;
            """;

    public static boolean driverInitialized = false;

    protected Connection conn = null;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverInitialized = true;
        } catch (ClassNotFoundException e) {
            ServerHelperBC.logger.error("SQL Driver init failed!", e);
        }
    }

    public void connect(final String ip, final String port, final String database, final String parameters, final String user, final String password) throws SQLException {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                ServerHelperBC.logger.warn("Failed to close sql connection!");
                ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
            }
            conn = null;
        }

        String url = "jdbc:mysql://%s:%s/%s%s".formatted(ip, port, database, parameters.isEmpty() ? "" : "?" + parameters);
        conn = DriverManager.getConnection(url, user, password);
        createWhiteListTable();
    }

    public void disconnect() {
        if (conn == null) {
            return;
        }
        try {
            if (conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
        }
        conn = null;
    }

    public void createWhiteListTable() throws SQLException {
        Preconditions.checkState(isConnected(), "SQL is not connected!");
        
        Statement statement = conn.createStatement();
        statement.execute(WHITELIST_TABLE_CREATE_SQL);
        statement.close();
    }

    public boolean isConnected() {
        try {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public Connection getConnection() {
        return conn;
    }

}

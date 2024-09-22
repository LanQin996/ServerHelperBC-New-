package github.kasuminova.serverhelperbc.whitelist;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import github.kasuminova.network.message.whitelist.*;
import github.kasuminova.serverhelperbc.ServerHelperBC;
import github.kasuminova.serverhelperbc.database.WhitelistSQLManager;
import io.netty.util.internal.ThrowableUtil;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SQLWhiteList implements WhiteList {

    public static final String CHECK_ID_EXISTS_SQL = "SELECT COUNT(*) FROM `whitelist` WHERE `id` = ?";

    public static final String INSERT_SQL =
            "INSERT INTO `whitelist` (`id`, `userName`) VALUES (?, ?)";

    public static final String DELETE_SQL =
            "UPDATE `whitelist` SET `deleted` = true WHERE `id` = ?";

    public static final String DELETE_PERMANENT_WITH_ID_SQL =
            "DELETE FROM `whitelist` WHERE `id` = ?";

    public static final String UPDATE_USERNAME_SQL =
            "UPDATE `whitelist` SET `userName` = ? WHERE `id` = ?";

    public static final String UPDATE_ID_SQL =
            "UPDATE `whitelist` SET `id` = ? WHERE `id` = ?";

    public static final String GET_HISTORY_WITH_ID_SQL =
            "SELECT `id`, `userName`, `lastUpdateTime` FROM `history` `w` WHERE `id` = ?";

    public static final String GET_FULL_INFO_WITH_ID_SQL =
            "SELECT `id`, `userName`, `lastUpdateTime`, `deleted` FROM `whitelist` `w` WHERE `id` = ? LIMIT 1";

    public static final String GET_FULL_INFO_WITH_USERNAME_SQL =
            "SELECT `id`, `userName`, `lastUpdateTime`, `deleted` FROM `whitelist` `w` WHERE `userName` = ? LIMIT 1";

    public static final String GET_INFO_WITH_USERNAME_SQL =
            "SELECT `id`, `userName` FROM `whitelist` `w` WHERE `userName` = ? LIMIT 1";

    public static final String GET_INFO_WITH_ID_SQL =
            "SELECT `id`, `userName` FROM `whitelist` `w` WHERE `id` = ?";

    protected final WhitelistSQLManager sqlManager = new WhitelistSQLManager();

    protected final Cache<String, Optional<FullWhiteListInfo>> localCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    @Override
    public WhiteListUpdateResult add(final FullWhiteListInfo toAdd) {
        try {
            WhiteListInfo exists;

            exists = get(toAdd.getId());
            if (exists != null) {
                return new WhiteListUpdateResult(UpdateType.ADD, ResultCode.ID_ALREADY_EXISTS, exists.toFullWhiteListInfo());
            }

            exists = get(toAdd.getUserName());
            if (exists != null) {
                return new WhiteListUpdateResult(UpdateType.ADD, ResultCode.USERNAME_ALREADY_EXISTS, exists.toFullWhiteListInfo());
            }
        } catch (Exception e) {
            ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
        }

        Connection conn = sqlManager.getConnection();

        try {
            PreparedStatement statement = conn.prepareStatement(INSERT_SQL);
            statement.setLong(1, toAdd.getId());
            statement.setString(2, toAdd.getUserName());
            statement.executeUpdate();
        } catch (Exception e) {
            ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
        }

        localCache.invalidate(toAdd.getUserName());
        return new WhiteListUpdateResult(UpdateType.ADD, toAdd);
    }

    @Override
    public WhiteListUpdateResult get(final String key, final int searchMethod) {
        Connection conn = sqlManager.getConnection();

        try {
            FullWhiteListInfo result;
            switch (searchMethod) {
                case SearchMethod.SEARCH_ID -> {
                    WhiteListInfo get = get(key);
                    if (get == null) {
                        return new WhiteListUpdateResult(UpdateType.GET, ResultCode.USERNAME_NOT_EXIST);
                    }
                    result = get.toFullWhiteListInfo();
                }
                case SearchMethod.SEARCH_QQ -> {
                    WhiteListInfo get = get(Long.parseLong(key));
                    if (get == null) {
                        return new WhiteListUpdateResult(UpdateType.GET, ResultCode.ID_NOT_EXIST);
                    }
                    result = get.toFullWhiteListInfo();
                }
                default -> throw new IllegalArgumentException("Unknown search method: " + searchMethod);
            }

            PreparedStatement statement = conn.prepareStatement(GET_HISTORY_WITH_ID_SQL);
            statement.setLong(1, result.getId());
            ResultSet resultSet = statement.executeQuery();

            List<WhiteListInfo> history = result.getHistory();
            while (resultSet.next()) {
                history.add(new WhiteListInfo(resultSet));
            }
            return new WhiteListUpdateResult(UpdateType.GET, result);
        } catch (Exception e) {
            ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
        }

        return switch (searchMethod) {
            case SearchMethod.SEARCH_ID -> new WhiteListUpdateResult(UpdateType.GET, ResultCode.USERNAME_NOT_EXIST);
            case SearchMethod.SEARCH_QQ -> new WhiteListUpdateResult(UpdateType.GET, ResultCode.ID_NOT_EXIST);
            default -> null;
        };
    }

    @Override
    public WhiteListUpdateResult update(final String oldName, final String newName) {
        try {
            WhiteListInfo result = get(oldName);
            if (result != null) {
                WhiteListInfo exists = get(newName);
                if (exists == null) {
                    Connection conn = sqlManager.getConnection();
                    PreparedStatement statement = conn.prepareStatement(UPDATE_USERNAME_SQL);

                    statement.setString(1, newName);
                    statement.setLong(2, result.getId());
                    statement.executeUpdate();

                    localCache.invalidate(oldName);
                } else {
                    return new WhiteListUpdateResult(UpdateType.UPDATE, ResultCode.USERNAME_ALREADY_EXISTS);
                }
            } else {
                return new WhiteListUpdateResult(UpdateType.UPDATE, ResultCode.USERNAME_NOT_EXIST);
            }
        } catch (Exception e) {
            ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
        }
        return null;
    }

    @Override
    public WhiteListUpdateResult update(final String oldId, final long newId) {
        try {
            WhiteListInfo result = get(Long.parseLong(oldId));
            if (result != null) {
                WhiteListInfo exists = get(newId);
                if (exists == null) {
                    Connection conn = sqlManager.getConnection();
                    PreparedStatement statement = conn.prepareStatement(UPDATE_ID_SQL);

                    statement.setLong(1, newId);
                    statement.setLong(2, result.getId());
                    statement.executeUpdate();

                    localCache.invalidate(result.getUserName());
                } else {
                    return new WhiteListUpdateResult(UpdateType.UPDATE, ResultCode.ID_ALREADY_EXISTS);
                }
            } else {
                return new WhiteListUpdateResult(UpdateType.UPDATE, ResultCode.ID_NOT_EXIST);
            }
        } catch (Exception e) {
            ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
        }
        return null;
    }

    @Nullable
    public WhiteListInfo get(long qq) throws Exception {
        Connection conn = sqlManager.getConnection();

        PreparedStatement statement = conn.prepareStatement(GET_FULL_INFO_WITH_ID_SQL);
        statement.setLong(1, qq);

        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return new WhiteListInfo(resultSet);
        }

        return null;
    }

    @Nullable
    public WhiteListInfo get(String userName) throws Exception {
        Connection conn = sqlManager.getConnection();

        PreparedStatement statement = conn.prepareStatement(GET_FULL_INFO_WITH_USERNAME_SQL);
        statement.setString(1, userName);

        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return new WhiteListInfo(resultSet);
        }

        return null;
    }

    @Override
    public String getUserName(final String qq) {
        try {
            WhiteListInfo result = get(Long.parseLong(qq));
            if (result != null) {
                return result.getUserName();
            }
        } catch (Exception e) {
            ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
        }
        return null;
    }

    @Override
    public WhiteListUpdateResult remove(final String key, final int searchMethod, final boolean removeRecycleBin) {
        WhiteListInfo result;
        try {
            switch (searchMethod) {
                case SearchMethod.SEARCH_ID -> {
                    result = get(key);
                    if (result == null) {
                        return new WhiteListUpdateResult(UpdateType.REMOVE, ResultCode.USERNAME_NOT_EXIST);
                    }
                }
                case SearchMethod.SEARCH_QQ -> {
                    result = get(Long.parseLong(key));
                    if (result == null) {
                        return new WhiteListUpdateResult(UpdateType.REMOVE, ResultCode.ID_NOT_EXIST);
                    }
                }
                default -> throw new IllegalArgumentException("Unknown search method: " + searchMethod);
            }

            Connection conn = sqlManager.getConnection();
            PreparedStatement statement = removeRecycleBin
                    ? conn.prepareStatement(DELETE_PERMANENT_WITH_ID_SQL)
                    : conn.prepareStatement(DELETE_SQL);

            statement.setLong(1, result.getId());
            statement.executeUpdate();

            localCache.invalidate(result.getUserName());
            return new WhiteListUpdateResult(UpdateType.REMOVE, ResultCode.SUCCESS, result.toFullWhiteListInfo());
        } catch (Exception e) {
            ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
        }

        return null;
    }

    @Override
    public boolean isInList(final String userName) {
        try {
            return localCache.get(userName, () -> {
                WhiteListInfo info = get(userName);
                return info == null ? Optional.empty() : Optional.of(info.toFullWhiteListInfo());
            }).isPresent();
        } catch (ExecutionException e) {
            ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
        }
        return false;
    }

}

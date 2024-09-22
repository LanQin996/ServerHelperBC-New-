package github.kasuminova.serverhelperbc.whitelist;

import github.kasuminova.network.message.whitelist.*;
import github.kasuminova.serverhelperbc.ServerHelperBC;
import github.kasuminova.serverhelperbc.util.ColouredLogger;
import io.netty.util.internal.ThrowableUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自动维护本地文件的一个白名单系统。
 * TODO 计划重写部分逻辑（屎山）
 */
public class FileWhiteList implements WhiteList, SearchMethod {
    private final File whiteListFile;
    private final File whiteListRecycleBinFile;
    private final ColouredLogger logger;

    /**
     *  参数分别为 String userName, String id
     *  <p>
     *      此 map 用于检查白名单、同步和快速搜索
     *  </p>
     */
    private final ConcurrentHashMap<String, String> whiteList = new ConcurrentHashMap<>(100);
    /**
     *  参数分别为 String userName, String id
     *  <p>
     *      此 map 用于同步回收站和快速搜索
     *  </p>
     */
    private final ConcurrentHashMap<String, String> whiteListRecycleBin = new ConcurrentHashMap<>(100);

    private final SimpleWhiteListMap fullWhiteList;

    //参数分别为 ConcurrentHashMap<String id, WhiteListInfo info>
    private ConcurrentHashMap<String, FullWhiteListInfo> fullWhiteListMap;
    private final SimpleWhiteListMap fullWhiteListRecycleBin;

    //参数分别为 ConcurrentHashMap<String id, WhiteListInfo info>
    private ConcurrentHashMap<String, FullWhiteListInfo> fullWhiteListRecycleBinMap;
    private Thread automaticTask;
    private volatile boolean isStarted;

    /**
     * 新建一个自动维护的系统实例。此时还尚未初始化，必须调用 load() 后才能使用。
     *
     * @param dataFolder 数据文件夹，该文件夹下会创建 whitelist.json 和 whitelist_removed.json 两个文件
     */
    public FileWhiteList(File dataFolder) {
        this.logger = ServerHelperBC.logger;
        this.whiteListFile = new File(dataFolder.getPath() + File.separator + "whitelist.json");
        this.whiteListRecycleBinFile = new File(dataFolder.getPath() + File.separator + "whitelist_recycle_bin.json");

        fullWhiteList = new SimpleWhiteListMap(whiteListFile);

        fullWhiteListRecycleBin = new SimpleWhiteListMap(whiteListRecycleBinFile);
    }

    /**
     * 添加一个白名单信息，如果移除记录中有对应的信息，则从移除信息中还原。
     *
     * @param fullWhiteListInfo 白名单信息
     * @return 添加成功返回添加的白名单信息，用户名被其他 QQ 绑定或当前 QQ 已绑定用户名时返回错误信息和冲突的白名单信息
     */
    @Override
    public WhiteListUpdateResult add(FullWhiteListInfo fullWhiteListInfo) {
        String id = fullWhiteListInfo.getIdAsString();
        String userName = fullWhiteListInfo.getUserName();

        if (whiteList.containsKey(userName)) {
            WhiteListUpdateResult result = get(userName, SEARCH_ID);
            return new WhiteListUpdateResult(UpdateType.ADD, ResultCode.USERNAME_ALREADY_EXISTS, result.getFullWhiteListInfo());
        }
        if (whiteList.containsValue(id)) {
            WhiteListUpdateResult result = get(id, SEARCH_QQ);
            return new WhiteListUpdateResult(UpdateType.ADD, ResultCode.ID_ALREADY_EXISTS, result.getFullWhiteListInfo());
        }

        //搜索回收站
        if (whiteListRecycleBin.containsKey(userName)) {
            FullWhiteListInfo oldInfo = fullWhiteListRecycleBinMap.get(whiteListRecycleBin.get(userName));
            if (!oldInfo.getIdAsString().equals(id)) {
                WhiteListUpdateResult result = getInRecycleBin(oldInfo.getIdAsString(), SEARCH_QQ);
                return new WhiteListUpdateResult(UpdateType.ADD, ResultCode.USERNAME_ALREADY_EXISTS, result.getFullWhiteListInfo());
            } else {
                fullWhiteListInfo.addHistory(oldInfo, true);
                fullWhiteListRecycleBinMap.remove(id);
                fullWhiteListRecycleBin.setLastUpdateTimeNow();

                whiteListRecycleBin.remove(userName);
                fullWhiteList.setLastUpdateTimeNow();
            }
        }
        if (whiteListRecycleBin.containsValue(id)) {
            FullWhiteListInfo oldInfo = fullWhiteListRecycleBinMap.get(id);
            if (!oldInfo.getUserName().equals(userName)) {
                WhiteListUpdateResult result = getInRecycleBin(id, SEARCH_QQ);
                return new WhiteListUpdateResult(UpdateType.ADD, ResultCode.ID_ALREADY_EXISTS, result.getFullWhiteListInfo());
            } else {
                fullWhiteListInfo.addHistory(oldInfo, true);
                fullWhiteListRecycleBinMap.remove(id);
                fullWhiteListRecycleBin.setLastUpdateTimeNow();

                whiteListRecycleBin.remove(userName);
                fullWhiteList.setLastUpdateTimeNow();
            }
        }

        this.whiteList.put(userName, fullWhiteListInfo.getIdAsString());
        fullWhiteListMap.put(id, fullWhiteListInfo);
        fullWhiteList.setLastUpdateTimeNow();

        return new WhiteListUpdateResult(UpdateType.ADD, fullWhiteListInfo);
    }

    /**
     * 移除一个白名单，根据关键词搜索并删除
     *
     * @param key          QQ 或 用户名
     * @param searchMethod 搜索方式
     * @param removeRecycleBin 是否删除回收站内的白名单而不是当前白名单
     * @return 删除成功返回 ResultCode.SUCCESS，用户名或 QQ 不存在时返回错误信息
     */
    @Override
    public WhiteListUpdateResult remove(String key, int searchMethod, boolean removeRecycleBin) {
        if (removeRecycleBin) {
            return removeRecycleBin(key, searchMethod);
        } else {
            return remove(key, searchMethod);
        }
    }

    /**
     * 移除一个白名单，根据关键词搜索并删除
     *
     * @param key          QQ 或 用户名
     * @param searchMethod 搜索方式
     * @return 删除成功返回 ResultCode.SUCCESS，用户名或 QQ 不存在时返回错误信息
     */
    public WhiteListUpdateResult remove(String key, int searchMethod) {
        String id = searchID(key, searchMethod);
        if (id == null) return new WhiteListUpdateResult(UpdateType.REMOVE, ResultCode.ID_NOT_EXIST);

        FullWhiteListInfo info = fullWhiteListMap.get(id);

        if (info != null) {
            whiteList.remove(info.getUserName());
            fullWhiteListMap.remove(id);
            fullWhiteList.setLastUpdateTimeNow();

            FullWhiteListInfo oldInfo = fullWhiteListRecycleBinMap.get(id);
            if (oldInfo != null) {
                fullWhiteListRecycleBinMap.remove(id);

                whiteListRecycleBin.remove(info.getUserName());

                info.mergeHistory(oldInfo);
            }

            whiteListRecycleBin.put(info.getUserName(), id);
            fullWhiteListRecycleBinMap.put(id, info);
            fullWhiteListRecycleBin.setLastUpdateTimeNow();

            return new WhiteListUpdateResult(UpdateType.REMOVE, ResultCode.SUCCESS, oldInfo);
        } else {
            return new WhiteListUpdateResult(UpdateType.REMOVE, ResultCode.USERNAME_NOT_EXIST);
        }
    }

    /**
     * 移除一个白名单，根据关键词搜索并删除
     *
     * @param key          QQ 或 用户名
     * @param searchMethod 搜索方式
     * @return 删除成功返回 ResultCode.SUCCESS，用户名或 QQ 不存在时返回错误信息
     */
    public WhiteListUpdateResult removeRecycleBin(String key, int searchMethod) {
        String id = searchIDRecycleBin(key, searchMethod);
        if (id == null) return new WhiteListUpdateResult(UpdateType.REMOVE, ResultCode.ID_NOT_EXIST);

        FullWhiteListInfo info = fullWhiteListRecycleBinMap.get(id);

        if (info != null) {
            whiteListRecycleBin.remove(info.getUserName());
            fullWhiteListRecycleBinMap.remove(id);
            fullWhiteListRecycleBin.setLastUpdateTimeNow();

            return new WhiteListUpdateResult(UpdateType.REMOVE, ResultCode.SUCCESS, info);
        } else {
            return new WhiteListUpdateResult(UpdateType.REMOVE, ResultCode.USERNAME_NOT_EXIST);
        }
    }

    /**
     * 获取一个白名单，根据关键词获取
     *
     * @param key          QQ 或 用户名
     * @param searchMethod 搜索方式
     * @return 白名单信息，不存在则返回失败信息
     */
    @Override
    public WhiteListUpdateResult get(String key, int searchMethod) {
        String id = searchID(key, searchMethod);
        if (id == null) return new WhiteListUpdateResult(UpdateType.GET, ResultCode.ID_NOT_EXIST);

        FullWhiteListInfo result = fullWhiteListMap.get(id);
        if (result == null) return new WhiteListUpdateResult(UpdateType.GET, ResultCode.USERNAME_NOT_EXIST);
        return new WhiteListUpdateResult(UpdateType.GET, result);
    }

    /**
     * 获取一个回收站内的白名单，根据关键词获取
     *
     * @param key          QQ 或 用户名
     * @param searchMethod 搜索方式
     * @return 白名单信息，不存在则返回失败信息
     */
    public WhiteListUpdateResult getInRecycleBin(String key, int searchMethod) {
        String id = searchIDRecycleBin(key, searchMethod);
        if (id == null) return new WhiteListUpdateResult(UpdateType.GET, ResultCode.ID_NOT_EXIST);

        FullWhiteListInfo result = fullWhiteListRecycleBinMap.get(id);
        if (result == null) return new WhiteListUpdateResult(UpdateType.GET, ResultCode.USERNAME_NOT_EXIST);
        return new WhiteListUpdateResult(UpdateType.GET, result);
    }

    /**
     * 获取对应 QQ 的白名单用户名
     *
     * @param id QQ
     * @return 对应的用户名，如果无则返回 null
     */
    @Override
    public String getUserName(String id) {
        FullWhiteListInfo fullWhiteListInfo = fullWhiteListMap.get(id);
        return fullWhiteListInfo == null ? null : fullWhiteListInfo.getUserName();
    }

    /**
     * 更新一个白名单信息，根据关键词获取信息
     *
     * @param oldName 旧用户名
     * @param newName 新用户名
     * @return 更新后的信息，用户名或 QQ 不存在时返回失败信息
     */
    public WhiteListUpdateResult update(String oldName, String newName) {
        String id = searchID(oldName, SearchMethod.SEARCH_ID);
        if (id == null) return new WhiteListUpdateResult(UpdateType.UPDATE, ResultCode.ID_NOT_EXIST);

        FullWhiteListInfo oldInfo = fullWhiteListMap.get(id);
        FullWhiteListInfo newInfo = oldInfo.copySelf(true);

        whiteList.remove(oldInfo.getUserName());
        fullWhiteListMap.remove(String.valueOf(oldInfo.getId()));

        newInfo.setUserName(newName);
        newInfo.setLastUpdateTimeNow();

        whiteList.put(newInfo.getUserName(), newInfo.getIdAsString());
        fullWhiteListMap.put(String.valueOf(newInfo.getId()), newInfo);
        fullWhiteList.setLastUpdateTimeNow();

        return new WhiteListUpdateResult(UpdateType.UPDATE, newInfo);
    }

    /**
     * 更新一个白名单信息，根据关键词获取信息
     *
     * @param oldId 旧 QQ
     * @param newId 新 QQ
     * @return 更新后的信息，用户名或 QQ 不存在时返回失败信息
     */
    public WhiteListUpdateResult update(String oldId, long newId) {
        String id = searchID(oldId, SearchMethod.SEARCH_QQ);
        if (id == null) return new WhiteListUpdateResult(UpdateType.UPDATE, ResultCode.USERNAME_NOT_EXIST);

        FullWhiteListInfo oldInfo = fullWhiteListMap.get(id);
        FullWhiteListInfo newInfo = oldInfo.copySelf(true);

        whiteList.remove(oldInfo.getUserName());
        fullWhiteListMap.remove(String.valueOf(oldInfo.getId()));

        newInfo.setId(newId);
        newInfo.setLastUpdateTimeNow();

        whiteList.put(newInfo.getUserName(), newInfo.getIdAsString());
        fullWhiteListMap.put(String.valueOf(newInfo.getId()), newInfo);
        fullWhiteList.setLastUpdateTimeNow();

        return new WhiteListUpdateResult(UpdateType.UPDATE, newInfo);
    }

    /**
     * 指定的用户名是否在白名单内
     *
     * @param userName 用户名
     * @return 有则返回 true, 无则返回 false
     */
    public boolean isInList(String userName) {
        return whiteList.containsKey(userName);
    }

    /**
     * 根据搜索方式获取 QQ
     *
     * @param key          QQ 或 用户名
     * @param searchMethod 搜索方式
     * @return QQ，不存在时返回 null
     */
    public String searchID(String key, int searchMethod) {
        switch (searchMethod) {
            case SEARCH_ID:
                return whiteList.get(key);
            case SEARCH_QQ:
                return key;
            default:
                throw new IllegalArgumentException("Invalid searchMethod!");
        }
    }

    /**
     * 根据搜索方式获取 QQ，从回收站列表获取。
     *
     * @param key          QQ 或 用户名
     * @param searchMethod 搜索方式
     * @return QQ，不存在时返回 null
     */
    public String searchIDRecycleBin(String key, int searchMethod) {
        switch (searchMethod) {
            case SEARCH_ID:
                return whiteListRecycleBin.get(key);
            case SEARCH_QQ:
                return key;
            default:
                throw new IllegalArgumentException("Invalid searchMethod!");
        }
    }

    /**
     * 初始化系统，必须在调用本类的其他方法之前先调用此方法
     *
     * @throws IOException 本地文件读取错误时抛出
     */
    public void load() throws IOException {
        if (!whiteListFile.exists()) {
            if (!whiteListFile.createNewFile()) {
                throw new IOException(
                        String.format("%s 文件创建失败!", whiteListFile.getPath())
                );
            }
        }
        if (!whiteListRecycleBinFile.exists()) {
            if (!whiteListRecycleBinFile.createNewFile()) {
                throw new IOException(
                        String.format("%s 文件创建失败!", whiteListRecycleBinFile.getPath())
                );
            }
        }

        //载入本地信息
        fullWhiteList.load();
        fullWhiteListRecycleBin.load();
        fullWhiteListMap = fullWhiteList.getWhiteList();
        fullWhiteListRecycleBinMap = fullWhiteListRecycleBin.getWhiteList();

        //复制白名单信息
        if (!fullWhiteList.getWhiteList().isEmpty()) {
            fullWhiteList.getWhiteList().forEach((id, whiteListInfo) -> whiteList.put(whiteListInfo.getUserName(), id));
        }
        if (!fullWhiteListRecycleBin.getWhiteList().isEmpty()) {
            fullWhiteListRecycleBin.getWhiteList().forEach((id, whiteListInfo) -> whiteListRecycleBin.put(whiteListInfo.getUserName(), id));
        }

        //启动自动维护线程
        automaticTask = new Thread((new MaintainTask()));
        isStarted = true;
        automaticTask.start();
    }

    /**
     * 卸载系统，调用后不能再调用其他方法，除非再次调用 load()
     *
     * @throws IOException 本地文件读取错误时抛出
     */
    public void unLoad() throws IOException {
        isStarted = false;

        logger.info("Waiting for maintainTask shutdown...");
        try {
            automaticTask.interrupt();
            automaticTask.join(3000);
        } catch (IllegalArgumentException | InterruptedException ignored) {
        }

        fullWhiteList.unload();
        fullWhiteListRecycleBin.unload();
    }

    /**
     * IO 兼维护线程，检查文件变动并写入到本地
     */
    private class MaintainTask implements Runnable {
        AtomicLong fullWhiteListLastUpdateTime = new AtomicLong(fullWhiteList.getLastUpdateTime());
        AtomicLong fullWhiteListRecycleBinLastUpdateTime = new AtomicLong(fullWhiteListRecycleBin.getLastUpdateTime());

        @Override
        public void run() {
            while (isStarted) {
                updateFile(fullWhiteListLastUpdateTime, fullWhiteList);
                updateFile(fullWhiteListRecycleBinLastUpdateTime, fullWhiteListRecycleBin);

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private void updateFile(AtomicLong lastUpdateTime, SimpleWhiteListMap whiteListMap) {
            long fullWhiteListLastUpdateTime = whiteListMap.getLastUpdateTime();
            if (lastUpdateTime.get() == fullWhiteListLastUpdateTime) return;

            try {
                lastUpdateTime.set(fullWhiteListLastUpdateTime);

                RandomAccessFile randomAccessFile = whiteListMap.getWriter();
                //清空文件内容
                randomAccessFile.setLength(0);

                //写入新内容
                randomAccessFile.write(whiteListMap.toJson().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                logger.error(String.format("Failed to write %s!\n%s",
                        whiteListMap.getFile().getName(),
                        ThrowableUtil.stackTraceToString(e)));
            }
        }
    }
}

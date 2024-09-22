package github.kasuminova.serverhelperbc.whitelist;

import github.kasuminova.network.message.whitelist.FullWhiteListInfo;
import github.kasuminova.network.message.whitelist.WhiteListUpdateResult;

public interface WhiteList {

    /**
     * 添加一个白名单信息，如果移除记录中有对应的信息，则从移除信息中还原。
     *
     * @param fullWhiteListInfo 白名单信息
     * @return 添加成功返回添加的白名单信息，用户名被其他 QQ 绑定或当前 QQ 已绑定用户名时返回错误信息和冲突的白名单信息
     */
    WhiteListUpdateResult add(FullWhiteListInfo fullWhiteListInfo);

    /**
     * 获取一个白名单，根据关键词获取
     *
     * @param key          QQ 或 用户名
     * @param searchMethod 搜索方式
     * @return 白名单信息，不存在则返回失败信息
     */
    WhiteListUpdateResult get(String key, int searchMethod);

    /**
     * 更新一个白名单信息，根据关键词获取信息
     *
     * @param oldName 旧用户名
     * @param newName 新用户名
     * @return 更新后的信息，用户名或 QQ 不存在时返回失败信息
     */
    WhiteListUpdateResult update(String oldName, String newName);

    /**
     * 更新一个白名单信息，根据关键词获取信息
     *
     * @param oldId 旧 QQ
     * @param newId 新 QQ
     * @return 更新后的信息，用户名或 QQ 不存在时返回失败信息
     */
    WhiteListUpdateResult update(String oldId, long newId);

    /**
     * 获取对应 QQ 的白名单用户名
     *
     * @param id QQ
     * @return 对应的用户名，如果无则返回 null
     */
    String getUserName(String id);

    /**
     * 移除一个白名单，根据关键词搜索并删除
     *
     * @param key          QQ 或 用户名
     * @param searchMethod 搜索方式
     * @param removeRecycleBin 是否删除回收站内的白名单而不是当前白名单
     * @return 删除成功返回 ResultCode.SUCCESS，用户名或 QQ 不存在时返回错误信息
     */
    WhiteListUpdateResult remove(String key, int searchMethod, boolean removeRecycleBin);

    /**
     * 指定的用户名是否在白名单内
     *
     * @param userName 用户名
     * @return 有则返回 true, 无则返回 false
     */
    boolean isInList(String userName);

}

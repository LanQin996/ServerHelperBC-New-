package github.kasuminova.serverhelperbc.whitelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.kasuminova.network.message.whitelist.FullWhiteListInfo;
import github.kasuminova.serverhelperbc.util.MapSerializeUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WhiteList 的一个简易封装
 */
public class SimpleWhiteListMap {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private RandomAccessFile writer;
    //ConcurrentHashMap<String QQ, WhiteListInfo>
    private ConcurrentHashMap<String, FullWhiteListInfo> whiteList;
    private long lastUpdateTime;

    /**
     * 新建一个 whitelist，必须调用 load() 后才能使用。
     *
     * @param file 本地文件
     */
    public SimpleWhiteListMap(File file) {
        this.file = file;
    }

    /**
     * 载入本地文件并初始化流，此时可以由自动维护线程写入新内容。
     *
     * @throws IOException 打开文件发生错误时抛出
     */
    public void load() throws IOException {
        whiteList = (ConcurrentHashMap<String, FullWhiteListInfo>) MapSerializeUtil.parseToMap(new FileReader(file), true);
        if (whiteList == null) whiteList = new ConcurrentHashMap<>(100);
        writer = new RandomAccessFile(file, "rws");
        lastUpdateTime = System.currentTimeMillis();
    }

    public void unload() throws IOException {
        writer.close();
    }

    public RandomAccessFile getWriter() {
        return writer;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTimeNow() {
        lastUpdateTime = System.currentTimeMillis();
    }

    public File getFile() {
        return file;
    }

    public String toJson() {
        return GSON.toJson(whiteList);
    }

    public ConcurrentHashMap<String, FullWhiteListInfo> getWhiteList() {
        return whiteList;
    }
}

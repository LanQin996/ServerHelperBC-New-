package github.kasuminova.serverhelperbc;

import github.kasuminova.serverhelperbc.command.BCCommands;
import github.kasuminova.serverhelperbc.data.ServerHelperBCConfig;
import github.kasuminova.serverhelperbc.eventlistener.EventListener;
import github.kasuminova.serverhelperbc.eventlistener.MOTDListener;
import github.kasuminova.serverhelperbc.favicon.FaviconLoader;
import github.kasuminova.serverhelperbc.hitokoto.HitokotoAPI;
import github.kasuminova.serverhelperbc.network.PluginServer;
import github.kasuminova.serverhelperbc.util.ColouredLogger;
import github.kasuminova.serverhelperbc.util.ConsoleColor;
import github.kasuminova.serverhelperbc.util.FileUtils;
import github.kasuminova.serverhelperbc.whitelist.FileWhiteList;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ThrowableUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServerHelperBC extends Plugin {
    public static final ProxyServer PROXY = ProxyServer.getInstance();
    public static final PluginServer PLUGIN_SERVER = new PluginServer();
    public static final Map<String, ChannelHandlerContext> CONNECTED_MANAGERS = new HashMap<>();
    public static final Map<String, ChannelHandlerContext> CONNECTED_SUB_SERVERS = new HashMap<>();
    public static ServerHelperBCConfig config = null;
    public static ServerHelperBC instance = null;
    public static FileWhiteList whiteList = null;
    public static ColouredLogger logger = null;
    public static FaviconLoader faviconLoader = null;
    public static ServerPing.ModInfo modInfoCache = new ServerPing.ModInfo();
    private EventListener listener;
    private MOTDListener motdListener;

    public static <M extends Serializable> void sendToAllManagers(M message) {
        for (ChannelHandlerContext ctx : CONNECTED_MANAGERS.values()) {
            ctx.writeAndFlush(message);
        }
    }

    @Override
    public void onLoad() {
        logger = new ColouredLogger(
                ConsoleColor.formatColor(ConsoleColor.BLUE, "[小云智脑] "),
                getLogger()
        );
        instance = this;

        CompletableFuture.runAsync(HitokotoAPI::getRandomHitokoto);
        PROXY.getPluginManager().registerCommand(this, new BCCommands());
    }

    @Override
    public PluginDescription getDescription() {
        PluginDescription pluginDescription = new PluginDescription();

        pluginDescription.setName("ServerHelperBC");
        pluginDescription.setMain("github.kasuminova.serverhelperbc.ServerHelperBC");
        pluginDescription.setVersion("1.1.0");
        pluginDescription.setAuthor("KasumiNova");
        pluginDescription.setDescription("");
        return pluginDescription;
    }

    @Override
    public void onEnable() {
        //初始化
        config = new ServerHelperBCConfig();
        whiteList = new FileWhiteList(getDataFolder());
        faviconLoader = new FaviconLoader(getDataFolder());

        if (!checkDataFolder()) {
            return;
        }

        try {
            loadConfig();

            whiteList.load();
            PLUGIN_SERVER.start(config.getIp(), config.getPort());

            PROXY.getPluginManager().registerListener(this, listener = new EventListener());
        } catch (IOException e) {
            logger.warn("配置文件载入失败！");
            logger.warn(ThrowableUtil.stackTraceToString(e));
        } catch (Exception e) {
            logger.error("插件服务器启动失败！");
            logger.error(ThrowableUtil.stackTraceToString(e));
            try {
                whiteList.unLoad();
            } catch (IOException ex) {
                logger.warn(ThrowableUtil.stackTraceToString(ex));
            }
        }

        try {
            File faviconFile = faviconLoader.getFaviconFile();
            if (!faviconFile.exists()) {
                FileUtils.extractJarFile("/favicon.png", faviconFile.toPath());
            }
            faviconLoader.load();
        } catch (Exception e) {
            logger.error("服务器 MOTD 图标载入失败！");
            logger.error(ThrowableUtil.stackTraceToString(e));
        }
        PROXY.getPluginManager().registerListener(this, motdListener = new MOTDListener());
    }

    public boolean checkDataFolder() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                logger.warn("插件数据文件夹创建失败！");
                logger.warn("请检查插件文件夹是否被使用，若检查无误，请输入 serverhelperbc reload 重载插件！");
                return false;
            }
        }
        return true;
    }

    public void loadConfig() throws IOException {
        logger.info("载入配置文件...");
        File dataFolder = getDataFolder();
        File configFile = new File(dataFolder + File.separator + "config.yml");
        if (!configFile.exists()) {
            if (!dataFolder.exists()) {
                if (!dataFolder.mkdirs()) {
                    throw new IOException(dataFolder.getPath() + " 创建失败！");
                }
            }
            FileUtils.extractJarFile("/config.yml", configFile.toPath());
        }

        YamlConfiguration configProvider = (YamlConfiguration) ConfigurationProvider.getProvider(YamlConfiguration.class);
        Configuration config = configProvider.load(configFile, ServerHelperBCConfig.DEFAULT_CONFIG);
        ServerHelperBC.config.loadFormConfig(config);
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            PROXY.getPluginManager().unregisterListener(listener);
            listener = null;
        }
        if (motdListener != null) {
            PROXY.getPluginManager().unregisterListener(motdListener);
            motdListener = null;
        }

        PLUGIN_SERVER.stop();

        try {
            whiteList.unLoad();
        } catch (IOException e) {
            logger.warn(String.format(
                    "尝试保存白名单数据的时候出现了问题！\n%s",
                    ThrowableUtil.stackTraceToString(e)));
        }
    }
}

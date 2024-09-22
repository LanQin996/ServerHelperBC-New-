package github.kasuminova.serverhelperbc.eventlistener;

import github.kasuminova.serverhelperbc.ServerHelperBC;
import github.kasuminova.serverhelperbc.hackery.ServerPingHackery;
import github.kasuminova.serverhelperbc.hitokoto.HitokotoAPI;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MOTDListener implements Listener {

    private final AtomicBoolean serverCountCached = new AtomicBoolean(false);
    private long lastPingedServerTime = -1;
    private int cachedOnlineServerCount = -1;
    private int pingedServerCount = -1;

    @EventHandler
    public void onProxyPing(ProxyPingEvent event) {
        PendingConnection connection = event.getConnection();
        ProxyServer proxy = ServerHelperBC.PROXY;

        ServerPing.Protocol protocol = new ServerPing.Protocol(proxy.getName(), connection.getVersion());
        int maxPlayers;

        synchronized (serverCountCached) {
            if (serverCountCached.get() && lastPingedServerTime + 10000 >= System.currentTimeMillis()) {
                maxPlayers = (cachedOnlineServerCount - 1) * ServerHelperBC.config.getSubServerPlayerLimit();
            } else {
                Map<String, ServerInfo> serversCopy = proxy.getServersCopy();
                maxPlayers = (serversCopy.size() - 1) * ServerHelperBC.config.getSubServerPlayerLimit();
                cachedOnlineServerCount = 0;
                pingServers(serversCopy);
            }
        }

        ServerPing.Players players = new ServerPing.Players(
                maxPlayers,
                proxy.getOnlineCount(), null);
        BaseComponent[] desc = TextComponent.fromLegacyText(ServerHelperBC.config.getMotd()
                .replace("\\n", "\n")
                .replace("{Hitokoto}", HitokotoAPI.getHitokotoCache())
                .replace("&", "§"));

        ServerPing response = new ServerPing(protocol, players, new TextComponent(desc), ServerHelperBC.faviconLoader.getFavicon());
        ServerPingHackery.overrideServerPingModInfo(response, ServerHelperBC.modInfoCache);
        event.setResponse(response);
    }

    private void pingServers(final Map<String, ServerInfo> serversCopy) {
        serversCopy.values().forEach(value -> value.ping((result, error) -> {
            synchronized (serverCountCached) {
                pingedServerCount++;
                if (result != null) {
                    cachedOnlineServerCount++;
                }
                if (pingedServerCount == serversCopy.size()) {
                    serverCountCached.set(true);
                    lastPingedServerTime = System.currentTimeMillis();
                }
            }
        }));
    }

}

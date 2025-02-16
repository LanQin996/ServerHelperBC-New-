package github.kasuminova.serverhelperbc.eventlistener;

import github.kasuminova.network.message.chatmessage.GameChatMessage;
import github.kasuminova.network.message.whitelist.SearchMethod;
import github.kasuminova.network.message.whitelist.UserInGroupQueryMessage;
import github.kasuminova.serverhelperbc.ServerHelperBC;
import github.kasuminova.serverhelperbc.util.ConstPool;
import github.kasuminova.serverhelperbc.util.MiscUtils;
import github.kasuminova.serverhelperbc.whitelist.FileWhiteList;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class EventListener implements Listener {
    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        String message = event.getMessage();
        String sender = event.getSender().toString();
        ProxiedPlayer player = ServerHelperBC.PROXY.getPlayer(event.getSender().toString());

        if (event.isCommand() || event.isProxyCommand()) {
            if (message.startsWith("/mv") && !ServerHelperBC.PROXY.getPlayer(sender).hasPermission("serverhelper.bypasscommand")) {
                event.setCancelled(true);
                player.sendMessage(ConstPool.DISABLED_COMMAND);
            }
        } else {
            if (!MiscUtils.isNum(message)) {
                ServerHelperBC.sendToAllManagers(new GameChatMessage(sender, message));
            }
        }
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        ServerInfo server = event.getTarget();
        ProxiedPlayer player = event.getPlayer();

        if (!server.getName().equals("lobby") && server.getPlayers().size() >= ServerHelperBC.config.getSubServerPlayerLimit()) {
            if (!player.hasPermission("serverhelper.bypassplayerlimit")) {
                if (player.getServer() == null) {
                    player.connect(ServerHelperBC.PROXY.getServerInfo("lobby"));
                } else {
                    event.setCancelled(true);
                }
                player.sendMessage(ConstPool.SERVER_PLAYER_LIMITED_MSG);
                player.sendTitle(ConstPool.SERVER_PLAYER_LIMITED_TITLE);
            } else {
                player.sendMessage(ConstPool.SERVER_PLAYER_LIMITED_JOIN_MSG);
                player.sendTitle(ConstPool.SERVER_PLAYER_LIMITED_JOIN_TITLE);
            }
        }
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        PendingConnection connection = event.getConnection();
        String userName = connection.getName();

        ServerHelperBC.logger.info(String.format("%s 尝试进入服务器，正在被检查...", userName));
        var whiteList = ServerHelperBC.whiteList;
        var id = whiteList.searchID(userName, SearchMethod.SEARCH_ID);
        if (whiteList.isInList(userName) && UserInGroupQueryMessage.query(Long.parseLong(id))) {
            ServerHelperBC.logger.info(String.format("%s 通过了白名单验证，准许进入. IP:%s",
                    userName, connection.getSocketAddress()));
        } else {
            ServerHelperBC.logger.info(String.format("%s 不在服务器白名单中，断开连接. IP:%s", userName, connection.getSocketAddress()));
            event.setCancelReason(new ComponentBuilder()
                    .color(ChatColor.RED)
                    .append("[新星工程防御系统] 你不在服务器的白名单中，或在任意的本服服务器群中，请加入 QQ 群 471614563 并在")
                    .bold(true).append("群内").reset()
                    .color(ChatColor.RED).append("发送").append("\n")
                    .color(ChatColor.YELLOW).append("#申请白名单 ").append(userName).append("\n")
                    .color(ChatColor.AQUA).append("若输入有误或出现故障等问题，请私聊服主 QQ：2755271615")
                    .create()
            );
            event.setCancelled(true);
        }
    }
}

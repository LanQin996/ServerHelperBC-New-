package github.kasuminova.serverhelperbc.network.handler;

import github.kasuminova.network.message.chatmessage.GroupChatMessage;
import github.kasuminova.network.message.servercmd.CmdExecFailedMessage;
import github.kasuminova.network.message.servercmd.CmdExecMessage;
import github.kasuminova.network.message.serverinfo.OnlineGetMessage;
import github.kasuminova.network.message.serverinfo.OnlinePlayerListMessage;
import github.kasuminova.network.message.whitelist.*;
import github.kasuminova.serverhelperbc.ServerHelperBC;
import io.netty.channel.ChannelHandlerContext;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ManagerHandler extends AbstractHandler<ManagerHandler> {
    private final String clientId;

    public ManagerHandler(String clientId) {
        this.clientId = clientId;
    }

    @Override
    protected void onRegisterMessages() {
        registerMessage(GroupChatMessage.class, ManagerHandler::sendGameChatMessage);

        registerMessage(WhiteListAddMessage.class, ManagerHandler::addWhiteList);
        registerMessage(WhiteListGetMessage.class, ManagerHandler::getWhiteList);
        registerMessage(WhiteListRemoveMessage.class, ManagerHandler::removeWhiteList);
        registerMessage(WhiteListUpdateMessage.class, ManagerHandler::updateWhiteList);

        registerMessage(OnlineGetMessage.class, ManagerHandler::getOnlinePlayers);

        registerMessage(CmdExecMessage.class, ManagerHandler::executeCommand);
    }

    private static void executeCommand(ManagerHandler handler, CmdExecMessage message) {
        ChannelHandlerContext ctx = ServerHelperBC.CONNECTED_SUB_SERVERS.get(message.serverName);
        if (ctx == null) {
            handler.ctx.writeAndFlush(
                    new CmdExecFailedMessage(message.sender, message.serverName, "未找到目标服务器：" + message.serverName));
        } else {
            ctx.writeAndFlush(message);
        }
    }

    private static void sendGameChatMessage(ManagerHandler handler, GroupChatMessage message) {
        String userName = ServerHelperBC.whiteList.getUserName(String.valueOf(message.id));
        if (userName != null) {
            //示例：『群聊消息』Kasumi_Nova: 哼哼，啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊
            BaseComponent[] chatMessage = new ComponentBuilder()
                    .append("『群聊消息』").color(ChatColor.GOLD)
                    .append(userName).color(ChatColor.AQUA)
                    .append(": ").append(message.message).color(ChatColor.GRAY)
                    .create();

            ServerHelperBC.PROXY.broadcast(chatMessage);
        }
    }

    private static void addWhiteList(ManagerHandler handler, WhiteListAddMessage message) {
        ServerHelperBC.logger.info("Received WhiteListAddMessage");
        handler.ctx.writeAndFlush(ServerHelperBC.whiteList.add(message.fullWhiteListInfo));
    }

    private static void getWhiteList(ManagerHandler handler, WhiteListGetMessage message) {
        ServerHelperBC.logger.info("Received WhiteListGetMessage");
        handler.ctx.writeAndFlush(ServerHelperBC.whiteList.get(message.key, message.searchMethod));
    }

    private static void removeWhiteList(ManagerHandler handler, WhiteListRemoveMessage message) {
        ServerHelperBC.logger.info("Received WhiteListRemoveMessage");
        handler.ctx.writeAndFlush(ServerHelperBC.whiteList.remove(message.key, message.searchMethod, message.removeRecycleBin));
    }

    private static void updateWhiteList(ManagerHandler handler, WhiteListUpdateMessage message) {
        ServerHelperBC.logger.info("Received WhiteListUpdateMessage");
        switch (message.searchMethod) {
            case SearchMethod.SEARCH_USERNAME:
                handler.ctx.writeAndFlush(ServerHelperBC.whiteList.update(
                        message.oldName, message.newName));
                break;

            case SearchMethod.SEARCH_QQ:
                handler.ctx.writeAndFlush(ServerHelperBC.whiteList.update(
                        String.valueOf(message.oldId), message.newId));
        }
    }

    private static void getOnlinePlayers(ManagerHandler handler, OnlineGetMessage message) {
        if (message.getPlayerList) {
            Collection<ProxiedPlayer> online = ServerHelperBC.PROXY.getPlayers();
            List<String> players = new ArrayList<>();
            for (ProxiedPlayer proxiedPlayer : online) {
                Server server = proxiedPlayer.getServer();
                if (server != null) {
                    players.add(proxiedPlayer.getDisplayName() + " - " + server.getInfo().getName());
                } else {
                    players.add(proxiedPlayer.getDisplayName());
                }
            }
            handler.ctx.writeAndFlush(new OnlinePlayerListMessage(players.size(), players.toArray(new String[0])));
        } else {
            handler.ctx.writeAndFlush(new OnlinePlayerListMessage(ServerHelperBC.PROXY.getOnlineCount()));
        }
    }

    @Override
    protected void channelActive0() {
        ServerHelperBC.CONNECTED_MANAGERS.put(clientId, ctx);
        ServerHelperBC.logger.info(
                "管理端：IP:/" + clientIP + ", ID：" + clientId + " 已连接至服务器。");
    }

    @Override
    protected void channelInactive0() {
        ServerHelperBC.CONNECTED_MANAGERS.remove(clientId);
        ServerHelperBC.logger.info(
                "管理端：IP:/" + clientIP + ", ID：" + clientId + " 已断开连接。");
    }
}

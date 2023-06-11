package github.kasuminova.serverhelperbc.network.handler;

import github.kasuminova.network.message.chatmessage.GroupChatMessage;
import github.kasuminova.network.message.playercmd.KickMeMessage;
import github.kasuminova.network.message.playercmd.PlayerCmdExecFailedMessage;
import github.kasuminova.network.message.playercmd.PlayerCmdExecMessage;
import github.kasuminova.network.message.protocol.HeartbeatMessage;
import github.kasuminova.network.message.protocol.HeartbeatResponse;
import github.kasuminova.network.message.servercmd.CmdExecFailedMessage;
import github.kasuminova.network.message.servercmd.CmdExecMessage;
import github.kasuminova.network.message.servercmd.CmdExecResultsMessage;
import github.kasuminova.network.message.servercmd.GlobalCmdExecMessage;
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
        registerMessage(GlobalCmdExecMessage.class, ManagerHandler::executeGlobalCommand);
        registerMessage(PlayerCmdExecMessage.class, ManagerHandler::executePlayerCommand);
        registerMessage(KickMeMessage.class, ManagerHandler::kickPlayer);

        registerMessage(HeartbeatMessage.class, (handler, message) -> heartbeatResponse());
    }

    private static void kickPlayer(ManagerHandler handler, KickMeMessage message) {
        String userName = ServerHelperBC.whiteList.getUserName(message.id);
        if (userName == null) {
            handler.ctx.writeAndFlush(new CmdExecResultsMessage("BC", message.id, new String[]{"执行失败：未找到 QQ 所绑定的白名单。"}));
            return;
        }

        ProxiedPlayer player = ServerHelperBC.PROXY.getPlayer(userName);
        if (player == null) {
            handler.ctx.writeAndFlush(new CmdExecResultsMessage("BC", userName, new String[]{"执行失败：玩家不在线。"}));
            return;
        }

        player.disconnect(new ComponentBuilder().color(ChatColor.RED).append("你被踹出服务器力！").create());

        handler.ctx.writeAndFlush(new CmdExecResultsMessage("BC", message.id, new String[]{userName + " 被踹出服务器力！"}));
        ServerHelperBC.PROXY.broadcast(new ComponentBuilder()
                .append(userName + " 被踹出服务器力！").color(ChatColor.GOLD)
                .create());
    }

    private static void executeGlobalCommand(ManagerHandler handler, GlobalCmdExecMessage message) {
        ServerHelperBC.CONNECTED_SUB_SERVERS.forEach((serverName, ctx) ->
                ctx.writeAndFlush(new CmdExecMessage(serverName, message.sender, message.cmd)));
    }

    private static void executePlayerCommand(ManagerHandler handler, PlayerCmdExecMessage message) {
        String playerName = ServerHelperBC.whiteList.getUserName(message.playerName);
        if (playerName == null) {
            handler.ctx.writeAndFlush(
                    new PlayerCmdExecFailedMessage(message.playerName, message.serverName, message.sender, "执行错误：找不到此 QQ 绑定的白名单。"));
            return;
        }

        ProxiedPlayer player = ServerHelperBC.PROXY.getPlayer(playerName);
        if (player == null || player.getServer() == null) {
            handler.ctx.writeAndFlush(
                    new PlayerCmdExecFailedMessage(message.playerName, message.serverName, message.sender, "执行错误：" + playerName + " 不在线。"));
            return;
        }

        Server server = player.getServer();
        String serverName = server.getInfo().getName().toUpperCase();
        ChannelHandlerContext ctx = ServerHelperBC.CONNECTED_SUB_SERVERS.get(serverName);
        if (ctx == null) {
            handler.ctx.writeAndFlush(
                    new PlayerCmdExecFailedMessage(message.playerName, message.serverName, message.sender, "执行错误：未找到目标服务器：" + serverName));
        } else {
            message.serverName = serverName;
            message.playerName = playerName;
            ctx.writeAndFlush(message);
        }
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
            case SearchMethod.SEARCH_ID:
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

    private void heartbeatResponse() {
        ctx.writeAndFlush(new HeartbeatResponse());
    }


}

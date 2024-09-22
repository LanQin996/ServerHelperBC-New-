package github.kasuminova.serverhelperbc.network.handler;

import github.kasuminova.network.message.playercmd.PlayerCmdExecFailedMessage;
import github.kasuminova.network.message.protocol.HeartbeatMessage;
import github.kasuminova.network.message.protocol.HeartbeatResponse;
import github.kasuminova.network.message.servercmd.CmdExecFailedMessage;
import github.kasuminova.network.message.servercmd.CmdExecResultsMessage;
import github.kasuminova.network.message.serverinfo.ModListMessage;
import github.kasuminova.serverhelperbc.ServerHelperBC;
import io.netty.channel.ChannelHandlerContext;
import net.md_5.bungee.api.ServerPing;

public class SubServerHandler extends AbstractHandler<SubServerHandler> {
    private final String clientId;

    public SubServerHandler(String clientId) {
        this.clientId = clientId;
    }

    @Override
    protected void onRegisterMessages() {
        registerMessage(CmdExecFailedMessage.class, SubServerHandler::forwardCmdExecFailedMessage);
        registerMessage(CmdExecResultsMessage.class, SubServerHandler::forwardCmdExecResultMessage);
        registerMessage(PlayerCmdExecFailedMessage.class, SubServerHandler::forwardPlayerCmdExecFailedMessage);
        registerMessage(ModListMessage.class, SubServerHandler::dispatchForgeModList);

        registerMessage(HeartbeatMessage.class, (handler, message) -> heartbeatResponse());
    }

    private static void forwardCmdExecResultMessage(SubServerHandler handler, CmdExecResultsMessage message) {
        ChannelHandlerContext ctx = ServerHelperBC.CONNECTED_MANAGERS.get(message.sender);
        if (ctx == null) {
            ServerHelperBC.logger.warn(
                    "接收到向名为 " + message.sender + " 的管理端转发数据包，但是当前并未找到对应名称的管理端，已丢弃数据包。");
        } else {
            ctx.writeAndFlush(message);
        }
    }

    private static void forwardCmdExecFailedMessage(SubServerHandler handler, CmdExecFailedMessage message) {
        ChannelHandlerContext ctx = ServerHelperBC.CONNECTED_MANAGERS.get(message.sender);
        if (ctx == null) {
            ServerHelperBC.logger.warn(
                    "接收到向名为 " + message.sender + " 的管理端转发数据包，但是当前并未找到对应名称的管理端，已丢弃数据包。");
        } else {
            ctx.writeAndFlush(message);
        }
    }

    private static void forwardPlayerCmdExecFailedMessage(SubServerHandler handler, PlayerCmdExecFailedMessage message) {
        ChannelHandlerContext ctx = ServerHelperBC.CONNECTED_MANAGERS.get(message.serverName);
        if (ctx == null) {
            ServerHelperBC.logger.warn(
                    "接收到向名为 " + message.sender + " 的管理端转发数据包，但是当前并未找到对应名称的管理端，已丢弃数据包。");
        } else {
            ctx.writeAndFlush(message);
        }
    }

    private static void dispatchForgeModList(SubServerHandler handler, ModListMessage message) {
        ServerPing.ModInfo modInfo = new ServerPing.ModInfo();
        modInfo.setModList(message.getModList().stream()
                .map(item -> new ServerPing.ModItem(item.getModID(), item.getVersion()))
                .toList());
        ServerHelperBC.modInfoCache = modInfo;
    }

    @Override
    protected void channelActive0() {
        ServerHelperBC.CONNECTED_SUB_SERVERS.put(clientId, ctx);
        ServerHelperBC.logger.info("子服：IP:/" + clientIP + ", 名称：" + clientId + " 已连接至服务器。");
    }

    @Override
    protected void channelInactive0() {
        ServerHelperBC.CONNECTED_SUB_SERVERS.remove(clientId);
        ServerHelperBC.logger.info("子服：IP:/" + clientIP + ", 名称：" + clientId + " 已断开连接。");
    }

    private void heartbeatResponse() {
        ctx.writeAndFlush(new HeartbeatResponse());
    }
}

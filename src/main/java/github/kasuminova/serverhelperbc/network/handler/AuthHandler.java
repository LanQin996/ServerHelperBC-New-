package github.kasuminova.serverhelperbc.network.handler;

import github.kasuminova.network.message.protocol.ClientType;
import github.kasuminova.network.message.protocol.ClientTypeMessage;
import github.kasuminova.network.message.protocol.PreDisconnectMessage;
import github.kasuminova.network.message.serverinfo.ModListGetMessage;
import github.kasuminova.serverhelperbc.ServerHelperBC;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.util.internal.ThrowableUtil;

public class AuthHandler extends AbstractHandler<AuthHandler> {
    private static <T extends ChannelInboundHandler> void addHandler(ChannelHandlerContext ctx, T instance) {
        ctx.pipeline().addLast(instance.getClass().getTypeName(), instance);
        try {
            instance.channelRegistered(ctx);
            instance.channelActive(ctx);
        } catch (Exception e) {
            ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
        }
    }

    private static <T extends ChannelInboundHandler> void removeHandler(ChannelHandlerContext ctx, Class<T> tClass) {
        ctx.pipeline().remove(tClass.getTypeName());
    }

    private static void auth(AuthHandler handler, ClientTypeMessage message) {
        ClientType clientType;
        try {
            clientType = ClientType.valueOf(message.type.toUpperCase());
        } catch (IllegalArgumentException e) {
            handler.ctx.writeAndFlush(new PreDisconnectMessage(String.format("Invalid Client Type %s!", message.type.toUpperCase())));
            return;
        }

        ServerHelperBC.logger.info("Detected ClientType " + clientType);
        removeHandler(handler.ctx, AuthHandler.class);

        switch (clientType) {
            case MANAGER:
                authManager(handler, message);
                break;
            case SUB_SERVER:
                authSubServer(handler, message);
                break;
        }
    }

    private static void authManager(AuthHandler handler, ClientTypeMessage message) {
        if (message.accessToken.equals(ServerHelperBC.config.getManagerAccessToken())) {
            ChannelHandlerContext connected = ServerHelperBC.CONNECTED_MANAGERS.get(message.clientId);
            if (connected != null) {
                connected.writeAndFlush(new PreDisconnectMessage("侦测到相同 ID 的管理端连接，即将断开旧连接。"));
                connected.close();
            }

            ManagerHandler managerHandler = new ManagerHandler(message.clientId);
            addHandler(handler.ctx, managerHandler);
        } else {
            handler.ctx.writeAndFlush(new PreDisconnectMessage("Token 错误。"));
            handler.ctx.close();
        }
    }

    private static void authSubServer(AuthHandler handler, ClientTypeMessage message) {
        if (message.accessToken.equals(ServerHelperBC.config.getSubServerAccessToken())) {
            if (ServerHelperBC.CONNECTED_SUB_SERVERS.containsKey(message.clientId)) {
                handler.ctx.writeAndFlush(new PreDisconnectMessage("中心服务器已经有一个相同名称的子服了！"));
                handler.ctx.close();
            } else {
                addHandler(handler.ctx, new SubServerHandler(message.clientId));
                if (message.clientId.equalsIgnoreCase("lobby")) {
                    handler.ctx.writeAndFlush(new ModListGetMessage());
                }
            }
        } else {
            handler.ctx.writeAndFlush(new PreDisconnectMessage("Token 错误。"));
            handler.ctx.close();
        }
    }

    @Override
    protected void onRegisterMessages() {
        registerMessage(ClientTypeMessage.class, AuthHandler::auth);
    }
}

package github.kasuminova.serverhelperbc.network;

import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;

public class ClientInfo {
    private final ChannelHandlerContext ctx;
    private int ping = 0;
    public ClientInfo(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public <M extends Serializable> void sendMessage(M message) {
        ctx.writeAndFlush(message);
    }

    public void sendHeartbeatMessage() {

    }
}

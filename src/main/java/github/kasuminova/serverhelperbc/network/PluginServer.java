package github.kasuminova.serverhelperbc.network;

import github.kasuminova.serverhelperbc.ServerHelperBC;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class PluginServer {
    EventLoopGroup boss;
    EventLoopGroup work;
    ChannelFuture future;

    public void start(String ip, int port) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        boss = new NioEventLoopGroup(4);
        work = new NioEventLoopGroup(2);

        bootstrap.group(boss, work)
                .channel(NioServerSocketChannel.class)
                .childHandler(new PluginServerInitializer());

        future = bootstrap.bind(new InetSocketAddress(ip, port)).sync();
        ServerHelperBC.logger.info(String.format("插件服务端已启动. IP: %s 端口: %s", ip, port));
    }

    public void stop() {
        work.shutdownGracefully();
        boss.shutdownGracefully();

        if (future != null) {
            try {
                future.channel().closeFuture().sync();
            } catch (Exception ignored) {
            }
        }
    }
}

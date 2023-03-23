package github.kasuminova.serverhelperbc.network;

import github.kasuminova.network.codec.CompressedObjectDecoder;
import github.kasuminova.network.codec.CompressedObjectEncoder;
import github.kasuminova.serverhelperbc.ServerHelperBC;
import github.kasuminova.serverhelperbc.network.handler.AuthHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class PluginServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast("CompressedObjectEncoder", new CompressedObjectEncoder());
        pipeline.addLast("CompressedObjectDecoder", new CompressedObjectDecoder(ServerHelperBC.class.getClassLoader()));
//        pipeline.addLast(new ObjectEncoder());
//        pipeline.addLast(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.weakCachingResolver(ServerHelperBC.class.getClassLoader())));

        pipeline.addLast(AuthHandler.class.getTypeName(), new AuthHandler());
    }
}

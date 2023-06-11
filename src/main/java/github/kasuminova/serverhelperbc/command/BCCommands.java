package github.kasuminova.serverhelperbc.command;

import github.kasuminova.network.message.servercmd.CmdExecMessage;
import github.kasuminova.serverhelperbc.ServerHelperBC;
import io.netty.channel.ChannelHandlerContext;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class BCCommands extends Command {
    public BCCommands() {
        super("serverhelperbc");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new ComponentBuilder().color(ChatColor.RED).append("用法：serverhelperbc reload | runCs | runGlobalCs").create());
            return;
        }

        String arg = args[0];

        switch (arg) {
            case "reload":
                ServerHelperBC.instance.onDisable();
                ServerHelperBC.instance.onEnable();
                sender.sendMessage(new ComponentBuilder().color(ChatColor.GREEN)
                        .append("ServerHelperBC 重载完成。").create());
                break;

            case "runCs":
                if (args.length <= 2) {
                    sender.sendMessage(new ComponentBuilder().color(ChatColor.RED)
                            .append("用法：serverhelperbc runCs <serverName> <command>").create());
                    break;
                }

                String command = mergeArgs(args, 2);
                ChannelHandlerContext ctx = ServerHelperBC.CONNECTED_SUB_SERVERS.get(args[1]);
                if (ctx == null) {
                    sender.sendMessage(new ComponentBuilder().color(ChatColor.RED)
                            .append("执行错误：未找到目标服务器。").create());
                } else {
                    ctx.writeAndFlush(new CmdExecMessage("", "", command));
                }
                break;

            case "runGlobalCs":
                if (args.length <= 2) {
                    sender.sendMessage(new ComponentBuilder().color(ChatColor.RED)
                            .append("用法：serverhelperbc runGlobalCs <command>").create());
                    break;
                }

                String command1 = mergeArgs(args, 1);
                for (final ChannelHandlerContext ctx1 : ServerHelperBC.CONNECTED_SUB_SERVERS.values()) {
                    ctx1.writeAndFlush(new CmdExecMessage("", "", command1));
                }
                break;
        }
    }

    private static String mergeArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];
            sb.append(arg);
            if (i + 1 < args.length) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}

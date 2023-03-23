package github.kasuminova.serverhelperbc.command;

import github.kasuminova.serverhelperbc.ServerHelperBC;
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
            sender.sendMessage(new ComponentBuilder().color(ChatColor.RED).append("用法：serverhelperbc reload").create());
            return;
        }

        String arg = args[0];

        if (arg.equals("reload")) {
            ServerHelperBC.instance.onDisable();
            ServerHelperBC.instance.onEnable();
            sender.sendMessage(new ComponentBuilder().color(ChatColor.GREEN).append("ServerHelperBC 重载完成。").create());
        }
    }
}

package github.kasuminova.serverhelperbc.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class ConstPool {
    public static final BaseComponent[] DISABLED_COMMAND = new ComponentBuilder()
            .color(ChatColor.RED)
            .append("命令已经被取消.因为你输入了服务器禁用的命令！")
            .create();

    public static final BaseComponent[] SERVER_PLAYER_LIMITED_MSG = new ComponentBuilder()
            .color(ChatColor.RED).bold(true)
            .append("此区已经达到人数上限 15 人，请稍后进入！")
            .append("\n")
            .color(ChatColor.AQUA).bold(true)
            .append("开通 行星/恒星/超新星/宸 VIP 可以绕过人数限制，请打开 VIP 菜单查看~")
            .create();

    public static final BaseComponent[] SERVER_PLAYER_LIMITED_JOIN_MSG = new ComponentBuilder()
            .color(ChatColor.AQUA).bold(true)
            .append("尊贵的 VIP 玩家，已经为您绕过人数限制！")
            .append("\n")
            .color(ChatColor.GREEN).bold(true)
            .append("感谢支持服务器~")
            .create();

    public static final Title SERVER_PLAYER_LIMITED_TITLE = ProxyServer.getInstance().createTitle()
            .title(new ComponentBuilder()
                    .color(ChatColor.RED).bold(true)
                    .append("此区已经达到人数上限 15 人，请稍后进入！")
                    .create())
            .subTitle(new ComponentBuilder()
                    .color(ChatColor.AQUA).bold(true)
                    .append("开通 行星/恒星/超新星/宸 VIP 可以绕过人数限制，请打开 VIP 菜单查看~")
                    .create())
            .fadeIn(15).stay(15).fadeOut(15);

    public static final Title SERVER_PLAYER_LIMITED_JOIN_TITLE = ProxyServer.getInstance().createTitle()
            .title(new ComponentBuilder()
                    .color(ChatColor.AQUA).bold(true)
                    .append("尊贵的 VIP 玩家，已经为您绕过人数限制！")
                    .create())
            .subTitle(new ComponentBuilder()
                    .color(ChatColor.GREEN).bold(true)
                    .append("感谢支持服务器~")
                    .create())
            .fadeIn(15).stay(15).fadeOut(15);

}

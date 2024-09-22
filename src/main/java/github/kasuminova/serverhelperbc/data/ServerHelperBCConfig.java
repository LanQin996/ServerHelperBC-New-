package github.kasuminova.serverhelperbc.data;

import net.md_5.bungee.config.Configuration;

public class ServerHelperBCConfig {
    public static final Configuration DEFAULT_CONFIG = new Configuration();

    public ServerHelperBCConfig() {
        DEFAULT_CONFIG.set("ServerHelperBC.Port", 20000);
        DEFAULT_CONFIG.set("ServerHelperBC.SubServerAccessToken", "123abc");
        DEFAULT_CONFIG.set("ServerHelperBC.ManagerAccessToken", "abc123");
        DEFAULT_CONFIG.set("ServerHelperBC.SubServerPlayerLimit", 20);
        DEFAULT_CONFIG.set("ServerHelperBC.Motd", "#50eaff-ff66ff-ff6633NovaEngineering - World —— 欢迎来到 新星工程：世界！\n&7&o{Hitokoto}");
    }

    private String ip = "0.0.0.0";
    private int port = 20000;
    private String subServerAccessToken = "123abc";
    private String managerAccessToken = "abc123";
    private int subServerPlayerLimit = 20;
    private String motd = "";

    public void loadFormConfig(Configuration config) {
        ip = config.getString("ServerHelperBC.IP", "0.0.0.0");
        port = config.getInt("ServerHelperBC.Port", 20000);
        subServerAccessToken = config.getString("ServerHelperBC.SubServerAccessToken", "123abc");
        managerAccessToken = config.getString("ServerHelperBC.ManagerAccessToken", "abc123");
        subServerPlayerLimit = config.getInt("ServerHelperBC.SubServerPlayerLimit", 20);
        motd = config.getString("ServerHelperBC.Motd", "#50eaff-ff66ff-ff6633NovaEngineering - World —— 欢迎来到 新星工程：世界！\n&7&o{Hitokoto}");
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSubServerAccessToken() {
        return subServerAccessToken;
    }

    public void setSubServerAccessToken(String subServerAccessToken) {
        this.subServerAccessToken = subServerAccessToken;
    }

    public String getManagerAccessToken() {
        return managerAccessToken;
    }

    public void setManagerAccessToken(String managerAccessToken) {
        this.managerAccessToken = managerAccessToken;
    }

    public int getSubServerPlayerLimit() {
        return subServerPlayerLimit;
    }

    public void setSubServerPlayerLimit(final int subServerPlayerLimit) {
        this.subServerPlayerLimit = subServerPlayerLimit;
    }

    public String getMotd() {
        return motd;
    }

    public void setMotd(final String motd) {
        this.motd = motd;
    }

}

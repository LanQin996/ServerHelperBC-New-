package github.kasuminova.serverhelperbc.data;

import net.md_5.bungee.config.Configuration;

public class ServerHelperBCConfig {
    public static final Configuration DEFAULT_CONFIG = new Configuration();

    public ServerHelperBCConfig() {
        DEFAULT_CONFIG.set("ServerHelperBC.Port", 20000);
        DEFAULT_CONFIG.set("ServerHelperBC.SubServerAccessToken", "123abc");
        DEFAULT_CONFIG.set("ServerHelperBC.ManagerAccessToken", "abc123");
    }

    private String ip = "0.0.0.0";
    private int port = 20000;
    private String subServerAccessToken = "123abc";
    private String managerAccessToken = "abc123";

    public void loadFormConfig(Configuration configuration) {
        ip = configuration.getString("ServerHelperBC.IP", "0.0.0.0");
        port = configuration.getInt("ServerHelperBC.Port", 20000);
        subServerAccessToken = configuration.getString("ServerHelperBC.SubServerAccessToken", "123abc");
        managerAccessToken = configuration.getString("ServerHelperBC.ManagerAccessToken", "abc123");
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
}

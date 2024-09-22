package github.kasuminova.serverhelperbc.hackery;

import github.kasuminova.serverhelperbc.ServerHelperBC;
import io.netty.util.internal.ThrowableUtil;
import net.md_5.bungee.api.ServerPing;

import java.lang.reflect.Field;

public class ServerPingHackery {
    
    private static Field modinfo;
    
    static {
        Field modinfoField = null;
        try {
            modinfoField = ServerPing.class.getDeclaredField("modinfo");
            modinfoField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            ServerHelperBC.logger.warn("Failed to find modinfo field in ServerPing.class.");
            ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
        }
        ServerPingHackery.modinfo = modinfoField;
    }

    public static void overrideServerPingModInfo(ServerPing ping, ServerPing.ModInfo info) {
        if (modinfo == null) {
            return;
        }
        try {
            modinfo.set(ping, info);
        } catch (IllegalAccessException e) {
        }
    }

}

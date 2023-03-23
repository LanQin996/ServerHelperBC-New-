package github.kasuminova.serverhelperbc.util;

public class ConsoleColor {
    public static final String RED      = "\033[91m";
    public static final String GREEN    = "\033[92m";
    public static final String ORANGE   = "\033[93m";
    public static final String BLUE     = "\033[94m";
    public static final String PURPLE   = "\033[95m";
    public static final String AQUA     = "\033[96m";
    public static final String END      = "\033[m";
    public static String formatColor(String color, String str) {
        return String.format("%s%s%s", color, str, END);
    }

    public static String formatColor(String prefix, String color, String str) {
        return String.format("%s%s%s%s", prefix, color, str, END);
    }
}

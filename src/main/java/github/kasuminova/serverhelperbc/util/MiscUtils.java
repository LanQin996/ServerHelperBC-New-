package github.kasuminova.serverhelperbc.util;

public class MiscUtils {
    /**
     * 字符串是否都为数字
     * @param str 字符串
     * @return 当全为数字时返回 true，否则返回 false
     */
    public static Boolean isNum(String str) {
        return str.matches("[0-9]+");
    }
}

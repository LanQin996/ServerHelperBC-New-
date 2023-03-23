package github.kasuminova.serverhelperbc.util;

import io.netty.util.internal.ThrowableUtil;

import java.util.logging.Logger;

public class ColouredLogger {
    private final Logger logger;
    private final String prefix;

    /**
     * 创建一个带有颜色输出的 Logger
     */
    public ColouredLogger(String prefix, Logger logger) {
        this.prefix = prefix;
        this.logger = logger;
    }

    /**
     * 输出 INFO 日志
     *
     * @param msg 消息
     */
    public void info(String msg) {
        logger.info(ConsoleColor.formatColor(prefix, ConsoleColor.GREEN, msg));
    }

//    /**
//     * 输出 INFO 日志
//     *
//     * @param format    消息
//     * @param params    占位符
//     */
//    public void info(String format, Object... params) {
//        logger.info(format, params);
//    }

    /**
     * 输出 WARN 日志
     *
     * @param msg 消息
     */
    public void warn(String msg) {
        logger.warning(ConsoleColor.formatColor(prefix, ConsoleColor.ORANGE, msg));
    }

//    public void warn(String format, Object... params) {
//        logger.warning(format, params);
//    }

    /**
     * 输出 ERROR 日志
     *
     * @param msg 消息
     */
    public void error(String msg) {
        logger.warning(ConsoleColor.formatColor(prefix, ConsoleColor.RED, msg));
    }

//    public void error(String format, Object... params) {
//        logger.warning(format, params);
//    }

    /**
     * 输出 ERROR 日志
     *
     * @param msg 消息
     * @param e   错误信息
     */
    public void error(String msg, Throwable e) {
        String stackTrace = ThrowableUtil.stackTraceToString(e);
        logger.warning(ConsoleColor.formatColor(prefix, ConsoleColor.RED, msg));
        logger.warning(ConsoleColor.formatColor(ConsoleColor.RED, stackTrace));
    }

    /**
     * 输出 ERROR 日志
     *
     * @param e 错误信息
     */
    public void error(Throwable e) {
        String stackTrace = ThrowableUtil.stackTraceToString(e);
        logger.warning(ConsoleColor.formatColor(prefix, ConsoleColor.RED, stackTrace));
    }
}

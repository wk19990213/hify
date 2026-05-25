package com.hify.common.util;

/**
 * 日志净化工具 — 防止日志伪造攻击。
 * 将用户提供的输入写入日志前调用，去除换行和控制字符。
 */
public final class LogSanitizer {

    private static final int MAX_LENGTH = 500;

    private LogSanitizer() {
    }

    /**
     * 净化日志输入：去除换行、控制字符，截断过长内容
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // 替换换行、制表符为转义形式，移除所有其他控制字符
        String result = input
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        if (result.length() > MAX_LENGTH) {
            result = result.substring(0, MAX_LENGTH - 3) + "...";
        }
        return result;
    }
}

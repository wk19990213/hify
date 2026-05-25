package com.hify.common.util;

import java.util.Set;

/**
 * URL 安全验证器 - 防止 SSRF 攻击
 */
public class UrlSecurityValidator {

    // 内网 IP 段前缀
    private static final Set<String> BLOCKED_IP_PREFIXES = Set.of(
            "127.", "10.", "192.168.", "169.254.", "0.", "255."
    );

    // 172.16.0.0/12 私有地址段
    private static final int[] BLOCKED_172_RANGES = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    // 允许 HTTP 连接的本地地址（开发/Ollama 场景）
    private static final Set<String> LOCAL_HTTP_ALLOWED = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

    /**
     * 验证 URL 是否安全（防止 SSRF）
     * @param url 待验证的 URL
     * @return true 如果 URL 安全
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        // 1. 禁止 URL 中包含用户认证信息
        if (url.contains("@")) {
            return false;
        }

        // 2. 禁止 URL 中包含 fragment
        if (url.contains("#")) {
            return false;
        }

        // 3. 提取 host
        String host = extractHost(url);
        if (host == null) {
            return false;
        }

        boolean isHttps = url.startsWith("https://");
        boolean isLocalhost = LOCAL_HTTP_ALLOWED.contains(host);

        // 4. HTTP 仅允许本地回环地址（Ollama 等本地服务）
        if (!isHttps && !isLocalhost) {
            return false;
        }

        // 5. HTTPS 时仍需检查内网 IP
        if (isHttps && isInternalIp(host)) {
            return false;
        }

        // 6. HTTPS + localhost 拒绝（用 HTTP 即可）
        if (isHttps && host.equals("localhost")) {
            return false;
        }

        return true;
    }

    /**
     * 验证 URL 是否安全，如果不安全则抛出异常
     * @param url 待验证的 URL
     * @param fieldName 字段名（用于错误信息）
     */
    public static void validateUrl(String url, String fieldName) {
        if (!isValidUrl(url)) {
            throw new IllegalArgumentException(
                    fieldName + " 包含不安全的 URL，仅允许 HTTPS 且不能是内网地址: " + url);
        }
    }

    private static String extractHost(String url) {
        try {
            int start = url.indexOf("://") + 3;
            if (start < 3) return null;

            int end = url.indexOf('/', start);
            if (end == -1) end = url.indexOf('?', start);
            if (end == -1) end = url.indexOf('#', start);
            if (end == -1) end = url.length();

            String host = url.substring(start, end);
            // IPv6 bracket notation: [::1]:8080 → strip port after closing bracket
            if (host.startsWith("[")) {
                int bracketEnd = host.indexOf(']');
                if (bracketEnd == -1) return null;
                // check for port after ]
                int portIdx = host.indexOf(':', bracketEnd);
                if (portIdx != -1) host = host.substring(0, portIdx);
                else host = host.substring(0, bracketEnd + 1);
            } else {
                int portIndex = host.indexOf(':');
                if (portIndex != -1) {
                    host = host.substring(0, portIndex);
                }
            }
            return host.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isInternalIp(String host) {
        // IPv6 环回地址
        String lower = host.toLowerCase();
        if (lower.equals("[::1]") || lower.equals("::1")
                || lower.equals("[0:0:0:0:0:0:0:1]") || lower.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }

        // 检查常见内网前缀
        for (String prefix : BLOCKED_IP_PREFIXES) {
            if (host.startsWith(prefix)) {
                return true;
            }
        }

        // 检查 172.16.0.0/12 段
        if (host.startsWith("172.")) {
            try {
                String[] parts = host.split("\\.");
                if (parts.length >= 2) {
                    int secondOctet = Integer.parseInt(parts[1]);
                    for (int rangeStart : BLOCKED_172_RANGES) {
                        if (secondOctet == rangeStart) {
                            return true;
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // 不是 IP，跳过
            }
        }

        return false;
    }
}

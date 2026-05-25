package com.hify.provider.adapter;

import com.hify.common.util.UrlSecurityValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSRF 防护测试 - P1-3
 */
class UrlSecurityValidatorTest {

    @Test
    void testInternalIp_Blocked() {
        String[] internalUrls = {
                "http://192.168.1.1",
                "http://10.0.0.1",
                "http://172.16.0.1",
                "http://169.254.1.1",
                "http://0.0.0.0",
                "https://192.168.1.1",
                "https://10.0.0.1",
                "https://172.16.0.1",
        };

        for (String url : internalUrls) {
            assertFalse(UrlSecurityValidator.isValidUrl(url),
                    "Internal URL should be blocked: " + url);
        }
    }

    @Test
    void testLocalhostHttp_Allowed() {
        // 本地 HTTP 允许（Ollama 开发场景）
        assertTrue(UrlSecurityValidator.isValidUrl("http://localhost:11434"));
        assertTrue(UrlSecurityValidator.isValidUrl("http://127.0.0.1:8080"));
        assertTrue(UrlSecurityValidator.isValidUrl("http://localhost"));
    }

    @Test
    void testNonHttps_External_Blocked() {
        String httpUrl = "http://example.com";
        assertFalse(UrlSecurityValidator.isValidUrl(httpUrl),
                "HTTP URL should be blocked");
    }

    @Test
    void testHttps_ValidDomain_Allowed() {
        String[] validUrls = {
                "https://api.openai.com",
                "https://api.anthropic.com",
                "https://generativelanguage.googleapis.com",
        };

        for (String url : validUrls) {
            assertTrue(UrlSecurityValidator.isValidUrl(url),
                    "Valid HTTPS URL should be allowed: " + url);
        }
    }

    @Test
    void testUrlWithCredentials_Blocked() {
        String url = "https://user:pass@api.openai.com";
        assertFalse(UrlSecurityValidator.isValidUrl(url),
                "URL with credentials should be blocked");
    }

    @Test
    void testUrlWithFragment_Blocked() {
        String url = "https://api.openai.com#fragment";
        assertFalse(UrlSecurityValidator.isValidUrl(url),
                "URL with fragment should be blocked");
    }

    @Test
    void testValidateUrl_ThrowsOnInvalidUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            UrlSecurityValidator.validateUrl("http://192.168.1.1:6379", "baseUrl");
        });
    }

    @Test
    void testValidateUrl_PassesForValidUrl() {
        assertDoesNotThrow(() -> {
            UrlSecurityValidator.validateUrl("https://api.openai.com", "baseUrl");
        });
    }

    @Test
    void testValidateUrl_PassesForLocalhostHttp() {
        assertDoesNotThrow(() -> {
            UrlSecurityValidator.validateUrl("http://localhost:11434", "baseUrl");
        });
    }

    @Test
    void testNullUrl_Blocked() {
        assertFalse(UrlSecurityValidator.isValidUrl(null));
    }

    @Test
    void testBlankUrl_Blocked() {
        assertFalse(UrlSecurityValidator.isValidUrl(""));
        assertFalse(UrlSecurityValidator.isValidUrl("   "));
    }

    @Test
    void testIpv6Loopback_Blocked() {
        // HTTPS + IPv6 loopback 应阻止
        String[] blockedIpv6 = {
                "https://[::1]:8080/path",
                "https://[::1]",
                "https://[0:0:0:0:0:0:0:1]/api",
        };
        for (String url : blockedIpv6) {
            assertFalse(UrlSecurityValidator.isValidUrl(url),
                    "HTTPS IPv6 loopback URL should be blocked: " + url);
        }
    }

    @Test
    void testIpv6LocalhostHttp_Allowed() {
        // HTTP + IPv6 loopback 允许（本地开发）
        assertTrue(UrlSecurityValidator.isValidUrl("http://[::1]:11434"));
    }

    @Test
    void testIpv6WithPort_ExtractsHostCorrectly() {
        // IPv6 with port should be correctly rejected as loopback
        assertFalse(UrlSecurityValidator.isValidUrl("https://[::1]:8080/path"));
        // Valid external with port should work
        assertTrue(UrlSecurityValidator.isValidUrl("https://api.openai.com:443"));
    }
}

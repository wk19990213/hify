package com.hify.common.util;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DNS 二次校验 — 防止 DNS 重绑定攻击。
 * 首次解析结果会被缓存，后续请求重新解析并与缓存结果对比。
 * 若解析结果不一致（攻击者篡改了 DNS 指向内网 IP），则拒绝解析。
 */
@Slf4j
public class SafeDns implements Dns {

    private final Dns delegate = Dns.SYSTEM;
    private final Map<String, List<InetAddress>> cache = new ConcurrentHashMap<>();

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        List<InetAddress> cached = cache.get(hostname);
        List<InetAddress> resolved = delegate.lookup(hostname);

        if (cached == null) {
            // 首次解析：缓存并返回
            cache.put(hostname, resolved);
            log.debug("SafeDns 首次缓存: host={}, ips={}", hostname, toIpList(resolved));
            return resolved;
        }

        // 二次校验：对比缓存与当前解析结果
        if (!ipListsEqual(cached, resolved)) {
            log.error("DNS 重绑定检测: host={}, 缓存IP={}, 当前解析IP={}",
                    hostname, toIpList(cached), toIpList(resolved));
            throw new UnknownHostException("DNS rebinding detected for " + hostname
                    + ": cached=" + toIpList(cached) + ", resolved=" + toIpList(resolved));
        }

        // 返回缓存结果，保证连接稳定性
        return cached;
    }

    /** 比较两个 IP 地址列表是否包含完全相同的地址 */
    private boolean ipListsEqual(List<InetAddress> a, List<InetAddress> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (InetAddress ai : a) {
            boolean found = false;
            for (InetAddress bi : b) {
                if (ai.getHostAddress().equals(bi.getHostAddress())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private String toIpList(List<InetAddress> list) {
        return list.stream()
                .map(InetAddress::getHostAddress)
                .toList()
                .toString();
    }
}

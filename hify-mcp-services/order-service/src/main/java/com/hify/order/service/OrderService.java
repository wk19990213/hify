package com.hify.order.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrderService {

    private final Map<String, Map<String, Object>> orders = new LinkedHashMap<>();

    public OrderService() {
        addOrder("12345", "已发货", "张三",
            List.of(Map.of("name", "Hify Pro 会员年卡", "price", 299.00, "qty", 1),
                Map.of("name", "API 调用包（10万次）", "price", 99.00, "qty", 2)),
            "SF1234567890", 497.00, "2026-05-20 10:30:00");

        addOrder("67890", "待发货", "李四",
            List.of(Map.of("name", "企业版许可证", "price", 1999.00, "qty", 1)),
            null, 1999.00, "2026-05-21 14:20:00");

        addOrder("11111", "已签收", "王五",
            List.of(Map.of("name", "技术咨询小时包", "price", 500.00, "qty", 3)),
            "YT9876543210", 1500.00, "2026-05-18 09:15:00");
    }

    private void addOrder(String orderId, String status, String customer,
                          List<Map<String, Object>> items, String tracking,
                          double totalAmount, String createTime) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderId", orderId);
        order.put("status", status);
        order.put("customer", customer);
        order.put("items", items);
        order.put("tracking", tracking);
        order.put("totalAmount", totalAmount);
        order.put("createTime", createTime);
        orders.put(orderId, order);
    }

    public Optional<Map<String, Object>> queryOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public List<Map<String, Object>> listOrders(String status) {
        List<Map<String, Object>> result = new ArrayList<>(orders.values());
        if (status != null && !status.isEmpty()) {
            result.removeIf(o -> !status.equals(o.get("status")));
        }
        return result;
    }
}

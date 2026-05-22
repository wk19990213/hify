#!/usr/bin/env python3
"""订单查询 MCP Server — 零依赖，纯 stdlib JSON-RPC over stdio"""
import json
import sys
import logging

logging.basicConfig(level=logging.INFO, stream=sys.stderr)
logger = logging.getLogger("order-server")

ORDERS = {
    "12345": {
        "orderId": "12345",
        "status": "已发货",
        "customer": "张三",
        "items": [
            {"name": "Hify Pro 会员年卡", "price": 299.00, "qty": 1},
            {"name": "API 调用包（10万次）", "price": 99.00, "qty": 2},
        ],
        "tracking": "SF1234567890",
        "totalAmount": 497.00,
        "createTime": "2026-05-20 10:30:00",
    },
    "67890": {
        "orderId": "67890",
        "status": "待发货",
        "customer": "李四",
        "items": [
            {"name": "企业版许可证", "price": 1999.00, "qty": 1}
        ],
        "tracking": None,
        "totalAmount": 1999.00,
        "createTime": "2026-05-21 14:20:00",
    },
    "11111": {
        "orderId": "11111",
        "status": "已签收",
        "customer": "王五",
        "items": [
            {"name": "技术咨询小时包", "price": 500.00, "qty": 3}
        ],
        "tracking": "YT9876543210",
        "totalAmount": 1500.00,
        "createTime": "2026-05-18 09:15:00",
    },
}


def make_response(req_id, result):
    return json.dumps({"jsonrpc": "2.0", "id": req_id, "result": result})


def make_error(req_id, code, message):
    return json.dumps({"jsonrpc": "2.0", "id": req_id, "error": {"code": code, "message": message}})


def handle_initialize(req_id, params):
    return make_response(req_id, {
        "protocolVersion": "2024-11-05",
        "capabilities": {"tools": {}},
        "serverInfo": {"name": "order-server", "version": "1.0.0"},
    })


def handle_list_tools(req_id, params):
    return make_response(req_id, {"tools": [
        {
            "name": "query_order",
            "description": "根据订单号查询订单详情，返回订单状态、客户、商品明细、快递单号等信息",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "orderId": {"type": "string", "description": "订单号，例如 12345"}
                },
                "required": ["orderId"],
            },
        },
        {
            "name": "list_orders",
            "description": "列出所有订单，可按状态过滤（已发货/待发货/已签收）",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "status": {"type": "string", "description": "订单状态过滤，可选：已发货、待发货、已签收"}
                },
            },
        },
    ]})


def handle_call_tool(req_id, params):
    name = params.get("name", "")
    arguments = params.get("arguments", {})

    if name == "query_order":
        order_id = arguments.get("orderId", "")
        order = ORDERS.get(order_id)
        if order is None:
            text = json.dumps({"error": f"订单不存在: {order_id}"}, ensure_ascii=False)
        else:
            text = json.dumps(order, ensure_ascii=False)
        return make_response(req_id, {"content": [{"type": "text", "text": text}]})

    if name == "list_orders":
        status = arguments.get("status")
        orders = list(ORDERS.values())
        if status:
            orders = [o for o in orders if o["status"] == status]
        text = json.dumps(orders, ensure_ascii=False)
        return make_response(req_id, {"content": [{"type": "text", "text": text}]})

    return make_response(req_id, {"content": [{"type": "text", "text": json.dumps({"error": f"未知工具: {name}"}, ensure_ascii=False)}]})


HANDLERS = {
    "initialize": handle_initialize,
    "tools/list": handle_list_tools,
    "tools/call": handle_call_tool,
}


def main():
    logger.info("order-server starting...")
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            req = json.loads(line)
            req_id = req.get("id")
            method = req.get("method", "")
            params = req.get("params", {})

            handler = HANDLERS.get(method)
            if handler:
                resp = handler(req_id, params)
            else:
                resp = make_error(req_id, -32601, f"Method not found: {method}")

            sys.stdout.write(resp + "\n")
            sys.stdout.flush()
        except Exception as e:
            logger.error(f"Error processing request: {e}")
            err = make_error(None, -32603, str(e))
            sys.stdout.write(err + "\n")
            sys.stdout.flush()
    logger.info("order-server shutting down.")


if __name__ == "__main__":
    main()

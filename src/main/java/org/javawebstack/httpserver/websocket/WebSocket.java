package org.javawebstack.httpserver.websocket;

import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.util.websocket.WebSocketUtil;

import java.io.IOException;

public class WebSocket {

    private final Exchange exchange;

    public WebSocket(Exchange exchange) {
        this.exchange = exchange;
    }

    public Exchange getExchange() {
        return exchange;
    }

    public void close() {
        close(null, null);
    }

    public void close(Integer code, String reason) {
        try {
            WebSocketUtil.close(exchange.socket(), code, reason);
        } catch (IOException ignored) {}
    }

    public void send(String message) {
        try {
            WebSocketUtil.send(exchange.socket(), message);
        } catch (IOException ignored) {
        }
    }

    public void send(byte[] message) {
        try {
            WebSocketUtil.send(exchange.socket(), message);
        } catch (IOException ignored) {
        }
    }

}

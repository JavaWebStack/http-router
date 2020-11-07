package org.javawebstack.httpserver.handler;

import org.javawebstack.httpserver.websocket.WebSocket;

public interface WebSocketHandler {
    void onConnect(WebSocket socket);
    void onMessage(WebSocket socket, String message);
    void onClose(WebSocket socket, int code, String reason);
}

package org.javawebstack.httpserver.handler;

import org.javawebstack.httpserver.websocket.WebSocket;

public interface WebSocketHandler {
    void onConnect(WebSocket socket);

    void onMessage(WebSocket socket, String message);

    void onMessage(WebSocket socket, byte[] message);

    void onClose(WebSocket socket, Integer code, String reason);
}

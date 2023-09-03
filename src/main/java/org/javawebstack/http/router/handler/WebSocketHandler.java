package org.javawebstack.http.router.handler;

import org.javawebstack.http.router.websocket.WebSocket;

public interface WebSocketHandler {
    void onConnect(WebSocket socket);

    void onMessage(WebSocket socket, String message);

    void onMessage(WebSocket socket, byte[] message);

    void onClose(WebSocket socket, Integer code, String reason);
}

package org.javawebstack.httpserver.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;

import java.util.HashMap;
import java.util.Map;

@org.eclipse.jetty.websocket.api.annotations.WebSocket
public class InternalWebSocketAdapter {
    public static Map<String, WebSocket> webSockets = new HashMap<>();
    @OnWebSocketConnect
    public void onConnect(Session session){
        WebSocket socket = webSockets.get(session.getUpgradeResponse().getHeader("X-Server-WSID"));
        if(socket == null) {
            session.close(500, "Server Error");
            return;
        }
        socket.setSession(session);
        socket.getHandler().onConnect(socket);
    }
    @OnWebSocketMessage
    public void onMessage(Session session, String message){
        WebSocket socket = webSockets.get(session.getUpgradeResponse().getHeader("X-Server-WSID"));
        if(socket == null) {
            session.close(500, "Server Error");
            return;
        }
        socket.getHandler().onMessage(socket, message);
    }
    @OnWebSocketClose
    public void onClose(Session session, int code, String reason){
        WebSocket socket = webSockets.get(session.getUpgradeResponse().getHeader("X-Server-WSID"));
        if(socket != null){
            webSockets.remove(session.getUpgradeResponse().getHeader("X-Server-WSID"));
            socket.getHandler().onClose(socket, code, reason);
        }
    }
}

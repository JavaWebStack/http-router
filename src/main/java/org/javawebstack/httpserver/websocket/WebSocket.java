package org.javawebstack.httpserver.websocket;

import org.eclipse.jetty.websocket.api.Session;

public class WebSocket {
    private WebSocketConnectionEvent webSocketConnectionEvent;
    private WebSocketErrorEvent webSocketErrorEvent;
    private WebSocketMessageEvent webSocketMessageEvent;

    public WebSocket connect(WebSocketConnectionEvent event){
        webSocketConnectionEvent = event;
        return this;
    }

    public WebSocket message(WebSocketMessageEvent event){
        webSocketMessageEvent = event;
        return this;
    }

    public WebSocket error(WebSocketErrorEvent event){
        webSocketErrorEvent = event;
        return this;
    }

    public interface WebSocketConnectionEvent{
        void onConnect(Session session);
    }

    public interface WebSocketErrorEvent{
        void onError(Session session, Throwable throwable);
    }

    public interface WebSocketMessageEvent{
        void onMessage(Session session, String message);
    }

    public WebSocketConnectionEvent getWebSocketConnectionEvent() {
        return webSocketConnectionEvent;
    }

    public WebSocketErrorEvent getWebSocketErrorEvent() {
        return webSocketErrorEvent;
    }

    public WebSocketMessageEvent getWebSocketMessageEvent() {
        return webSocketMessageEvent;
    }
}

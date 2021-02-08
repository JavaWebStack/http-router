package org.javawebstack.httpserver.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.handler.WebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;

public class WebSocket {

    private final Exchange exchange;
    private final WebSocketHandler handler;
    private Session session;

    public WebSocket(Exchange exchange, WebSocketHandler handler) {
        this.exchange = exchange;
        this.handler = handler;
    }

    public Exchange getExchange() {
        return exchange;
    }

    WebSocketHandler getHandler() {
        return handler;
    }

    void setSession(Session session) {
        this.session = session;
    }

    public void close() {
        session.close();
    }

    public void close(int code, String reason) {
        session.close(code, reason);
    }

    public void send(String message) {
        try {
            session.getRemote().sendString(message);
        } catch (IOException ignored) {
        }
    }

    public void send(byte[] message) {
        try {
            session.getRemote().sendBytes(ByteBuffer.wrap(message));
        } catch (IOException ignored) {
        }
    }

}

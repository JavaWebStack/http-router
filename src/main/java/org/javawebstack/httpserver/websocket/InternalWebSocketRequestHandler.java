package org.javawebstack.httpserver.websocket;

import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.adapter.IHTTPSocket;
import org.javawebstack.httpserver.handler.RequestHandler;
import org.javawebstack.httpserver.handler.WebSocketHandler;
import org.javawebstack.httpserver.util.websocket.WebSocketFrame;
import org.javawebstack.httpserver.util.websocket.WebSocketUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class InternalWebSocketRequestHandler implements RequestHandler {

    private final WebSocketHandler handler;

    public InternalWebSocketRequestHandler(WebSocketHandler handler) {
        this.handler = handler;
    }

    public Object handle(Exchange exchange) {
        IHTTPSocket socket = exchange.socket();
        try {
            WebSocket webSocket = new WebSocket(exchange);
            handler.onConnect(webSocket);
            WebSocketFrame frame;
            while (true) {
                try {
                    frame = WebSocketFrame.read(socket.getInputStream());
                } catch (IOException ex) {
                    handler.onClose(webSocket, null, null);
                    socket.close();
                    break;
                }
                if(frame.getOpcode() == WebSocketUtil.OP_CLOSE) {
                    WebSocketUtil.ClosePayload close = WebSocketUtil.parseClose(frame.getPayload());
                    handler.onClose(webSocket, close.getCode(), close.getReason());
                    socket.close();
                    break;
                }
                if(frame.getOpcode() == WebSocketUtil.OP_PING) {
                    frame.setOpcode(WebSocketUtil.OP_PONG).setMaskKey(null).write(socket.getOutputStream());
                    continue;
                }
                if(frame.getOpcode() == WebSocketUtil.OP_BINARY) {
                    handler.onMessage(webSocket, frame.getPayload());
                    continue;
                }
                if(frame.getOpcode() == WebSocketUtil.OP_TEXT) {
                    handler.onMessage(webSocket, new String(frame.getPayload(), StandardCharsets.UTF_8));
                    continue;
                }
            }
        } catch (IOException ignored) {}
        return null;
    }
}

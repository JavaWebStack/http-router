package org.javawebstack.httpserver.util.websocket;

import org.javawebstack.httpserver.HTTPStatus;
import org.javawebstack.httpserver.adapter.IHTTPSocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class WebSocketUtil {

    public static final byte OP_CLOSE = 0x8;
    public static final byte OP_PING = 0x9;
    public static final byte OP_PONG = 0xA;
    public static final byte OP_TEXT = 0x1;
    public static final byte OP_BINARY = 0x2;

    public static String[] getProtocols(IHTTPSocket socket) {
        String key = socket.getRequestHeader("sec-websocket-protocol");
        if(key == null || key.length() == 0)
            return new String[0];
        return key.split(" ?,");
    }

    public static boolean accept(IHTTPSocket socket, String protocol) throws IOException {
        String key = socket.getRequestHeader("sec-websocket-key");
        if(key == null) {
            socket.setResponseStatus(HTTPStatus.BAD_REQUEST);
            socket.writeHeaders();
            socket.close();
            return false;
        }
        if(protocol != null)
            socket.setResponseHeader("sec-websocket-protocol", protocol);
        socket.setResponseHeader("sec-websocket-accept", calcKey(key));
        socket.setResponseStatus(HTTPStatus.SWITCHING_PROTOCOLS);
        socket.setResponseHeader("upgrade", "websocket");
        socket.setResponseHeader("Connection", "Upgrade");
        socket.writeHeaders();
        return true;
    }

    public static void close(IHTTPSocket socket, Integer code, String reason) throws IOException {
        byte[] reasonBytes = reason == null ? null : reason.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[code == null ? 0 : (reason == null ? 2 : (reasonBytes.length + 2))];
        if(code != null) {
            payload[0] = (byte) (code >> 8);
            payload[1] = (byte) (code & 0xF);
            if(reasonBytes != null)
                System.arraycopy(reasonBytes, 0, payload, 2, reasonBytes.length);
        }
        new WebSocketFrame().setFin(true).setOpcode(OP_CLOSE).setPayload(payload).write(socket.getOutputStream());
        socket.close();
    }

    public static ClosePayload parseClose(byte[] payload) {
        ClosePayload close = new ClosePayload();
        if(payload.length >= 2) {
            close.code = (payload[0] << 8) | payload[1];
            if(payload.length > 2) {
                byte[] reasonBytes = new byte[payload.length - 2];
                System.arraycopy(payload, 2, reasonBytes, 0, reasonBytes.length);
                close.reason = new String(reasonBytes, StandardCharsets.UTF_8);
            }
        }
        return close;
    }

    public static void send(IHTTPSocket socket, String message) throws IOException {
        new WebSocketFrame().setFin(true).setOpcode(OP_TEXT).setPayload(message.getBytes(StandardCharsets.UTF_8)).write(socket.getOutputStream());
    }

    public static void send(IHTTPSocket socket, byte[] message) throws IOException {
        new WebSocketFrame().setFin(true).setOpcode(OP_BINARY).setPayload(message).write(socket.getOutputStream());
    }

    public static class ClosePayload {

        private Integer code;
        private String reason;

        public Integer getCode() {
            return code;
        }

        public String getReason() {
            return reason;
        }
    }

    private static String calcKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII));
            return new String(Base64.getEncoder().encode(digest.digest()), StandardCharsets.US_ASCII);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

}

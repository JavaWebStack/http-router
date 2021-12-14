package org.javawebstack.httpserver.adapter.untertow;

import io.undertow.Undertow;
import org.javawebstack.httpserver.adapter.IHTTPSocketHandler;
import org.javawebstack.httpserver.adapter.IHTTPSocketServer;

import java.io.IOException;

public class UndertowHTTPSocketServer implements IHTTPSocketServer {

    private int port = 80;
    private Undertow server;
    private IHTTPSocketHandler handler;

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(httpServerExchange -> handler.handle(new UndertowHTTPSocket(httpServerExchange)))
                .build();
        server.start();
    }

    public void stop() {
        server.stop();
    }

    public void join() {
        try {
            server.getWorker().awaitTermination();
        } catch (InterruptedException e) {}
    }

    public void setHandler(IHTTPSocketHandler handler) {
        this.handler = handler;
    }

    public boolean isWebSocketSupported() {
        return false;
    }

}

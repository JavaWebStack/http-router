package org.javawebstack.httpserver.adapter.undertow;

import io.undertow.Undertow;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.websockets.core.WebSocketVersion;
import org.javawebstack.httpserver.adapter.IHTTPSocketHandler;
import org.javawebstack.httpserver.adapter.IHTTPSocketServer;
import org.javawebstack.httpserver.util.websocket.WebSocketUtil;
import org.xnio.Options;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UndertowHTTPSocketServer implements IHTTPSocketServer {

    private int port = 80;
    private Undertow server;
    private IHTTPSocketHandler handler;
    private ExecutorService executorService;

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        executorService = Executors.newCachedThreadPool();
        server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setServerOption(Options.KEEP_ALIVE, true)
                .setHandler(new BlockingHandler(httpServerExchange -> {
                    if(httpServerExchange.getRequestHeaders().contains("sec-websocket-key")) {
                        httpServerExchange.upgradeChannel((streamConnection, httpServerExchange1) -> {
                            InputStream inputStream = new StreamSourceInputStream(streamConnection.getSourceChannel());
                            OutputStream outputStream = new StreamSinkOutputStream(streamConnection.getSinkChannel());
                            handler.handle(new UndertowHTTPSocket(httpServerExchange1, inputStream, outputStream));
                        });
                        httpServerExchange.putAttachment(WebSocketVersion.ATTACHMENT_KEY, WebSocketVersion.V13);
                        if(!WebSocketUtil.accept(new UndertowHTTPSocket(httpServerExchange, null, null), null))
                            return;
                        httpServerExchange.endExchange();
                    } else {
                        handler.handle(new UndertowHTTPSocket(httpServerExchange, null, null));
                    }
                }))
                .build();
        server.start();
    }

    public void stop() {
        executorService.shutdown();
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
        return true;
    }

}

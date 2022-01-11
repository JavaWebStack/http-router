package org.javawebstack.httpserver.adapter.simple;

import org.javawebstack.httpserver.adapter.IHTTPSocketHandler;
import org.javawebstack.httpserver.adapter.IHTTPSocketServer;
import org.javawebstack.httpserver.util.websocket.WebSocketUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class SimpleHTTPSocketServer implements IHTTPSocketServer {

    private final Thread schedulerThread;
    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private int port = 80;
    private int maxThreads = 64;
    private IHTTPSocketHandler handler;

    public SimpleHTTPSocketServer() {
        this.schedulerThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    SimpleHTTPSocket httpSocket = new SimpleHTTPSocket(socket);
                    executorService.execute(() -> {
                        if(httpSocket.getRequestHeaderNames().contains("sec-websocket-key")) {
                            try {
                                if(!WebSocketUtil.accept(httpSocket, null))
                                    return;
                            } catch (IOException e) {
                                return;
                            }
                        }
                        handler.handle(httpSocket);
                    });
                } catch (IOException exception) {}
            }
        });
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setHandler(IHTTPSocketHandler handler) {
        this.handler = handler;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void start() throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.executorService = new ThreadPoolExecutor(1, maxThreads, 60L, TimeUnit.SECONDS, new SynchronousQueue());
        this.schedulerThread.start();
    }

    public void join() {
        try {
            schedulerThread.join();
        } catch (InterruptedException e) {}
    }

    public void stop() {
        this.executorService.shutdown();
        try {
            this.serverSocket.close();
        } catch (IOException e) {}
    }

    public boolean isWebSocketSupported() {
        return true;
    }

}

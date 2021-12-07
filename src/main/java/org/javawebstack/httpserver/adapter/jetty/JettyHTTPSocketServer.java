package org.javawebstack.httpserver.adapter.jetty;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.javawebstack.httpserver.adapter.IHTTPSocketHandler;
import org.javawebstack.httpserver.adapter.IHTTPSocketServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JettyHTTPSocketServer implements IHTTPSocketServer {

    private Server server;
    private IHTTPSocketHandler handler;
    private int port;

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        server = new Server(port);
        server.setHandler(new AbstractHandler() {
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
                if("websocket".equals(httpServletRequest.getHeader("Upgrade"))) {
                    httpServletResponse.setStatus(400);
                    httpServletResponse.getOutputStream().write("Websockets are not supported by this server!".getBytes(StandardCharsets.UTF_8));
                    httpServletResponse.getOutputStream().close();
                    return;
                }
                handler.handle(new JettyHTTPSocket(httpServletRequest, httpServletResponse));
            }
        });
        server.setErrorHandler(new ErrorHandler());
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void join() {
        try {
            server.join();
        } catch (InterruptedException e) {}
    }

    public void setHandler(IHTTPSocketHandler handler) {
        this.handler = handler;
    }

    public boolean isWebSocketSupported() {
        return false;
    }

}

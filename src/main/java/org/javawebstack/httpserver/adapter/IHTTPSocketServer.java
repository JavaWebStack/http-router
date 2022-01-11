package org.javawebstack.httpserver.adapter;

import java.io.IOException;

public interface IHTTPSocketServer {

    void setPort(int port);
    int getPort();
    void setMaxThreads(int maxThreads);
    void start() throws IOException;
    void stop();
    void join();
    void setHandler(IHTTPSocketHandler handler);
    boolean isWebSocketSupported();

}

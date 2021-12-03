package org.javawebstack.httpserver.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HTTPServerSocket {

    private final ServerSocket serverSocket;

    public HTTPServerSocket(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void close() throws IOException {
        serverSocket.close();
    }

    public boolean isClosed() {
        return serverSocket.isClosed();
    }

    public HTTPSocket accept() throws IOException {
        Socket socket = serverSocket.accept();
        return new HTTPSocket(socket);
    }

}

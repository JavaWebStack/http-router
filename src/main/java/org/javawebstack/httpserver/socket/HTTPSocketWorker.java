package org.javawebstack.httpserver.socket;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class HTTPSocketWorker {

    private final Thread schedulerThread;
    private ExecutorService executorService;
    private final HTTPServerSocket serverSocket;

    public HTTPSocketWorker(HTTPServerSocket serverSocket, Consumer<HTTPSocket> handler) {
        this.serverSocket = serverSocket;
        this.schedulerThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    HTTPSocket socket = serverSocket.accept();
                    executorService.execute(() -> {
                        try {
                            handler.accept(socket);
                            if(!socket.isClosed())
                                socket.close();
                        } catch (IOException ex) {}
                    });
                } catch (IOException exception) {}
            }
        });
    }

    public HTTPSocketWorker start() {
        this.executorService = Executors.newCachedThreadPool();
        this.schedulerThread.start();
        return this;
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

}

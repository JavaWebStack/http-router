package org.javawebstack.httpserver.adapter.simple;

import org.javawebstack.httpserver.HTTPMethod;
import org.javawebstack.httpserver.HTTPStatus;
import org.javawebstack.httpserver.adapter.IHTTPSocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SimpleHTTPSocket implements IHTTPSocket {

    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final HTTPMethod requestMethod;
    private final String requestPath;
    private String requestQuery;
    private final String requestVersion;
    private final Map<String, List<String>> requestHeaders = new HashMap<>();
    private final Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
    private int responseStatus = 200;
    private String responseStatusMessage = "OK";
    private boolean headersSent;

    public SimpleHTTPSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        socket.getOutputStream().flush();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int lb = -1;
        while (true) {
            int b = inputStream.read();
            if(b == -1) {
                socket.close();
                throw new IOException("Unexpected end of stream");
            }
            if(b == '\r' && lb == '\n') {
                b = inputStream.read();
                break;
            }
            baos.write(b);
            lb = b;
        }
        String[] lines = new String(baos.toByteArray(), StandardCharsets.UTF_8).split("\\r?\\n");
        if(lines.length < 2) {
            socket.close();
            throw new IOException("Invalid http request");
        }
        String[] first = lines[0].split(" ");
        if(first.length != 3 || !first[1].startsWith("/")) {
            socket.close();
            throw new IOException("Invalid http request");
        }
        requestMethod = HTTPMethod.valueOf(first[0]);
        String[] pathSplit = first[1].split("\\?", 2);
        requestPath = pathSplit[0];
        if(pathSplit.length == 2)
            requestQuery = pathSplit[1];
        requestVersion = first[2];
        if(!requestVersion.equals("HTTP/1.1") && !requestVersion.equals("HTTP/1.0")) {
            setResponseStatus(HTTPStatus.HTTP_VERSION_NOT_SUPPORTED);
            writeHeaders();
            close();
            throw new IOException("Unsupported http version");
        }
        for(int i=1; i<lines.length; i++) {
            if(lines[i].length() == 0)
                continue;
            String[] hspl = lines[i].split(": ", 2);
            if(hspl.length != 2)
                throw new IOException("Invalid http request");
            List<String> values = requestHeaders.computeIfAbsent(hspl[0].toLowerCase(Locale.ROOT), h -> new ArrayList<>());
            values.add(hspl[1]);
        }
    }

    public String getRemoteAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    public SimpleHTTPSocket setResponseStatus(HTTPStatus status) {
        return setResponseStatus(status.getStatus(), status.getMessage());
    }

    public SimpleHTTPSocket setResponseStatus(int status) {
        HTTPStatus s = HTTPStatus.byStatus(status);
        return setResponseStatus(status, s != null ? s.getMessage() : "Unknown");
    }

    public SimpleHTTPSocket setResponseStatus(int status, String message) {
        this.responseStatus = status;
        this.responseStatusMessage = message;
        return this;
    }

    public SimpleHTTPSocket setResponseHeader(String name, String value) {
        responseHeaders.put(name.toLowerCase(Locale.ROOT), Arrays.asList(value));
        return this;
    }

    public SimpleHTTPSocket addResponseHeader(String name, String value) {
        responseHeaders.computeIfAbsent(name.toLowerCase(Locale.ROOT), h -> new ArrayList<>()).add(value);
        return this;
    }

    public void close() throws IOException {
        socket.close();
    }

    public void writeHeaders() throws IOException {
        if(headersSent)
            return;
        headersSent = true;
        StringBuilder sb = new StringBuilder(requestVersion)
                .append(' ')
                .append(responseStatus)
                .append(' ')
                .append(responseStatusMessage)
                .append("\r\n");
        responseHeaders.forEach((k, l) -> l.forEach(v -> sb.append(k.toLowerCase(Locale.ROOT)).append(": ").append(v).append("\r\n")));
        sb.append("\r\n");
        outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return new HTTPOutputStream();
    }

    public HTTPMethod getRequestMethod() {
        return requestMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getRequestQuery() {
        return requestQuery;
    }

    public String getRequestVersion() {
        return requestVersion;
    }

    public Set<String> getRequestHeaderNames() {
        return requestHeaders.keySet();
    }

    public String getRequestHeader(String name) {
        List<String> values = requestHeaders.get(name.toLowerCase(Locale.ROOT));
        return values == null || values.size() == 0 ? null : values.get(0);
    }

    public List<String> getRequestHeaders(String name) {
        return requestHeaders.getOrDefault(name.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseStatusMessage() {
        return responseStatusMessage;
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    private class HTTPOutputStream extends OutputStream {
        public void write(int i) throws IOException {
            if(!headersSent)
                writeHeaders();
            outputStream.write(i);
        }
        public void close() throws IOException {
            outputStream.close();
        }
        public void flush() throws IOException {
            outputStream.flush();
        }
    }

}

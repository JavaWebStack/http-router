package org.javawebstack.httpserver.test;

import org.javawebstack.httpserver.HTTPMethod;
import org.javawebstack.httpserver.adapter.IHTTPSocket;

import java.io.*;
import java.util.*;

public class TestHTTPSocket implements IHTTPSocket {

    private final HTTPMethod requestMethod;
    private final String requestPath;
    private final String requestQuery;
    private InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    private final OutputStream outputStream = new ByteArrayOutputStream();
    private boolean closed;
    private int responseStatus = 200;
    private String responseStatusMessage = "OK";
    private final Map<String, List<String>> requestHeaders = new HashMap<>();
    private final Map<String, List<String>> responseHeaders = new HashMap<>();

    public TestHTTPSocket(HTTPMethod method, String url) {
        this(method, url, null);
    }

    public TestHTTPSocket(HTTPMethod method, String url, Map<String, String> headers) {
        this.requestMethod = method;
        String[] pathSplit = url.split("\\?", 2);
        requestPath = pathSplit[0];
        if(pathSplit.length == 2)
            requestQuery = pathSplit[1];
        else
            requestQuery = null;
        if(headers != null) {
            headers.entrySet().forEach(e -> {
                List<String> list = new ArrayList<>();
                list.add(e.getValue());
                this.requestHeaders.put(e.getKey(), list);
            });
        }
    }

    public Map<String, List<String>> getRequestHeaders() {
        return requestHeaders;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    public TestHTTPSocket setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    public void close() throws IOException {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public TestHTTPSocket setResponseStatus(int status, String message) {
        this.responseStatus = status;
        this.responseStatusMessage = message;
        return this;
    }

    public TestHTTPSocket setResponseHeader(String name, String value) {
        responseHeaders.put(name.toLowerCase(Locale.ROOT), Arrays.asList(value));
        return this;
    }

    public TestHTTPSocket addResponseHeader(String name, String value) {
        responseHeaders.computeIfAbsent(name.toLowerCase(Locale.ROOT), h -> new ArrayList<>()).add(value);
        return this;
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
        return "HTTP/1.1";
    }

    public Set<String> getRequestHeaderNames() {
        return requestHeaders.keySet();
    }

    public List<String> getRequestHeaders(String name) {
        return requestHeaders.get(name);
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseStatusMessage() {
        return responseStatusMessage;
    }

    public void writeHeaders() throws IOException {

    }

    public String getRemoteAddress() {
        return "127.0.0.1";
    }

}

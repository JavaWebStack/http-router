package org.javawebstack.httpserver.adapter.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import org.javawebstack.httpserver.HTTPMethod;
import org.javawebstack.httpserver.HTTPStatus;
import org.javawebstack.httpserver.adapter.IHTTPSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

public class UndertowHTTPSocket implements IHTTPSocket {

    private final HttpServerExchange exchange;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private boolean closed;

    public UndertowHTTPSocket(HttpServerExchange exchange, InputStream inputStream, OutputStream outputStream) {
        this.exchange = exchange;
        this.inputStream = inputStream == null ? exchange.getInputStream() : inputStream;
        this.outputStream = outputStream == null ? exchange.getOutputStream() : outputStream;
    }

    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    public void close() throws IOException {
        if(closed)
            return;
        closed = true;
        exchange.getOutputStream().close();
    }

    public boolean isClosed() {
        return exchange.isComplete();
    }

    public IHTTPSocket setResponseStatus(int status, String message) {
        exchange.setStatusCode(status);
        return this;
    }

    public IHTTPSocket setResponseHeader(String name, String value) {
        exchange.getResponseHeaders().put(new HttpString(name), value);
        return this;
    }

    public IHTTPSocket addResponseHeader(String name, String value) {
        exchange.getResponseHeaders().add(new HttpString(name), value);
        return this;
    }

    public HTTPMethod getRequestMethod() {
        return HTTPMethod.valueOf(exchange.getRequestMethod().toString());
    }

    public String getRequestPath() {
        return exchange.getRequestPath();
    }

    public String getRequestQuery() {
        return exchange.getQueryString();
    }

    public String getRequestVersion() {
        return exchange.getProtocol().toString();
    }

    public Set<String> getRequestHeaderNames() {
        return exchange.getRequestHeaders().getHeaderNames().stream().map(HttpString::toString).collect(Collectors.toSet());
    }

    public String getRequestHeader(String name) {
        HeaderValues values = exchange.getRequestHeaders().get(name);
        if(values == null)
            return null;
        return values.getFirst();
    }

    public List<String> getRequestHeaders(String name) {
        HeaderValues values = exchange.getRequestHeaders().get(name);
        if(values == null)
            return Collections.emptyList();
        return new ArrayList<>(values);
    }

    public int getResponseStatus() {
        return exchange.getStatusCode();
    }

    public String getResponseStatusMessage() {
        HTTPStatus status = HTTPStatus.byStatus(getResponseStatus());
        if(status == null)
            return null;
        return status.getMessage();
    }

    public void writeHeaders() throws IOException {
        exchange.getOutputStream().write(new byte[0]);
    }

    public String getRemoteAddress() {
        return exchange.getSourceAddress().getAddress().getHostAddress();
    }

    public HttpServerExchange getExchange() {
        return exchange;
    }

}

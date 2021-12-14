package org.javawebstack.httpserver.adapter.untertow;

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

    public UndertowHTTPSocket(HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    public InputStream getInputStream() throws IOException {
        return exchange.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return exchange.getOutputStream();
    }

    public void close() throws IOException {
        exchange.endExchange();
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
        exchange.getResponseSender().send("");
    }

    public String getRemoteAddress() {
        return exchange.getSourceAddress().getAddress().getHostAddress();
    }

    public HttpServerExchange getExchange() {
        return exchange;
    }

}

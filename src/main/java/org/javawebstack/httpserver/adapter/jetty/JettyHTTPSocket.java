package org.javawebstack.httpserver.adapter.jetty;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.javawebstack.httpserver.HTTPMethod;
import org.javawebstack.httpserver.HTTPStatus;
import org.javawebstack.httpserver.adapter.IHTTPSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JettyHTTPSocket implements IHTTPSocket {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private boolean closed;

    public JettyHTTPSocket(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public HttpServletRequest rawRequest() {
        return request;
    }

    public HttpServletResponse rawResponse() {
        return response;
    }

    public InputStream getInputStream() throws IOException {
        return request.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    public void close() throws IOException {
        closed = true;
        response.getOutputStream().close();
    }

    public boolean isClosed() {
        return closed;
    }

    public IHTTPSocket setResponseStatus(int status, String message) {
        response.setStatus(status, message);
        return this;
    }

    public IHTTPSocket setResponseHeader(String name, String value) {
        response.setHeader(name, value);
        return this;
    }

    public IHTTPSocket addResponseHeader(String name, String value) {
        response.addHeader(name, value);
        return this;
    }

    public HTTPMethod getRequestMethod() {
        return HTTPMethod.valueOf(request.getMethod());
    }

    public String getRequestPath() {
        return request.getPathInfo();
    }

    public String getRequestQuery() {
        return request.getQueryString();
    }

    public String getRequestVersion() {
        return "HTTP/1.1";
    }

    public Set<String> getRequestHeaderNames() {
        return new HashSet<>(Collections.list(request.getHeaderNames()));
    }

    public String getRequestHeader(String name) {
        return request.getHeader(name);
    }

    public List<String> getRequestHeaders(String name) {
        return Collections.list(request.getHeaders(name));
    }

    public int getResponseStatus() {
        return response.getStatus();
    }

    public String getResponseStatusMessage() {
        HTTPStatus status = HTTPStatus.byStatus(response.getStatus());
        return status == null ? null : status.getMessage();
    }

    public void writeHeaders() throws IOException {
        response.flushBuffer();
    }

    public String getRemoteAddress() {
        return request.getRemoteAddr();
    }

}

package org.javawebstack.http.router.adapter;

import org.javawebstack.http.router.HTTPMethod;
import org.javawebstack.http.router.HTTPStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public interface IHTTPSocket {

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream() throws IOException;

    void close() throws IOException;

    boolean isClosed();

    default IHTTPSocket setResponseStatus(HTTPStatus status) {
        return setResponseStatus(status.getStatus(), status.getMessage());
    }

    default IHTTPSocket setResponseStatus(int status) {
        HTTPStatus s = HTTPStatus.byStatus(status);
        return setResponseStatus(status, s != null ? s.getMessage() : "Unknown");
    }

    IHTTPSocket setResponseStatus(int status, String message);

    IHTTPSocket setResponseHeader(String name, String value);

    IHTTPSocket addResponseHeader(String name, String value);

    HTTPMethod getRequestMethod();

    String getRequestPath();

    String getRequestQuery();

    String getRequestVersion();

    Set<String> getRequestHeaderNames();

    default String getRequestHeader(String name) {
        List<String> headers = getRequestHeaders(name);
        if(headers == null || headers.size() == 0)
            return null;
        return headers.get(0);
    }

    List<String> getRequestHeaders(String name);

    int getResponseStatus();

    String getResponseStatusMessage();

    void writeHeaders() throws IOException;

    String getRemoteAddress();

}

package org.javawebstack.httpserver.test;

import org.javawebstack.httpserver.HTTPMethod;
import org.javawebstack.httpserver.HTTPServer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class HTTPTest {

    private final HTTPServer server;
    private final Map<String, String> defaultHeaders = new HashMap<>();

    protected HTTPTest(HTTPServer server) {
        this.server = server;
    }

    public HTTPServer getServer() {
        return server;
    }

    public void setDefaultHeader(String key, String value) {
        defaultHeaders.put(key, value);
    }

    public void setBearerToken(String token) {
        setDefaultHeader("Authorization", "Bearer " + token);
    }

    public TestExchange httpGet(String url) {
        return httpRequest(HTTPMethod.GET, url, null);
    }

    public TestExchange httpPost(String url) {
        return httpPost(url, null);
    }

    public TestExchange httpPost(String url, Object content) {
        return httpRequest(HTTPMethod.POST, url, content);
    }

    public TestExchange httpPut(String url) {
        return httpPut(url, null);
    }

    public TestExchange httpPut(String url, Object content) {
        return httpRequest(HTTPMethod.PUT, url, content);
    }

    public TestExchange httpDelete(String url) {
        return httpDelete(url, null);
    }

    public TestExchange httpDelete(String url, Object content) {
        return httpRequest(HTTPMethod.DELETE, url, content);
    }

    public TestExchange httpRequest(HTTPMethod method, String url, Object content) {
        TestHTTPSocket socket = new TestHTTPSocket(method, url);
        defaultHeaders.forEach((k, v) -> socket.getRequestHeaders().put(k.toLowerCase(Locale.ROOT), Collections.singletonList(v)));
        if (content != null) {
            if (content instanceof String) {
                socket.setInputStream(new ByteArrayInputStream(((String) content).getBytes(StandardCharsets.UTF_8)));
            } else if (content instanceof byte[]) {
                socket.setInputStream(new ByteArrayInputStream((byte[]) content));
            } else {
                socket.setInputStream(new ByteArrayInputStream(server.getAbstractMapper().toAbstract(content).toJsonString().getBytes(StandardCharsets.UTF_8)));
            }
        }
        TestExchange exchange = new TestExchange(server, socket);
        server.execute(exchange);
        return exchange;
    }

}

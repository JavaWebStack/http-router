package org.javawebstack.httpserver.test;

import org.javawebstack.httpserver.HTTPServer;
import org.javawebstack.httpserver.helper.HttpMethod;

import java.util.HashMap;
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
        return httpRequest(HttpMethod.GET, url, null);
    }

    public TestExchange httpPost(String url) {
        return httpPost(url, null);
    }

    public TestExchange httpPost(String url, Object content) {
        return httpRequest(HttpMethod.POST, url, content);
    }

    public TestExchange httpPut(String url) {
        return httpPut(url, null);
    }

    public TestExchange httpPut(String url, Object content) {
        return httpRequest(HttpMethod.PUT, url, content);
    }

    public TestExchange httpDelete(String url) {
        return httpDelete(url, null);
    }

    public TestExchange httpDelete(String url, Object content) {
        return httpRequest(HttpMethod.DELETE, url, content);
    }

    public TestExchange httpRequest(HttpMethod method, String url, Object content) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setPath(url);
        defaultHeaders.forEach(request::addHeader);
        if (content != null) {
            if (content instanceof String) {
                request.setContent((String) content);
            } else if (content instanceof byte[]) {
                request.setContent((byte[]) content);
            } else {
                request.setContent(server.getAbstractMapper().toAbstract(content).toJsonString());
            }
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestExchange exchange = new TestExchange(server, request, response);
        server.execute(exchange);
        return exchange;
    }

}

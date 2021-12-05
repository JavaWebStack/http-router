package org.javawebstack.httpserver.test;

import org.javawebstack.abstractdata.AbstractElement;
import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.HTTPServer;
import org.javawebstack.httpserver.adapter.IHTTPSocket;
import org.javawebstack.httpserver.util.MimeType;
import org.junit.jupiter.api.Assertions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TestExchange extends Exchange {
    
    private TestHTTPSocket testSocket;

    public TestExchange(HTTPServer service, TestHTTPSocket socket) {
        super(service, socket);
        this.testSocket = socket;
    }

    public TestExchange print() {
        printResponse();
        return this;
    }

    public String getOutputString() {
        try {
            return new String(((ByteArrayOutputStream) testSocket.getOutputStream()).toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return null;
        }
    }

    public TestExchange printResponse() {
        System.out.println("HTTP Response " + testSocket.getResponseStatus());
        System.out.println(getOutputString());
        return this;
    }

    public TestExchange assertStatus(int status) {
        Assertions.assertEquals(status, testSocket.getResponseStatus());
        return this;
    }

    public TestExchange assertStatus(int status, String message) {
        Assertions.assertEquals(status, testSocket.getResponseStatus(), message);
        return this;
    }

    public TestExchange assertNotStatus(int status) {
        Assertions.assertNotEquals(status, testSocket.getResponseStatus());
        return this;
    }

    public TestExchange assertNotStatus(int status, String message) {
        Assertions.assertNotEquals(status, testSocket.getResponseStatus(), message);
        return this;
    }

    public TestExchange assertOk() {
        Assertions.assertEquals(200, testSocket.getResponseStatus());
        return this;
    }

    public TestExchange assertOk(String message) {
        Assertions.assertEquals(200, testSocket.getResponseStatus(), message);
        return this;
    }

    public TestExchange assertCreated() {
        Assertions.assertEquals(201, testSocket.getResponseStatus());
        return this;
    }

    public TestExchange assertCreated(String message) {
        Assertions.assertEquals(201, testSocket.getResponseStatus(), message);
        return this;
    }

    public TestExchange assertNoContent() {
        Assertions.assertEquals(204, testSocket.getResponseStatus());
        return this;
    }

    public TestExchange assertNoContent(String message) {
        Assertions.assertEquals(204, testSocket.getResponseStatus(), message);
        return this;
    }

    public TestExchange assertRedirect() {
        Assertions.assertTrue(isRedirect());
        return this;
    }

    public TestExchange assertRedirect(String message) {
        Assertions.assertTrue(isRedirect(), message);
        return this;
    }

    public TestExchange assertRedirectTo(String url) {
        assertRedirect();
        Assertions.assertEquals(url, testSocket.getResponseHeaders().get("location"));
        return this;
    }

    public TestExchange assertRedirectTo(String url, String message) {
        assertRedirect(message);
        Assertions.assertEquals(url, testSocket.getResponseHeaders().get("location"), message);
        return this;
    }

    public TestExchange assertNotFound() {
        Assertions.assertEquals(404, testSocket.getResponseStatus());
        return this;
    }

    public TestExchange assertNotFound(String message) {
        Assertions.assertEquals(404, testSocket.getResponseStatus(), message);
        return this;
    }

    public TestExchange assertBadRequest() {
        Assertions.assertEquals(400, testSocket.getResponseStatus());
        return this;
    }

    public TestExchange assertBadRequest(String message) {
        Assertions.assertEquals(400, testSocket.getResponseStatus(), message);
        return this;
    }

    public TestExchange assertForbidden() {
        Assertions.assertEquals(403, testSocket.getResponseStatus());
        return this;
    }

    public TestExchange assertForbidden(String message) {
        Assertions.assertEquals(403, testSocket.getResponseStatus(), message);
        return this;
    }

    public TestExchange assertUnauthorized() {
        Assertions.assertEquals(401, testSocket.getResponseStatus());
        return this;
    }

    public TestExchange assertUnauthorized(String message) {
        Assertions.assertEquals(401, testSocket.getResponseStatus(), message);
        return this;
    }

    public TestExchange assertSuccess() {
        Assertions.assertEquals(200, (testSocket.getResponseStatus() / 100) * 100);
        return this;
    }

    public TestExchange assertSuccess(String message) {
        Assertions.assertEquals(200, (testSocket.getResponseStatus() / 100) * 100, message);
        return this;
    }

    public TestExchange assertError() {
        Assertions.assertTrue(testSocket.getResponseStatus() >= 400);
        return this;
    }

    public TestExchange assertError(String message) {
        Assertions.assertTrue(testSocket.getResponseStatus() >= 400, message);
        return this;
    }

    public TestExchange assertHeader(String key, String value) {
        Assertions.assertEquals(value, testSocket.getResponseHeaders().get(key.toLowerCase(Locale.ROOT)));
        return this;
    }

    public TestExchange assertHeader(String key, String value, String message) {
        Assertions.assertEquals(value, testSocket.getResponseHeaders().get(key.toLowerCase(Locale.ROOT)), message);
        return this;
    }

    public TestExchange assertJsonPath(String path, Object value) {
        Assertions.assertTrue(checkGraph(getPathElement(mockResponseBody(), path), value));
        return this;
    }

    public TestExchange assertJsonPath(String path, Object value, String message) {
        Assertions.assertTrue(checkGraph(getPathElement(mockResponseBody(), path), value), message);
        return this;
    }

    public TestExchange assertJson(Object value) {
        assertJsonPath(null, value);
        return this;
    }

    public TestExchange assertJson(Object value, String message) {
        assertJsonPath(null, value, message);
        return this;
    }

    public TestExchange assertBody(String content) {
        Assertions.assertEquals(content, getOutputString());
        return this;
    }

    public TestExchange assertBody(String content, String message) {
        Assertions.assertEquals(content, getOutputString(), message);
        return this;
    }

    private AbstractElement mockResponseBody() {
        List<String> contentTypes = testSocket.getResponseHeaders().get("content-type");
        MimeType type = MimeType.byMimeType(contentTypes.size() > 0 ? contentTypes.get(0) : null);
        if (type == null)
            type = MimeType.JSON;
        switch (type) {
            default:
                return AbstractElement.fromJson(getOutputString());
            case YAML:
                return AbstractElement.fromYaml(getOutputString(), true);
            case X_WWW_FORM_URLENCODED:
                return AbstractElement.fromFormData(getOutputString());
        }
    }

    private boolean checkGraph(AbstractElement element, Object value) {
        if (value == null)
            return element == null;
        if (element == null)
            return false;
        AbstractElement val = getServer().getAbstractMapper().toAbstract(value);
        if (val.isNull())
            return element.isNull();
        if (val.isObject()) {
            if (!element.isObject())
                return false;
            for (String key : val.object().keys()) {
                if (!element.object().has(key))
                    return false;
                if (!checkGraph(element.object().get(key), val.object().get(key)))
                    return false;
            }
            return true;
        }
        if (val.isArray()) {
            if (!element.isArray())
                return false;
            if (val.array().size() != element.array().size())
                return false;
            for (int i = 0; i < val.array().size(); i++) {
                if (!checkGraph(element.array().get(i), val.array().get(i)))
                    return false;
            }
            return true;
        }
        if (val.isString()) {
            if (!element.isString())
                return false;
            return val.string().equals(element.string());
        }
        if (val.isNumber()) {
            if (!element.isNumber())
                return false;
            return val.number().equals(element.number());
        }
        if (val.isBoolean()) {
            if (!element.isBoolean())
                return false;
            return val.bool() == element.bool();
        }
        return false;
    }

    private boolean isRedirect() {
        List<Integer> redirectCodes = new ArrayList<>();
        redirectCodes.add(301);
        redirectCodes.add(302);
        redirectCodes.add(303);
        redirectCodes.add(307);
        redirectCodes.add(308);

        return redirectCodes.contains(testSocket.getResponseStatus());
    }
}

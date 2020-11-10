package org.javawebstack.httpserver.test;

import com.google.gson.JsonElement;
import org.javawebstack.graph.GraphElement;
import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.HTTPServer;
import org.javawebstack.httpserver.helper.MimeType;
import org.junit.jupiter.api.Assertions;

import java.util.Collection;

public class TestExchange extends Exchange {
    private MockHttpServletRequest mockReq;
    private MockHttpServletResponse mockRes;
    public TestExchange(HTTPServer service, MockHttpServletRequest request, MockHttpServletResponse response) {
        super(service, request, response);
        this.mockReq = request;
        this.mockRes = response;
    }
    public TestExchange print(){
        printResponse();
        return this;
    }

    public MockHttpServletRequest mockRequest() {
        return mockReq;
    }

    public MockHttpServletResponse mockResponse(){
        return mockRes;
    }
    public TestExchange printResponse(){
        System.out.println("HTTP Response "+mockRes.getStatus());
        System.out.println(mockRes.getContentString());
        return this;
    }
    public TestExchange assertStatus(int status){
        Assertions.assertEquals(status, mockRes.getStatus());
        return this;
    }
    public TestExchange assertStatus(int status, String message){
        Assertions.assertEquals(status, mockRes.getStatus(), message);
        return this;
    }
    public TestExchange assertNotStatus(int status){
        Assertions.assertNotEquals(status, mockRes.getStatus());
        return this;
    }
    public TestExchange assertNotStatus(int status, String message){
        Assertions.assertNotEquals(status, mockRes.getStatus(), message);
        return this;
    }
    public TestExchange assertSuccess(){
        Assertions.assertEquals(200, mockRes.getStatus()/100);
        return this;
    }
    public TestExchange assertSuccess(String message){
        Assertions.assertEquals(200, mockRes.getStatus()/100, message);
        return this;
    }
    public TestExchange assertError(){
        Assertions.assertTrue(mockRes.getStatus() >= 400);
        return this;
    }
    public TestExchange assertError(String message){
        Assertions.assertTrue(mockRes.getStatus() >= 400, message);
        return this;
    }
    public TestExchange assertHeader(String key, String value){
        Assertions.assertEquals(value, mockRes.getHeader(key));
        return this;
    }
    public TestExchange assertHeader(String key, String value, String message){
        Assertions.assertEquals(value, mockRes.getHeader(key), message);
        return this;
    }
    public TestExchange assertJsonPath(String path, Object value){
        Assertions.assertTrue(checkGraph(getPathElement(mockResponseBody(mockRes), path), value));
        return this;
    }
    public TestExchange assertJsonPath(String path, Object value, String message){
        Assertions.assertTrue(checkGraph(getPathElement(mockResponseBody(mockRes), path), value), message);
        return this;
    }
    public TestExchange assertJson(Object value){
        assertJsonPath(null, value);
        return this;
    }
    public TestExchange assertJson(Object value, String message){
        assertJsonPath(null, value, message);
        return this;
    }
    public TestExchange assertBody(String content){
        Assertions.assertEquals(content, mockRes.getContentString());
        return this;
    }
    public TestExchange assertBody(String content, String message){
        Assertions.assertEquals(content, mockRes.getContentString(), message);
        return this;
    }
    private GraphElement mockResponseBody(MockHttpServletResponse response){
        MimeType type = MimeType.byMimeType(response.getContentType());
        if(type == null)
            type = MimeType.JSON;
        switch (type){
            default:
                return GraphElement.fromJson(response.getContentString());
            case YAML:
                return GraphElement.fromYaml(response.getContentString(), true);
            case X_WWW_FORM_URLENCODED:
                return GraphElement.fromFormData(response.getContentString());
        }
    }
    private boolean checkGraph(GraphElement element, Object value){
        if(value == null)
            return element == null;
        if(element == null)
            return false;
        GraphElement val = getServer().getGraphMapper().toGraph(value);
        if(val.isNull())
            return element.isNull();
        if(val.isObject()){
            if(!element.isObject())
                return false;
            for(String key : val.object().keys()){
                if(!element.object().has(key))
                    return false;
                if(!checkGraph(element.object().get(key), val.object().get(key)))
                    return false;
            }
            return true;
        }
        if(val.isArray()){
            if(!element.isArray())
                return false;
            if(val.array().size() != element.array().size())
                return false;
            for(int i=0; i<val.array().size(); i++){
                if(!checkGraph(element.array().get(i), val.array().get(i)))
                    return false;
            }
            return true;
        }
        if(val.isString()){
            if(!element.isString())
                return false;
            return val.string().equals(element.string());
        }
        if(val.isNumber()){
            if(!element.isNumber())
                return false;
            return val.number().equals(element.number());
        }
        if(val.isBoolean()){
            if(!element.isBoolean())
                return false;
            return val.bool() == element.bool();
        }
        return false;
    }
}

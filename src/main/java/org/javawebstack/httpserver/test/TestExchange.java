package org.javawebstack.httpserver.test;

import com.google.gson.JsonElement;
import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.WebService;
import org.junit.jupiter.api.Assertions;

public class TestExchange extends Exchange {
    private MockHttpServletRequest mockReq;
    private MockHttpServletResponse mockRes;
    public TestExchange(WebService service, MockHttpServletRequest request, MockHttpServletResponse response) {
        super(service, request, response);
        this.mockReq = request;
        this.mockRes = response;
    }
    public TestExchange print(){
        printResponse();
        return this;
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
        Assertions.assertTrue(checkJson(getPathElement(getService().getGson().fromJson(mockRes.getContentString(), JsonElement.class), path), value));
        return this;
    }
    public TestExchange assertJsonPath(String path, Object value, String message){
        Assertions.assertTrue(checkJson(getPathElement(getService().getGson().fromJson(mockRes.getContentString(), JsonElement.class), path), value), message);
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
    private boolean checkJson(JsonElement element, Object value){
        if(value == null)
            return element == null;
        if(element == null)
            return false;
        JsonElement val = getService().getGson().toJsonTree(value);
        if(val.isJsonNull())
            return element.isJsonNull();
        if(val.isJsonObject()){
            if(!element.isJsonObject())
                return false;
            for(String key : val.getAsJsonObject().keySet()){
                if(!element.getAsJsonObject().has(key))
                    return false;
                if(!checkJson(element.getAsJsonObject().get(key), val.getAsJsonObject().get(key)))
                    return false;
            }
            return true;
        }
        if(val.isJsonArray()){
            if(!element.isJsonArray())
                return false;
            if(val.getAsJsonArray().size() != element.getAsJsonArray().size())
                return false;
            for(int i=0; i<val.getAsJsonArray().size(); i++){
                if(!checkJson(element.getAsJsonArray().get(i), val.getAsJsonArray().get(i)))
                    return false;
            }
            return true;
        }
        if(val.getAsJsonPrimitive().isString()){
            if(!element.getAsJsonPrimitive().isString())
                return false;
            return val.getAsString().equals(element.getAsString());
        }
        if(val.getAsJsonPrimitive().isNumber()){
            if(!element.getAsJsonPrimitive().isNumber())
                return false;
            return val.getAsNumber().equals(element.getAsNumber());
        }
        if(val.getAsJsonPrimitive().isBoolean()){
            if(!element.getAsJsonPrimitive().isBoolean())
                return false;
            return val.getAsBoolean() == element.getAsBoolean();
        }
        return false;
    }
}

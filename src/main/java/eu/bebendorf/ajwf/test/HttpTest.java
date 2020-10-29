package eu.bebendorf.ajwf.test;

import eu.bebendorf.ajwf.WebService;
import eu.bebendorf.ajwf.helper.HttpMethod;

import java.util.HashMap;
import java.util.Map;

public abstract class HttpTest {

    private final WebService service;
    private final Map<String, String> defaultHeaders = new HashMap<>();

    protected HttpTest(WebService service){
        this.service = service;
    }

    public WebService getService(){
        return service;
    }

    public void setDefaultHeader(String key, String value){
        defaultHeaders.put(key, value);
    }

    public void setBearerToken(String token){
        setDefaultHeader("Authorization", "Bearer "+token);
    }

    public TestExchange httpGet(String url){
        return httpRequest(HttpMethod.GET, url, null);
    }

    public TestExchange httpPost(String url){
        return httpPost(url, null);
    }

    public TestExchange httpPost(String url, Object content){
        return httpRequest(HttpMethod.POST, url, content);
    }

    public TestExchange httpPut(String url){
        return httpPut(url, null);
    }

    public TestExchange httpPut(String url, Object content){
        return httpRequest(HttpMethod.PUT, url, content);
    }

    public TestExchange httpDelete(String url){
        return httpDelete(url, null);
    }

    public TestExchange httpDelete(String url, Object content){
        return httpRequest(HttpMethod.DELETE, url, content);
    }

    public TestExchange httpRequest(HttpMethod method, String url, Object content){
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setPath(url);
        if(content != null){
            if(content instanceof String){
                request.setContent((String) content);
            }else if(content instanceof byte[]){
                request.setContent((byte[]) content);
            }else{
                request.setContent(service.getGson().toJson(content));
            }
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestExchange exchange = new TestExchange(service, request, response);
        service.execute(exchange);
        return exchange;
    }

}

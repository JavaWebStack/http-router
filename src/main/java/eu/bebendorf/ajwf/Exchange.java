package eu.bebendorf.ajwf;

import com.google.gson.JsonElement;
import eu.bebendorf.ajwf.helper.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Exchange {
    private final WebService service;
    private final HttpMethod method;
    private final String path;
    private byte[] body = null;
    public Map<String, Object> pathVariables;
    public Map<String, String> parameters = new HashMap<>();
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Map<String, Object> attributes = new HashMap<>();

    public Exchange(WebService service, HttpServletRequest request, HttpServletResponse response){
        this.service = service;
        this.request = request;
        this.response = response;
        this.path = request.getPathInfo();
        method = HttpMethod.valueOf(request.getMethod());
    }

    public <T> T getBody(Class<T> clazz){
        if(body == null)
            body = read();
        if(clazz == byte[].class)
            return (T) body;
        String body = new String(this.body, StandardCharsets.UTF_8);
        if(clazz == String.class)
            return (T) body;
        return service.getGson().fromJson(body, clazz);
    }

    public WebService getService() {
        return service;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getContentType(){
        return request.getContentType();
    }

    public byte[] read(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            InputStream is = request.getInputStream();
            byte [] data = new byte[1024];
            int r;
            while (is.available() > 0){
                r = is.read(data);
                baos.write(data, 0, r);
            }
        }catch(IOException ex){
            throw new RuntimeException(ex);
        }
        return baos.toByteArray();
    }
    public void write(String data){
        write(data.getBytes(StandardCharsets.UTF_8));
    }
    public void write(byte[] bytes){
        try {
            response.getOutputStream().write(bytes);
        }catch (IOException ex){
            throw new RuntimeException(ex);
        }
    }
    public void write(byte[] bytes, int offset, int length){
        try {
            response.getOutputStream().write(bytes, offset, length);
        } catch (IOException ex){
            throw new RuntimeException(ex);
        }
    }
    public void close() {
        try {
            response.getOutputStream().close();
        } catch (IOException ex){
            throw new RuntimeException(ex);
        }
    }
    public void header(String header, String value){
        if(header.equalsIgnoreCase("content-type")){
            response.setContentType(value);
            return;
        }
        response.setHeader(header, value);
    }
    public void status(int code){
        response.setStatus(code);
    }
    public String header(String header){
        return request.getHeader(header);
    }
    public void redirect(String url){
        response.setStatus(302);
        try {
            response.sendRedirect(url);
        }catch (IOException ex){
            throw new RuntimeException(ex);
        }
    }
    public HttpServletRequest rawRequest(){
        return request;
    }
    public HttpServletResponse rawResponse(){
        return response;
    }
    public <T> T attrib(String key){
        if(attributes.get(key) == null)
            return null;
        return (T) attributes.get(key);
    }
    public void attrib(String key, Object value){
        attributes.put(key, value);
    }
    public String bearerAuth(){
        String auth = header("Authorization");
        if(auth == null)
            return null;
        if(!auth.startsWith("Bearer "))
            return null;
        return auth.substring(7);
    }
    public <T> T getBodyPath(String path, Class<T> clazz){
        return service.getGson().fromJson(getBodyPathElement(path), clazz);
    }
    public JsonElement getBodyPathElement(String path){
        return getPathElement(getBody(JsonElement.class), path);
    }
    protected static JsonElement getPathElement(JsonElement source, String path){
        if(source == null || path == null || path.length() == 0)
            return source;
        if(!path.contains(".")){
            if(source.isJsonObject()){
                return source.getAsJsonObject().get(path);
            }else if(source.isJsonArray()){
                return source.getAsJsonArray().get(Integer.parseInt(path));
            }else{
                return null;
            }
        }
        String[] spl = path.split("\\.");
        return getPathElement(getPathElement(source, spl[0]), path.substring(spl[0].length()+1));
    }
}
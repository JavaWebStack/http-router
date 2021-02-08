package org.javawebstack.httpserver;

import com.google.gson.JsonElement;
import org.javawebstack.abstractdata.AbstractElement;
import org.javawebstack.abstractdata.AbstractNull;
import org.javawebstack.httpserver.helper.HttpMethod;
import org.javawebstack.httpserver.helper.MimeType;
import org.javawebstack.validator.ValidationContext;
import org.javawebstack.validator.ValidationException;
import org.javawebstack.validator.ValidationResult;
import org.javawebstack.validator.Validator;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Exchange {

    private final HTTPServer server;
    private final HttpMethod method;
    private final String path;
    private byte[] body = null;
    public Map<String, Object> pathVariables;
    public final Map<String, String> parameters = new HashMap<>();
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Map<String, Object> attributes = new HashMap<>();

    public Exchange(HTTPServer server, HttpServletRequest request, HttpServletResponse response){
        this.server = server;
        this.request = request;
        this.response = response;
        this.path = request.getPathInfo();
        method = "websocket".equalsIgnoreCase(request.getHeader("Upgrade")) ? HttpMethod.WEBSOCKET : HttpMethod.valueOf(request.getMethod());
    }

    public <T> T body(Class<T> clazz){
        if(body == null)
            body = read();
        if(clazz == byte[].class)
            return (T) body;
        String body = new String(this.body, StandardCharsets.UTF_8);
        if(clazz == String.class)
            return (T) body;
        MimeType type = MimeType.byMimeType(getContentType());
        if(type == null)
            type = MimeType.JSON;
        AbstractElement request = AbstractNull.INSTANCE;
        switch (type){
            case JSON:
                request = AbstractElement.fromJson(body);
                break;
            case YAML:
                request = AbstractElement.fromYaml(body, !(clazz.isArray() || Collection.class.isAssignableFrom(clazz)));
                break;
            case X_WWW_FORM_URLENCODED:
                request = AbstractElement.fromFormData(body);
                break;
        }
        ValidationResult result = Validator.getValidator(clazz).validate(new ValidationContext().attrib("exchange", this), request);
        if(!result.isValid())
            throw new ValidationException(result);
        return server.getAbstractMapper().fromAbstract(request, clazz);
    }

    public HTTPServer getServer() {
        return server;
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
    public Exchange write(String data){
        write(data.getBytes(StandardCharsets.UTF_8));
        return this;
    }
    public Exchange write(byte[] bytes){
        try {
            response.getOutputStream().write(bytes);
        }catch (IOException ignored){}
        return this;
    }
    public Exchange write(byte[] bytes, int offset, int length){
        try {
            response.getOutputStream().write(bytes, offset, length);
        } catch (IOException ignored){}
        return this;
    }
    public Exchange write(InputStream stream) throws IOException {
        byte[] buffer = new byte[1024];
        int r;
        while ((r = stream.read(buffer)) != -1)
            write(buffer, 0, r);
        stream.close();
        return this;
    }
    public Exchange close() {
        try {
            response.getOutputStream().close();
        } catch (IOException ignored){}
        return this;
    }
    public Exchange header(String header, String value){
        if(header.equalsIgnoreCase("content-type")){
            response.setContentType(value);
            return this;
        }
        response.setHeader(header, value);
        return this;
    }
    public Exchange status(int code){
        response.setStatus(code);
        return this;
    }
    public String header(String header){
        return request.getHeader(header);
    }

    public Exchange redirect(String url){
        response.setStatus(302);
        try {
            response.sendRedirect(url);
        }catch (IOException ex){
            throw new RuntimeException(ex);
        }
        return this;
    }

    public List<Locale> locales(){
        return Collections.list(request.getLocales());
    }

    public Locale locale(Locale... possible){
        if(possible.length == 0)
            return request.getLocale();
        List<Locale> possibleList = Arrays.asList(possible);
        for(Locale l : locales()){
            if(possibleList.contains(l))
                return l;
        }
        return possible[0];
    }

    public Exchange contentType(MimeType type) {
        return contentType(type != null ? type.getMimeTypes().get(0): null);
    }

    public Exchange contentType(String contentType) {
        if (contentType == null || contentType.equals(""))
            return contentType("text/plain");
        return header("Content-Type", contentType);
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
    public Exchange attrib(String key, Object value){
        attributes.put(key, value);
        return this;
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
        return server.getAbstractMapper().fromAbstract(getBodyPathElement(path), clazz);
    }
    public AbstractElement getBodyPathElement(String path){
        return getPathElement(body(AbstractElement.class), path);
    }
    protected static AbstractElement getPathElement(AbstractElement source, String path){
        if(source == null || path == null || path.length() == 0)
            return source;
        if(!path.contains(".")){
            if(source.isObject()){
                return source.object().get(path);
            }else if(source.isArray()){
                return source.array().get(Integer.parseInt(path));
            }else{
                return null;
            }
        }
        String[] spl = path.split("\\.");
        return getPathElement(getPathElement(source, spl[0]), path.substring(spl[0].length()+1));
    }
    public Exchange enableMultipart(String location){
        enableMultipart(location, -1L);
        return this;
    }

    public Exchange enableMultipart(String location, long maxFileSize){
        request.setAttribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(location, maxFileSize, -1L, 0));
        return this;
    }
}
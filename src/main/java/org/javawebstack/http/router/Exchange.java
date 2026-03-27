package org.javawebstack.http.router;

import org.javawebstack.abstractdata.*;
import org.javawebstack.abstractdata.mapper.Mapper;
import org.javawebstack.http.router.adapter.IHTTPSocket;
import org.javawebstack.http.router.handler.RequestHandler;
import org.javawebstack.http.router.multipart.Part;
import org.javawebstack.http.router.multipart.content.InMemoryCache;
import org.javawebstack.http.router.multipart.content.PartContentCache;
import org.javawebstack.http.router.multipart.content.TmpFolderCache;
import org.javawebstack.http.router.util.HeaderValue;
import org.javawebstack.http.router.util.MimeType;
import org.javawebstack.validator.ValidationContext;
import org.javawebstack.validator.ValidationException;
import org.javawebstack.validator.ValidationResult;
import org.javawebstack.validator.Validator;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Exchange {

    static ThreadLocal<Exchange> exchanges = new ThreadLocal<>();

    public static Exchange current() {
        return exchanges.get();
    }

    private final HTTPRouter router;
    private final HTTPMethod method;
    private byte[] body = null;
    private final Map<String, Object> pathVariables = new HashMap<>();
    private final AbstractObject queryParameters;
    private final IHTTPSocket socket;
    private final Map<String, Object> attributes = new HashMap<>();
    private List<Part> parts = null;
    private RequestHandler finalHandler;

    public Exchange(HTTPRouter router, IHTTPSocket socket) {
        this.router = router;
        this.socket = socket;
        this.method = getRequestMethodFromSocket(socket);
        this.queryParameters = AbstractElement.fromFormData(socket.getRequestQuery()).object();
    }

    public <T> T body(Class<T> clazz) {
        if (body == null)
            body = read();
        if (body == null)
            body = new byte[0];
        if (clazz == byte[].class)
            return (T) body;
        String body = new String(this.body, StandardCharsets.UTF_8);
        if (clazz == String.class)
            return (T) body;
        if (body.length() == 0)
            body = "{}";

        MimeType type = getMimeType();
        if (type == null)
            type = MimeType.JSON;
        AbstractElement request = null;
        boolean arrayLike = clazz.isArray() || Collection.class.isAssignableFrom(clazz);
        switch (type) {
            case JSON:
                request = AbstractElement.fromJson(body);
                break;
            case YAML:
                request = AbstractElement.fromYaml(body, !arrayLike);
                break;
            case X_WWW_FORM_URLENCODED:
                request = AbstractElement.fromFormData(body);
                break;
        }
        if(request == null || request.isNull())
            request = arrayLike ? new AbstractArray() : new AbstractObject();
        ValidationResult result = Validator.getValidator(clazz).validate(new ValidationContext().attrib("exchange", this), request);
        if (!result.isValid())
            throw new ValidationException(result);
        return router.getMapper().map(request, clazz);
    }

    public HTTPRouter getRouter() {
        return router;
    }

    public HTTPMethod getMethod() {
        return method;
    }

    public String getPath() {
        return socket.getRequestPath();
    }

    public String getContentType() {
        String contentType = socket.getRequestHeader("content-type");
        return contentType != null ? contentType : "";
    }

    public byte[] read() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            InputStream is = socket.getInputStream();
            byte[] data = new byte[1024];
            int r;
            while (is.available() > 0) {
                r = is.read(data);
                baos.write(data, 0, r);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return baos.toByteArray();
    }

    public Exchange write(String data) {
        write(data.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    public Exchange write(byte[] bytes) {
        try {
            socket.getOutputStream().write(bytes);
            socket.getOutputStream().flush();
        } catch (IOException ignored) {
        }
        return this;
    }

    public Exchange write(byte[] bytes, int offset, int length) {
        try {
            socket.getOutputStream().write(bytes, offset, length);
            socket.getOutputStream().flush();
        } catch (IOException ignored) {
        }
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
            socket.close();
        } catch (IOException ignored) {
        }
        return this;
    }

    public Exchange header(String header, String value) {
        socket.setResponseHeader(header, value);
        return this;
    }

    public Exchange status(int code) {
        socket.setResponseStatus(code);
        return this;
    }

    public String header(String header) {
        return socket.getRequestHeader(header);
    }

    public Exchange redirect(String url) {
        socket.setResponseStatus(302);
        socket.setResponseHeader("location", url);
        try {
            socket.writeHeaders();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public List<Locale> locales() {
        String locale = socket.getRequestHeader("accept-language");
        if(locale == null)
            return new ArrayList<>();
        return Stream.of(locale.split(" ?,")).map(s -> s.split(";")[0]).map(Locale::forLanguageTag).collect(Collectors.toList());
    }

    public Locale locale(Locale... possible) {
        List<Locale> requested = locales();
        if (possible.length == 0)
            return requested.size() > 0 ? requested.get(0) : null;
        List<Locale> possibleList = Arrays.asList(possible);
        for (Locale l : requested) {
            if (possibleList.contains(l))
                return l;
        }
        return possible[0];
    }

    public Exchange contentType(MimeType type) {
        return contentType(type != null ? type.getMimeTypes().get(0) : null);
    }

    public Exchange contentType(String contentType) {
        if (contentType == null || contentType.equals(""))
            return contentType("text/plain");
        return header("content-type", contentType);
    }

    public IHTTPSocket socket() {
        return socket;
    }

    public <T> T attrib(String key) {
        if (attributes.get(key) == null)
            return null;
        return (T) attributes.get(key);
    }

    public Exchange attrib(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    public <T> T path(String name) {
        return (T) pathVariables.get(name);
    }

    public String query(String name) {
        return queryParameters.string(name, null);
    }

    public String query(String name, String defaultValue) {
        return queryParameters.string(name, defaultValue);
    }

    public <T> T query(String name, Class<T> type) {
        return query(name, type, null);
    }

    public <T> T query(String name, Class<T> type, T defaultValue) {
        T t = new Mapper().map(queryParameters.get(name, AbstractNull.VALUE), type);
        if (t == null)
            return defaultValue;
        return t;
    }

    public String remoteAddr() {
        return socket.getRemoteAddress();
    }

    public Map<String, Object> getPathVariables() {
        return pathVariables;
    }

    public AbstractObject getQueryParameters() {
        return queryParameters;
    }

    public String bearerAuth() {
        String auth = header("Authorization");
        if (auth == null)
            return null;
        if (!auth.startsWith("Bearer "))
            return null;
        return auth.substring(7);
    }

    public <T> T getBodyPath(String path, Class<T> clazz) {
        return router.getMapper().map(getBodyPathElement(path), clazz);
    }

    public AbstractElement getBodyPathElement(String path) {
        return getPathElement(body(AbstractElement.class), path);
    }

    protected static AbstractElement getPathElement(AbstractElement source, String path) {
        if (source == null || path == null || path.length() == 0)
            return source;
        if (!path.contains(".")) {
            if (source.isObject()) {
                return source.object().get(path);
            } else if (source.isArray()) {
                return source.array().get(Integer.parseInt(path));
            } else {
                return null;
            }
        }
        String[] spl = path.split("\\.");
        return getPathElement(getPathElement(source, spl[0]), path.substring(spl[0].length() + 1));
    }

    private HTTPMethod getRequestMethodFromSocket(IHTTPSocket socket) {
        if ("websocket".equalsIgnoreCase(socket.getRequestHeader("upgrade")))
            return HTTPMethod.WEBSOCKET;
        if (router.getRoutingOptions().hasFormMethodParameter() && (socket.getRequestMethod() == HTTPMethod.GET || socket.getRequestMethod() == HTTPMethod.POST) && getMimeType() == MimeType.X_WWW_FORM_URLENCODED) {
            AbstractElement e = getBodyPathElement(router.getRoutingOptions().getFormMethodParameter());
            if (e != null) {
                try {
                    return HTTPMethod.valueOf(e.string());
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return socket.getRequestMethod();
    }

    /**
     * Use with CAUTION! This will load all parts into memory
     * @return
     */
    public Exchange enableMultipart() {
        return enableMultipart(new InMemoryCache());
    }

    /**
     * Use with CAUTION! This will run an expensive cleanup routine on each call and store files on disk
     * @param tmpFolder
     * @return
     */
    public Exchange enableMultipart(File tmpFolder) {
        PartContentCache cache = new TmpFolderCache(tmpFolder);
        cache.cleanup();
        return enableMultipart(cache);
    }

    public Exchange enableMultipart(PartContentCache cache) {
        if(parts != null)
            return this;
        HeaderValue contentType = new HeaderValue(getContentType());
        if(!contentType.getValue().toLowerCase(Locale.ROOT).equals("multipart/form-data"))
            return this;
        byte[] boundary = contentType.getDirectives().get("boundary").getBytes();
        try {
            InputStream stream;
            if(body != null) {
                stream = new ByteArrayInputStream(body);
            } else {
                body = new byte[0];
                stream = socket.getInputStream();
            }
            parts = Part.parse(stream, boundary, cache);
        } catch (Exception ex) {
            parts = new ArrayList<>();
        }
        return this;
    }

    public boolean isMultipart() {
        return parts != null;
    }

    public List<Part> getParts() {
        if(!isMultipart())
            throw new IllegalStateException("This is not a multipart request or multipart parsing is not enabled. Use enableMultipart to enable it or check with isMultipart");
        return parts;
    }

    public Part getPart(String name) {
        return getParts().stream().filter(p -> name.equals(p.getName())).findFirst().orElse(null);
    }

    public MimeType getMimeType() {
        HeaderValue contentType = new HeaderValue(getContentType());
        return MimeType.byMimeType(contentType.getValue().toLowerCase(Locale.ROOT));
    }

    protected Exchange setFinalHandler(RequestHandler finalHandler) {
        this.finalHandler = finalHandler;
        return this;
    }

    public RequestHandler getFinalHandler() {
        return finalHandler;
    }
}

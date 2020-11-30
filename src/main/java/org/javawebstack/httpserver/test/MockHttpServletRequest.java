package org.javawebstack.httpserver.test;

import org.javawebstack.httpserver.helper.HttpMethod;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;

public class MockHttpServletRequest implements HttpServletRequest {

    private MockServletInputStream inputStream = new MockServletInputStream();
    private final Map<String, String> headers = new HashMap<>();
    private String method = "GET";
    private String pathInfo = "/";
    private String queryString = "";

    public void setContent(byte[] content){
        inputStream = new MockServletInputStream(content);
    }

    public void setContent(String content){
        setContent(content.getBytes(StandardCharsets.UTF_8));
    }

    public void addHeader(String key, String value){
        headers.put(key, value);
    }

    public void setMethod(HttpMethod method){
        this.method = method.name();
    }

    public void setPath(String path){
        String[] spl = path.split("\\?");
        this.pathInfo = spl[0];
        queryString = spl.length > 1 ? spl[1] : "";
    }
    
    public String getAuthType() {
        return null;
    }
    
    public Cookie[] getCookies() {
        return new Cookie[0];
    }
    
    public long getDateHeader(String s) {
        return 0;
    }

    public String getHeader(String s) {
        return headers.get(s);
    }

    public Enumeration<String> getHeaders(String s) {
        String header = getHeader(s);
        Set<String> set = new HashSet<>();
        if(header != null)
            set.add(header);
        return Collections.enumeration(set);
    }

    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }
    
    public int getIntHeader(String s) {
        return Integer.parseInt(getHeader(s));
    }
    
    public String getMethod() {
        return method;
    }
    
    public String getPathInfo() {
        return pathInfo;
    }
    
    public String getPathTranslated() {
        return null;
    }
    
    public String getContextPath() {
        return null;
    }
    
    public String getQueryString() {
        return queryString;
    }
    
    public String getRemoteUser() {
        return null;
    }
    
    public boolean isUserInRole(String s) {
        return false;
    }
    
    public Principal getUserPrincipal() {
        return null;
    }
    
    public String getRequestedSessionId() {
        return null;
    }
    
    public String getRequestURI() {
        return null;
    }
    
    public StringBuffer getRequestURL() {
        return null;
    }
    
    public String getServletPath() {
        return null;
    }
    
    public HttpSession getSession(boolean b) {
        return null;
    }
    
    public HttpSession getSession() {
        return null;
    }
    
    public String changeSessionId() {
        return null;
    }
    
    public boolean isRequestedSessionIdValid() {
        return false;
    }
    
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }
    
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }
    
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }
    
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        return false;
    }
    
    public void login(String s, String s1) throws ServletException {

    }
    
    public void logout() throws ServletException {

    }
    
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }
    
    public Part getPart(String s) throws IOException, ServletException {
        return null;
    }
    
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        return null;
    }
    
    public Object getAttribute(String s) {
        return null;
    }
    
    public Enumeration<String> getAttributeNames() {
        return null;
    }
    
    public String getCharacterEncoding() {
        return null;
    }
    
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

    }
    
    public int getContentLength() {
        return inputStream.size();
    }
    
    public long getContentLengthLong() {
        return 0;
    }
    
    public String getContentType() {
        return getHeader("Content-Type");
    }
    
    public ServletInputStream getInputStream() throws IOException {
        return inputStream;
    }
    
    public String getParameter(String s) {
        return null;
    }
    
    public Enumeration<String> getParameterNames() {
        return null;
    }
    
    public String[] getParameterValues(String s) {
        return new String[0];
    }
    
    public Map<String, String[]> getParameterMap() {
        return null;
    }
    
    public String getProtocol() {
        return null;
    }
    
    public String getScheme() {
        return null;
    }
    
    public String getServerName() {
        return null;
    }
    
    public int getServerPort() {
        return 80;
    }
    
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(inputStream));
    }
    
    public String getRemoteAddr() {
        return "127.0.0.1";
    }
    
    public String getRemoteHost() {
        return "localhost";
    }
    
    public void setAttribute(String s, Object o) {

    }
    
    public void removeAttribute(String s) {

    }
    
    public Locale getLocale() {
        return Locale.ENGLISH;
    }
    
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(Collections.singletonList(getLocale()));
    }
    
    public boolean isSecure() {
        return false;
    }
    
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }
    
    public String getRealPath(String s) {
        return null;
    }

    public int getRemotePort() {
        return 0;
    }

    public String getLocalName() {
        return "localhost";
    }

    public String getLocalAddr() {
        return "127.0.0.1";
    }

    public int getLocalPort() {
        return 80;
    }

    public ServletContext getServletContext() {
        return null;
    }

    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    public boolean isAsyncStarted() {
        return false;
    }

    public boolean isAsyncSupported() {
        return false;
    }

    public AsyncContext getAsyncContext() {
        return null;
    }

    public DispatcherType getDispatcherType() {
        return null;
    }

}

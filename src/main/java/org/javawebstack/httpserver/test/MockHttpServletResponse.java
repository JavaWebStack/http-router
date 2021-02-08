package org.javawebstack.httpserver.test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class MockHttpServletResponse implements HttpServletResponse {

    private int status = 200;
    private final MockServletOutputStream outputStream = new MockServletOutputStream();
    private final Map<String, String> headers = new HashMap<>();

    public void addCookie(Cookie cookie) {

    }

    public boolean containsHeader(String s) {
        return headers.containsKey(s);
    }

    public String encodeURL(String s) {
        return null;
    }

    public String encodeRedirectURL(String s) {
        return null;
    }

    public String encodeUrl(String s) {
        return null;
    }

    public String encodeRedirectUrl(String s) {
        return null;
    }

    public void sendError(int i, String s) throws IOException {
        sendError(i);
    }

    public void sendError(int i) throws IOException {
        setStatus(i);
        outputStream.close();
    }

    public void sendRedirect(String s) throws IOException {
        setStatus(302);
        setHeader("Location", s);
    }

    public void setDateHeader(String s, long l) {

    }

    public void addDateHeader(String s, long l) {

    }

    public void setHeader(String s, String s1) {
        headers.put(s, s1);
    }

    public void addHeader(String s, String s1) {
        headers.put(s, s1);
    }

    public void setIntHeader(String s, int i) {
        setHeader(s, String.valueOf(i));
    }

    public void addIntHeader(String s, int i) {
        addHeader(s, String.valueOf(i));
    }

    public void setStatus(int i) {
        this.status = i;
    }

    public void setStatus(int i, String s) {
        setStatus(i);
    }

    public int getStatus() {
        return status;
    }

    public String getHeader(String s) {
        return headers.get(s);
    }

    public Collection<String> getHeaders(String s) {
        String header = getHeader(s);
        Set<String> set = new HashSet<>();
        if (header != null)
            set.add(header);
        return set;
    }

    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    public String getCharacterEncoding() {
        return null;
    }

    public String getContentType() {
        return getHeader("Content-Type");
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(outputStream);
    }

    public void setCharacterEncoding(String s) {

    }

    public void setContentLength(int i) {
        setIntHeader("Content-Length", i);
    }

    public void setContentLengthLong(long l) {
        setHeader("Content-Length", String.valueOf(l));
    }

    public void setContentType(String s) {
        setHeader("Content-Type", s);
    }

    public void setBufferSize(int i) {

    }

    public int getBufferSize() {
        return 0;
    }

    public void flushBuffer() throws IOException {

    }

    public void resetBuffer() {

    }

    public boolean isCommitted() {
        return false;
    }

    public void reset() {

    }

    public void setLocale(Locale locale) {

    }

    public Locale getLocale() {
        return null;
    }

    public byte[] getContent() {
        return outputStream.getBytes();
    }

    public String getContentString() {
        return outputStream.getString();
    }

}

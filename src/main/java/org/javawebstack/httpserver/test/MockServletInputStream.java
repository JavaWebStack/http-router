package org.javawebstack.httpserver.test;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class MockServletInputStream extends ServletInputStream {

    private final int size;
    private final ByteArrayInputStream inputStream;

    public MockServletInputStream(byte[] bytes){
        inputStream = new ByteArrayInputStream(bytes);
        size = bytes.length;
    }

    public MockServletInputStream(){
        this(new byte[0]);
    }

    public boolean isFinished() {
        return inputStream.available() == 0;
    }

    public boolean isReady() {
        return true;
    }

    public void setReadListener(ReadListener readListener) {

    }

    public int read() throws IOException {
        return inputStream.read();
    }

    public int size(){
        return size;
    }

}

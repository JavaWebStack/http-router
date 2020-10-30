package org.javawebstack.httpserver.test;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class MockServletOutputStream extends ServletOutputStream {

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public boolean isReady() {
        return true;
    }

    public void setWriteListener(WriteListener writeListener) {

    }

    public void write(int b) {
        buffer.write(b);
    }

    public byte[] getBytes(){
        return buffer.toByteArray();
    }

    public String getString(){
        return new String(getBytes(), StandardCharsets.UTF_8);
    }

}

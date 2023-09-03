package org.javawebstack.http.router.multipart.content;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ByteArrayContent implements PartContent {

    private byte[] data;

    public ByteArrayContent(byte[] data) {
        this.data = data;
    }

    public InputStream read() {
        if(data == null)
            throw new RuntimeException("Data has already been discarded");
        return new ByteArrayInputStream(data);
    }

    public void discard() {
        data = null;
    }

}

package org.javawebstack.httpserver.adapter.undertow;

import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class StreamSourceInputStream extends InputStream {

    private final StreamSourceChannel source;
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(1);

    public StreamSourceInputStream(StreamSourceChannel source) {
        this.source = source;
    }

    public synchronized int read() throws IOException {
        byteBuffer.position(0);
        int r;
        while ((r = source.read(byteBuffer)) == 0)
            Thread.yield();
        if(r == -1)
            return -1;
        int b = byteBuffer.position(0).get();
        return b;
    }

}

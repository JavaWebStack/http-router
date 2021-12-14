package org.javawebstack.httpserver.adapter.undertow;

import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class StreamSinkOutputStream extends OutputStream {

    private final StreamSinkChannel sink;
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(1);

    public StreamSinkOutputStream(StreamSinkChannel sink) {
        this.sink = sink;
    }

    public void write(int i) throws IOException {
        byteBuffer.position(0);
        byteBuffer.put((byte) i);
        byteBuffer.position(0);
        sink.write(byteBuffer);
    }

    public void close() throws IOException {
        sink.close();
    }

}

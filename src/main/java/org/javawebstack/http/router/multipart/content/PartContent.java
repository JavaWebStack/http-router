package org.javawebstack.http.router.multipart.content;

import java.io.InputStream;

public interface PartContent {

    InputStream read();
    void discard();

}

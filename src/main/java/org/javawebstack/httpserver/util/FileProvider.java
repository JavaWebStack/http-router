package org.javawebstack.httpserver.util;

import java.io.InputStream;

public interface FileProvider {

    InputStream getFile(String path);

}

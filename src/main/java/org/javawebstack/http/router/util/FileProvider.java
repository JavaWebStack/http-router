package org.javawebstack.http.router.util;

import java.io.InputStream;

public interface FileProvider {

    InputStream getFile(String path);

}

package org.javawebstack.httpserver.helper;

import org.eclipse.jetty.util.log.AbstractLogger;
import org.eclipse.jetty.util.log.Logger;

public class JettyNoLog extends AbstractLogger implements Logger {

    protected Logger newLogger(String s) {
        return this;
    }

    public String getName() {
        return "NoLog";
    }

    public void warn(String s, Object... objects) {

    }

    public void warn(Throwable throwable) {

    }

    public void warn(String s, Throwable throwable) {

    }

    public void info(String s, Object... objects) {

    }

    public void info(Throwable throwable) {

    }

    public void info(String s, Throwable throwable) {

    }

    public boolean isDebugEnabled() {
        return false;
    }

    public void setDebugEnabled(boolean b) {

    }

    public void debug(String s, Object... objects) {

    }

    public void debug(Throwable throwable) {

    }

    public void debug(String s, Throwable throwable) {

    }

    public void ignore(Throwable throwable) {

    }

}

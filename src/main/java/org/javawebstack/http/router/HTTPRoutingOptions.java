package org.javawebstack.http.router;

public class HTTPRoutingOptions {

    private boolean caseInsensitive;
    private boolean ignoreTrailingSlash = true;
    private String formMethodParameter = "_method";

    public HTTPRoutingOptions formMethodParameter(String formMethodParameter) {
         this.formMethodParameter = formMethodParameter;
         return this;
    }

    public HTTPRoutingOptions caseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
        return this;
    }

    public HTTPRoutingOptions ignoreTrailingSlash(boolean ignoreTrailingSlash) {
        this.ignoreTrailingSlash = ignoreTrailingSlash;
        return this;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public boolean isIgnoreTrailingSlash() {
        return ignoreTrailingSlash;
    }

    public String getFormMethodParameter() {
        return formMethodParameter;
    }

    public boolean hasFormMethodParameter() {
        return formMethodParameter != null;
    }

}

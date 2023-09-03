package org.javawebstack.http.router.util;

import java.util.HashMap;
import java.util.Map;

public class HeaderValue {

    final String value;
    final Map<String, String> directives = new HashMap<>();

    public HeaderValue(String source) {
        String[] spl = source.split("; ");
        value = spl[0];
        for(int i=1; i<spl.length; i++) {
            String[] spl2 = spl[i].split("=", 2);
            String value = spl2.length == 2 ? spl2[1] : "";
            if(value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length()-1);
            }
            directives.put(spl2[0], value);
        }
    }

    public String getValue() {
        return value;
    }

    public Map<String, String> getDirectives() {
        return directives;
    }

}

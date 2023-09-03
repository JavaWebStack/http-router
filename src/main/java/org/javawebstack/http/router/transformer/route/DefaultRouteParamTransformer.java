package org.javawebstack.http.router.transformer.route;

import java.util.UUID;

public class DefaultRouteParamTransformer extends RouteParamTransformer {
    public static final RouteParamTransformer INSTANCE = new DefaultRouteParamTransformer();

    protected DefaultRouteParamTransformer() {
        add("*|any", ".*", (e, s) -> s);
        add("*+|any+", ".+", (e, s) -> s);
        add("s|string", "[^/]+", (e, s) -> s);
        add("short", "\\-?[0-9]+", (e, s) -> Short.parseShort((String) s));
        add("i|int|integer", "\\-?[0-9]+", (e, s) -> Integer.parseInt((String) s));
        add("i*|int*|integer*", "[0-9]+", (e, s) -> Integer.parseInt((String) s));
        add("i+|int+|integer+", "[1-9][0-9]*", (e, s) -> Integer.parseInt((String) s));
        add("i-|int-|integer-", "\\-[1-9][0-9]*", (e, s) -> Integer.parseInt((String) s));
        add("l|long", "\\-?[0-9]+", (e, s) -> Long.parseLong((String) s));
        add("l*|long*", "[0-9]+", (e, s) -> Long.parseLong((String) s));
        add("l+|long+", "[1-9][0-9]*", (e, s) -> Long.parseLong((String) s));
        add("l-|long-", "\\-[1-9][0-9]*", (e, s) -> Long.parseLong((String) s));
        add("f|float", "\\-?[0-9]+(\\.[0-9]*)?", (e, s) -> Float.parseFloat((String) s));
        add("d|double", "\\-?[0-9]+(\\.[0-9]*)?", (e, s) -> Double.parseDouble((String) s));
        add("b|bool|boolean", "([Tt]rue|[Ff]alse|0|1)", (e, s) -> ((String) s).equalsIgnoreCase("true") || s.equals("1"));
        add("uuid", "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", (e, s) -> UUID.fromString((String) s));
    }
}

package org.javawebstack.http.router.transformer.route;

import java.util.List;

public interface RouteParamTransformerProvider {
    List<RouteParamTransformer> getRouteParamTransformer();

    default RouteParamTransformer getRouteParamTransformer(String type) {
        for (RouteParamTransformer t : getRouteParamTransformer()) {
            if (t.canTransform(type)) {
                return t;
            }
        }
        return null;
    }
}
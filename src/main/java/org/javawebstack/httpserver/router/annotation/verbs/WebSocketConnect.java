package org.javawebstack.httpserver.router.annotation.verbs;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(WebSocketConnect.Multiple.class)
public @interface WebSocketConnect {
    String value() default "/";

    String name() default "";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Multiple {
        WebSocketConnect[] value();
    }
}

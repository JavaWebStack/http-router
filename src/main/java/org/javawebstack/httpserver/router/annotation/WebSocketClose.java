package org.javawebstack.httpserver.router.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(WebSocketClose.Multiple.class)
public @interface WebSocketClose {
    String value() default "/";
    String name() default "";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Multiple {
        WebSocketClose[] value();
    }
}

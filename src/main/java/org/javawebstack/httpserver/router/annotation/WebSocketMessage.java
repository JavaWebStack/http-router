package org.javawebstack.httpserver.router.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(WebSocketMessage.Multiple.class)
public @interface WebSocketMessage {
    String value() default "/";
    String name() default "";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Multiple {
        WebSocketMessage[] value();
    }
}

package org.javawebstack.httpserver.router.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Put.Multiple.class)
public @interface Put {
    String value() default "/";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Multiple {
        Put[] value();
    }

}

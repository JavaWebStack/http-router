package org.javawebstack.httpserver.router.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Delete.Multiple.class)
public @interface Delete {
    String value() default "/";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Multiple {
        Delete[] value();
    }

}

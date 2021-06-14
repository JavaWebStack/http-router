package org.javawebstack.httpserver.router.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Patch.Multiple.class)
public @interface Patch {
    String value() default "/";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Multiple {
        Patch[] value();
    }

}

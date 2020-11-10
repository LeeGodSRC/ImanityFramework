package org.imanity.framework;

import org.imanity.framework.cache.Unless;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CachePut {

    String value();

    boolean ignoreKeyNull() default false;

    boolean asyncUpdate() default false;

    Class<? extends Unless>[] unless() default { };

    int lifetime() default 1;

    TimeUnit unit() default TimeUnit.MINUTES;

    boolean forever() default false;

}
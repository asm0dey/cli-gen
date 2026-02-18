package com.github.asm0dey.cligen.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Parameters {
    int index() default -1;
    String description() default "";
    String arity() default "";
    boolean required() default true;
}

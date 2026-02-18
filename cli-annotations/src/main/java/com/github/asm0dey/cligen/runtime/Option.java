package com.github.asm0dey.cligen.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Option {
    String[] names();
    String description() default "";
    boolean required() default false;
    String defaultValue() default "";
    String arity() default "";
    /**
     * Optional custom converter class used to convert the option's String value
     * into the target field type. The converter class must implement the
     * {@link com.github.asm0dey.cligen.runtime.Converter} interface and have a
     * public no-arg constructor. If not specified, built-in conversions
     * (int, long, boolean, etc.) and plain String assignment are used.
     */
    Class<?> converter() default java.lang.Void.class;
}

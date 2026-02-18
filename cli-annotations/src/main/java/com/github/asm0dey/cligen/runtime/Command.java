package com.github.asm0dey.cligen.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Command {
    String name();
    String description() default "";
    String version() default "";
    boolean mixinStandardHelpOptions() default false;
    Class<?>[] subcommands() default {};
}

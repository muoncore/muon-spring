package io.muoncore.newton.query;

import io.muoncore.newton.AggregateRoot;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NewtonView {

    String[] streams() default {};

    Class<? extends AggregateRoot>[] aggregateRoot()  default {};
}
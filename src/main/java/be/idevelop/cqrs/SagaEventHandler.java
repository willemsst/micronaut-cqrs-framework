package be.idevelop.cqrs;

import io.micronaut.aop.Adapter;
import io.micronaut.core.annotation.Indexed;
import io.micronaut.core.annotation.Introspected;

import java.lang.annotation.*;

@Documented
@Introspected
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Adapter(CqrsSagaEventHandler.class)
@Indexed(CqrsSagaEventHandler.class)
public @interface SagaEventHandler {
    String state();

    Class<? extends Record> event();
}

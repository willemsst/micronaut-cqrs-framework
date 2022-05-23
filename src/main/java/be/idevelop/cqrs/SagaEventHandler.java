package be.idevelop.cqrs;

import io.micronaut.context.annotation.Executable;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target({METHOD})
@Executable
public @interface SagaEventHandler {

    Class<?> event();
}

package be.idevelop.cqrs;

import io.micronaut.core.annotation.Introspected;
import jakarta.inject.Singleton;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target({TYPE})
@Introspected
@Singleton
public @interface SagaDefinition {

    Class<? extends Saga<?>> saga();
}

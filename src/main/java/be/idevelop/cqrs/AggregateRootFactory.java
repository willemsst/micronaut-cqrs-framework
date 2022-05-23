package be.idevelop.cqrs;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.beans.BeanIntrospection;
import jakarta.inject.Inject;

@Factory
final class AggregateRootFactory {

    @Inject
    private ApplicationContext applicationContext;

    @Bean
    <I extends Id<A, I>, A extends AggregateRoot<A, I>> A createNewAggregateRootInstance(I objectId, Class<A> clazz) {
        BeanIntrospection<A> introspection = BeanIntrospection.getIntrospection(clazz);
        //noinspection unchecked
        return (A) introspection.instantiate(objectId).setApplicationContext(applicationContext);
    }
}

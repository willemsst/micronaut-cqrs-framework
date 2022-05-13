package be.idevelop.cqrs;

import io.micronaut.core.annotation.Introspected;

@Introspected
public interface Id<A extends Entity<A, I>, I extends Id<A, I>> {

    Class<A> getEntityClass();

    String asString();
}

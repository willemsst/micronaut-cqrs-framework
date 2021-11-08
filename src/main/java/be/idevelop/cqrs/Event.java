package be.idevelop.cqrs;

import io.micronaut.core.annotation.Introspected;

@Introspected
public interface Event<I extends Id<? extends Entity<?, I>, I>> {

}

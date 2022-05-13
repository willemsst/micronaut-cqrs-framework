package be.idevelop.cqrs;

import io.micronaut.core.annotation.Introspected;

@Introspected
public interface Entity<THIS extends Entity<THIS, I>, I extends Id<THIS, I>> {
}

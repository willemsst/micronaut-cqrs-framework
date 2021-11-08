package be.idevelop.cqrs;

import io.micronaut.core.annotation.Indexed;

import java.util.EventListener;

@Indexed(value = CqrsEventHandler.class)
public interface CqrsEventHandler<I extends Id<A, I>, A extends AggregateRoot<A, I>, E extends Event<I>> extends EventListener {

    void onEvent(A aggregateRoot, E event);

}

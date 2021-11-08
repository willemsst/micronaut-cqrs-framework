package be.idevelop.cqrs;

import io.micronaut.core.annotation.Indexed;

import java.util.EventListener;

@Indexed(value = CqrsCommandHandler.class)
public interface CqrsCommandHandler<I extends Id<A, I>, A extends AggregateRoot<A, I>, C extends Command<I>> extends EventListener {

    A onCommand(A aggregateRoot, C command);

}

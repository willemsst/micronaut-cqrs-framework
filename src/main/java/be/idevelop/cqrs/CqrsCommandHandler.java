package be.idevelop.cqrs;

import io.micronaut.core.annotation.Indexed;
import reactor.core.publisher.Mono;

import java.util.EventListener;

@Indexed(value = CqrsCommandHandler.class)
public interface CqrsCommandHandler<I extends Id<A, I>, A extends AggregateRoot<A, I>, C extends Command<I>> extends EventListener {

    void onCommand(A aggregateRoot, C command);

}

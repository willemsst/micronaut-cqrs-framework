package be.idevelop.cqrs;

import reactor.core.publisher.Mono;

import java.util.List;

public interface EventBus {

    <I extends Id<A, I>, A extends AggregateRoot<A, I>>
    Mono<Boolean> publish(List<EventMessage<I>> eventMessages);
}

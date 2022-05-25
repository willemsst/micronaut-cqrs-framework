package be.idevelop.cqrs;

import reactor.core.publisher.Mono;

import java.util.List;

public interface EventBus {

    <I extends Id<A, I>, A extends AggregateRoot<A, I>, EM extends EventMessage<I, ? extends Record>> Mono<Boolean> publish(List<EM> eventMessages);
}

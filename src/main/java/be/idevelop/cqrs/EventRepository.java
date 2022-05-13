package be.idevelop.cqrs;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EventRepository {

    <I extends Id<A, I>, A extends AggregateRoot<A, I>, E extends Event<I>> Flux<EventMessage<I, E>> retrieveEventMessages(I objectId, Class<A> claßß);

    <I extends Id<A, I>, A extends AggregateRoot<A, I>> Mono<Boolean> saveEventMessages(List<EventMessage<I, ? extends Event<I>>> eventMessages, Class<A> claßß);
}

package be.idevelop.cqrs;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EventRepository {

    <I extends Id<A, I>, A extends AggregateRoot<A, I>> Flux<EventMessage<I, ? extends Record>> retrieveEventMessages(I objectId, Class<A> clazz);

    <I extends Id<A, I>, A extends AggregateRoot<A, I>> Mono<Boolean> saveEventMessages(List<EventMessage<I, ? extends Record>> eventMessages, Class<A> clazz);
}

package be.idevelop.cqrs;

import reactor.core.publisher.Mono;

public interface CommandBus {

    <I extends Id<A, I>, A extends AggregateRoot<A, I>> void publish(Command<I> command);

    <I extends Id<A, I>, A extends AggregateRoot<A, I>> Mono<I> publishAndWait(Command<I> command);
}

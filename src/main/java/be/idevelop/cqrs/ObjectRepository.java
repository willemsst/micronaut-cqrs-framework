package be.idevelop.cqrs;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

@Singleton
final class ObjectRepository {

    @Inject
    private EventRepository eventRepository;

    @Inject
    private EventBus eventBus;

    @Inject
    private AggregateRootFactory aggregateRootFactory;

    <I extends Id<A, I>, A extends AggregateRoot<A, I>> Mono<A> retrieve(I objectId, Class<A> claßß) {
        return this.eventRepository.retrieveEventMessages(objectId, claßß)
                .collect(() -> aggregateRootFactory.createNewAggregateRootInstance(objectId, claßß), AggregateRoot::replay)
                .switchIfEmpty(Mono.fromSupplier(() -> aggregateRootFactory.createNewAggregateRootInstance(objectId, claßß)));
    }

    <I extends Id<A, I>, A extends AggregateRoot<A, I>> Mono<A> save(A aggregateRoot) {
        var eventMessages = aggregateRoot.eventMessages();
        return this.eventRepository.saveEventMessages(eventMessages)
                .filter(success -> success)
                .flatMap(success -> this.eventBus.publish(eventMessages))
                .filter(success -> success)
                .map(success -> aggregateRoot.markSaved());
    }
}

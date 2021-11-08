package be.idevelop.cqrs;

import io.micronaut.core.beans.BeanIntrospection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
public abstract class AbstractSagaStore implements SagaStore {

    @Override
    public <I extends Id<A, I>, A extends AggregateRoot<A, I>, S extends Saga<S>> Flux<S> findAssociatedSagas(I id) {
        return doFindAssociatedSagas(id).flatMap(AbstractSagaStore::rebuildSaga);
    }

    private static <I extends Id<A, I>, A extends AggregateRoot<A, I>, S extends Saga<S>> Mono<S> rebuildSaga(SagaData sagaData) {
        return Mono.fromCallable(() ->
                        (S) BeanIntrospection.getIntrospection(Class.forName(sagaData.sagaClassName)).instantiate(sagaData.id, sagaData.created)
                )
                .onErrorResume(e -> Mono.empty())
                .doOnSuccess(saga -> saga.replayEvents(sagaData.handledEvents));
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void storeSaga(Saga saga) {
        doStore(new SagaData(saga.getSagaId(), saga.isLive(), saga.getAssociatedEntities(), saga.getCurrentVersion(), saga.getCreated(), saga.getScheduledTimeout(), saga.getClass().getName(), saga.getEventsToStore()));
    }

    static record SagaData(SagaId id, boolean live, Set<Id> associatedEntities, int currentVersion,
                           Instant created, Instant scheduledTimeout, String sagaClassName,
                           List<Saga.HandledEvent> handledEvents) {
    }

    public abstract Flux<SagaData> doFindAssociatedSagas(Id id);

    public abstract void doStore(SagaData sagaData);
}

package be.idevelop.cqrs;

import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.serde.annotation.Serdeable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unchecked")
public abstract class AbstractSagaStore implements SagaStore {

    @Override
    public <I extends Id<A, I>, A extends AggregateRoot<A, I>, S extends Saga<S>> Flux<S> findAssociatedSagas(I id) {
        return doFindAssociatedSagas(id).flatMap(AbstractSagaStore::rebuildSaga);
    }

    private static <I extends Id<A, I>, A extends AggregateRoot<A, I>, S extends Saga<S>> Mono<S> rebuildSaga(SagaData sagaData) {
        return Flux.fromIterable(
                        BeanIntrospector.SHARED.findIntrospections(beanIntrospectionReference -> Objects.equals(beanIntrospectionReference.getBeanType().getName(), sagaData.sagaClassName))
                )
                .take(1).singleOrEmpty()
                .map(introspection -> (S) introspection.instantiate(
                        sagaData.id,
                        sagaData.created
                ))
                .onErrorResume(e -> Mono.empty())
                .doOnSuccess(saga -> saga.setCurrentState(sagaData.sagaState))
                .doOnSuccess(saga -> saga.hydrateFieldDataFromJson(sagaData.fieldData));
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void storeSaga(Saga saga) {
        doStore(
                new SagaData(
                        saga.getSagaId(),
                        saga.getClass().getName(),
                        saga.getCreated(),
                        saga.getCurrentState(),
                        saga.getAssociatedEntities(),
                        saga.getScheduledTimeout(),
                        saga.getFieldDataAsJson()
                )
        );
    }

    @Serdeable
    record SagaData(
            SagaId id,
            String sagaClassName,
            Instant created,
            SagaState sagaState,
            Set<Id> associatedEntities,
            Instant scheduledTimeout,
            String fieldData
    ) {
    }

    public abstract Flux<SagaData> doFindAssociatedSagas(Id id);

    public abstract void doStore(SagaData sagaData);
}

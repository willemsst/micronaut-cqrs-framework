package be.idevelop.cqrs;

import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.micronaut.serde.annotation.Serdeable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unchecked")
public abstract class AbstractSagaStore implements SagaStore {

    private final JsonMapper jsonMapper;

    protected AbstractSagaStore(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public <I extends Id<A, I>, A extends AggregateRoot<A, I>, S extends Saga<S>> Flux<S> findAssociatedSagas(I id, Class<S> sagaClass) {
        return doFindAssociatedSagas(id, sagaClass)
                .flatMap(this::rebuildSaga);
    }

    private <I extends Id<A, I>, A extends AggregateRoot<A, I>, S extends Saga<S>> Mono<S> rebuildSaga(SagaData sagaData) {
        return Flux.fromIterable(
                        BeanIntrospector.SHARED.findIntrospections(beanIntrospectionReference -> Objects.equals(beanIntrospectionReference.getBeanType().getName(), sagaData.sagaClassName))
                )
                .take(1).singleOrEmpty()
                .map(introspection -> (S) introspection.instantiate(
                        sagaData.id,
                        sagaData.created
                ))
                .onErrorResume(e -> Mono.empty())
                .doOnNext(saga -> saga.setCurrentState(sagaData.sagaState))
                .doOnNext(saga -> {
                    try {
                        saga.hydrateFieldData(jsonMapper.readValue(sagaData.fieldData(), Argument.mapOf(String.class, Object.class)));
                    } catch (IOException e) {
                        throw new RuntimeException("Could not deserialize Saga field data from BSON", e);
                    }
                });
    }

    @Override
    public <S extends Saga<S>> void storeSaga(S saga) {
        try {
            doStore(
                    new SagaData(
                            saga.getSagaId(),
                            saga.getClass().getName(),
                            saga.getCreated(),
                            saga.getCurrentState(),
                            saga.getLinkedEntities(),
                            saga.getScheduledTimeout(),
                            jsonMapper.writeValueAsBytes(saga.getFieldData())
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException("Could not serialize Saga field data to BSON", e);
        }
    }

    @Override
    public <S extends Saga<S>> void deleteSaga(S saga) {
        doDeleteSaga(saga.getSagaId());
    }

    @SuppressWarnings("rawtypes")
    @Serdeable
    record SagaData(
            SagaId id,
            String sagaClassName,
            Instant created,
            SagaState sagaState,
            Set<Id> associatedEntities,
            Instant scheduledTimeout,
            byte[] fieldData
    ) {

    }
    protected abstract <I extends Id<A, I>, A extends AggregateRoot<A, I>, S extends Saga<S>> Flux<SagaData> doFindAssociatedSagas(I id, Class<S> sagaClass);

    protected abstract void doStore(SagaData sagaData);

    protected abstract <S extends Saga<S>> void doDeleteSaga(SagaId<S> sagaId);
}

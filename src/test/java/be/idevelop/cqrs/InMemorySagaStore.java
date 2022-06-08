package be.idevelop.cqrs;

import io.micronaut.json.JsonMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Singleton
public class InMemorySagaStore extends AbstractSagaStore {
    private final Set<SagaData> sagas = new HashSet<>();

    @Inject
    public InMemorySagaStore(JsonMapper jsonMapper) {
        super(jsonMapper);
    }

    @Override
    protected <I extends Id<A, I>, A extends AggregateRoot<A, I>, S extends Saga<S>> Flux<SagaData> doFindAssociatedSagas(I id, Class<S> sagaClass) {
        return Flux.fromIterable(sagas)
                .filter(sagaData -> Objects.equals(sagaData.sagaClassName(), sagaClass.getName()))
                .filter(sagaData -> sagaData.associatedEntities().contains(id));
    }

    @Override
    protected void doStore(SagaData sagaData) {
        this.sagas.add(sagaData);
    }

    @Override
    protected <S extends Saga<S>> void doDeleteSaga(SagaId<S> sagaId) {
        sagas.removeIf(x -> sagaId.equals(x.id()));
    }
}

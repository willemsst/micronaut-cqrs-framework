package be.idevelop.cqrs;

import reactor.core.publisher.Flux;

public interface SagaStore {

    <I extends Id<A, I>, A extends AggregateRoot<A, I>, S extends Saga<S>> Flux<S> findAssociatedSagas(I id);

    <S extends Saga<S>> void storeSaga(S saga);
}

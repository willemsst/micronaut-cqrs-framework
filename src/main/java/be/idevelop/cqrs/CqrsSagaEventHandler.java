package be.idevelop.cqrs;

import io.micronaut.core.annotation.Introspected;

@Introspected
public interface CqrsSagaEventHandler<S extends Saga<S>, I extends Id<?, I>, EVENT extends Record> {
    SagaState onEvent(S saga, EventMeta<I> meta, EVENT event);
}

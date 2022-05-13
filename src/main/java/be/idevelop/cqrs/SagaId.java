package be.idevelop.cqrs;

import java.util.UUID;

public record SagaId<S extends Saga<S>>(UUID value, Class<S> sagaClass) implements Id<S, SagaId<S>> {

    static <S extends Saga<S>> SagaId<S> createNew(Class<S> sagaClass) {
        return new SagaId<>(UUID.randomUUID(), sagaClass);
    }

    @Override
    public Class<S> getEntityClass() {
        return sagaClass;
    }

    @Override
    public String asString() {
        return value.toString();
    }
}

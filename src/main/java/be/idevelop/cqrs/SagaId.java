package be.idevelop.cqrs;

import java.util.UUID;

import static java.text.MessageFormat.format;

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

    @Override
    public String toString() {
        return format("[{0}.{1}]", sagaClass.getSimpleName(), value);
    }
}

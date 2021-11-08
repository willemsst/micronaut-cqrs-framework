package be.idevelop.cqrs;

import io.micronaut.core.annotation.Introspected;

@Introspected
public interface Command<I extends Id<?, I>> {

    CommandMeta<I> meta();

    static <ID extends Id<?, ID>> CommandMeta<ID> createMeta(ID id) {
        return new CommandMeta<>(id);
    }

    default I objectId() {
        return meta().objectId();
    }
}

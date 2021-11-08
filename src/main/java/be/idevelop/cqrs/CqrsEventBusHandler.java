package be.idevelop.cqrs;

import io.micronaut.core.annotation.Indexed;

import java.util.EventListener;

@Indexed(value = CqrsEventBusHandler.class)
public interface CqrsEventBusHandler<I extends Id<?, I>, M extends EventMeta<I>, E extends Event<I>> extends EventListener {

    void onEvent(E event, M metadata);

}

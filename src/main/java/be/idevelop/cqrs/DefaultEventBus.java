package be.idevelop.cqrs;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.Qualifier;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

final class DefaultEventBus implements EventBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventBus.class);

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    SagaRepository sagaRepository;

    @Override
    public <I extends Id<A, I>, A extends AggregateRoot<A, I>> Mono<Boolean> publish(List<EventMessage<I, ? extends Event<I>>> eventMessages) {
        return Flux.fromIterable(eventMessages)
                .flatMap(this::handleEventOnListeners)
                .flatMap(sagaRepository::handleEventOnSagas)
                .reduce(true, (x, y) -> true)
                .onErrorReturn(false);
    }

    @SuppressWarnings("unchecked")
    private <I extends Id<A, I>, A extends AggregateRoot<A, I>, EM extends EventMessage<I, ? extends Event<I>>> Mono<EM> handleEventOnListeners(EM eventMessage) {
        return getCqrsEventBusHandlers(eventMessage)
                .sort(OrderUtil.COMPARATOR)
                .doOnNext(handler -> handler.onEvent(eventMessage.event(), eventMessage.eventMeta()))
                .doOnError(throwable -> LOGGER.warn("Failed handling event"))
                .reduce(eventMessage, (em, handler) -> em);
    }

    @SuppressWarnings("rawtypes")
    private <I extends Id<A, I>, A extends AggregateRoot<A, I>, EM extends EventMessage<I, ? extends Event<I>>> Flux<CqrsEventBusHandler> getCqrsEventBusHandlers(EM eventMessage) {
        Qualifier<CqrsEventBusHandler> qualifier = Qualifiers.byTypeArguments(eventMessage.event().getClass(), eventMessage.eventMeta().getClass());
        var beansOfType = applicationContext.getBeansOfType(CqrsEventBusHandler.class, qualifier);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Found event handler {} for event: {}", beansOfType, eventMessage.event());
        }
        return Flux.fromIterable(beansOfType);
    }

}

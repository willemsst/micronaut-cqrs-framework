package be.idevelop.cqrs;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.Qualifier;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Introspected
public abstract class AggregateRoot<THIS extends AggregateRoot<THIS, I>, I extends Id<THIS, I>> implements Entity<THIS, I> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregateRoot.class);

    protected final I id;

    long version = -1;

    private final List<EventMessage<I, ? extends Event<I>>> eventMessages;

    @Inject
    private ApplicationContext applicationContext;

    protected AggregateRoot(@Parameter I id) {
        this.id = id;
        this.eventMessages = new ArrayList<>();
    }

    protected final void apply(Event<I> event) {
        this.register(event);
        this.dispatch(event);
    }

    final void replay(EventMessage<I, ? extends Event<I>> eventMessage) {
        this.dispatch(eventMessage.event());
    }

    private void register(Event<I> event) {
        this.incrementVersion();
        this.eventMessages.add(new EventMessage<>(new EventMeta<>(this.id, this.version, Instant.now()), event));
    }

    private void incrementVersion() {
        this.version++;
    }

    private void dispatch(Event<I> event) {
        //noinspection unchecked
        getCqrsEventHandlers(event)
                .sort(OrderUtil.COMPARATOR)
                .doOnNext(handler -> handler.onEvent(this, event))
                .doOnError(throwable -> LOGGER.warn("Failed handling event"))
                .map(handler -> true)
                .onErrorReturn(false)
                .subscribe();
    }

    final List<EventMessage<I, ? extends Event<I>>> eventMessages() {
        return Collections.unmodifiableList(this.eventMessages);
    }

    final THIS markSaved() {
        this.eventMessages.clear();
        //noinspection unchecked
        return (THIS) this;
    }

    public final I getId() {
        return id;
    }

    @SuppressWarnings({"rawtypes"})
    <E extends Event<I>> Flux<CqrsEventHandler> getCqrsEventHandlers(E event) {
        Qualifier<CqrsEventHandler> qualifier = Qualifiers.byTypeArguments(this.getClass(), event.getClass());

        var beansOfType = this.applicationContext.getBeansOfType(CqrsEventHandler.class, qualifier);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Found event handler {} for event: {} - {}", beansOfType, event, beansOfType.size());
        }
        return Flux.fromIterable(beansOfType);
    }

    final AggregateRoot<THIS, I> setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        return this;
    }
}

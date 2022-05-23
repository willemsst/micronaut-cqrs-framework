package be.idevelop.cqrs;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.Qualifier;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.beans.BeanIntrospector;
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

    private final List<EventMessage<I>> eventMessages;

    @Inject
    private ApplicationContext applicationContext;

    protected AggregateRoot(@Parameter I id) {
        this.id = id;
        this.eventMessages = new ArrayList<>();
    }

    protected final void apply(Record event) {
        EventMessage<I> eventMessage = this.register(event);
        this.dispatch(eventMessage);
    }

    final void replay(EventMessage<I> eventMessage) {
        this.dispatch(eventMessage);
    }

    private EventMessage<I> register(Record event) {
        if (isValidEvent(event)) {
            EventMessage<I> eventMessage = new EventMessage<>(new EventMeta<>(this.id, this.version + 1, Instant.now()), event);
            this.eventMessages.add(eventMessage);
            return eventMessage;
        } else {
            throw new IllegalStateException("Invalid event type. Record should have the @Event annotation. Please verify Record " + event.getClass());
        }
    }

    private static boolean isValidEvent(Record event) {
        if (event != null) {
            return BeanIntrospector.SHARED.findIntrospection(event.getClass())
                    .map(
                            beanIntrospection -> beanIntrospection.hasAnnotation(Event.class)
                    ).orElse(false);
        }
        return false;
    }

    private void dispatch(EventMessage<I> eventMessage) {
        //noinspection unchecked
        getCqrsEventHandlers(eventMessage.event())
                .sort(OrderUtil.COMPARATOR)
                .doOnNext(handler -> handler.onEvent(this, eventMessage.event()))
                .doOnError(throwable -> LOGGER.warn("Failed handling event"))
                .doOnComplete(() -> this.version = eventMessage.eventMeta().version())
                .subscribe();
    }

    final List<EventMessage<I>> eventMessages() {
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
    Flux<CqrsEventHandler> getCqrsEventHandlers(Record event) {
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

package be.idevelop.cqrs;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import static java.util.Collections.unmodifiableSet;

@SuppressWarnings({"rawtypes", "UnnecessaryDefault", "unchecked"})
@Introspected
@Serdeable
public abstract class Saga<THIS extends Saga<THIS>> implements Entity<THIS, SagaId<THIS>> {

    public static final TimeoutStrategy NO_TIMEOUT = new TimeoutStrategy(Long.MAX_VALUE, ChronoUnit.DAYS, TimeoutType.NO_TIMEOUT);
    private final SagaId<THIS> sagaId;
    private final Instant created;
    private final Set<Id<?, ?>> associatedEntities;
    private final TimeoutStrategy timeoutStrategy;
    private SagaState<THIS> currentState;
    private Instant scheduledTimeout;
    final Queue<OnSuccessCommandData<THIS, ? extends Id<?, ?>, ? extends Record>> commandsToPublish;

    public Saga(SagaId<THIS> sagaId, Instant created, Enum<? extends SagaState<THIS>> beginState) {
        this(sagaId, created, beginState, NO_TIMEOUT);
    }

    public Saga(SagaId<THIS> sagaId, Instant created, Enum<? extends SagaState<THIS>> beginState, TimeoutStrategy timeoutStrategy) {
        this.sagaId = sagaId;
        this.created = created;
        this.currentState = (SagaState<THIS>) beginState;
        this.timeoutStrategy = timeoutStrategy;
        this.associatedEntities = new LinkedHashSet<>();
        this.scheduledTimeout = initTimeout();
        this.commandsToPublish = new ArrayBlockingQueue<>(1);
    }

    private Instant initTimeout() {
        return switch (this.timeoutStrategy.timeoutType()) {
            case SINCE_START, SINCE_LAST_EVENT -> this.created.plus(timeoutStrategy.value(), timeoutStrategy.unit());
            case NO_TIMEOUT -> Instant.MAX;
            default ->
                    throw new IllegalStateException("Unknown timeout type " + timeoutStrategy.timeoutType() + ". Please implement for this switch statement.");
        };
    }

    private Instant updateTimeout() {
        return switch (timeoutStrategy.timeoutType()) {
            case SINCE_START -> this.scheduledTimeout;
            case SINCE_LAST_EVENT -> Instant.now().plus(timeoutStrategy.value(), timeoutStrategy.unit());
            case NO_TIMEOUT -> Instant.MAX;
            default ->
                    throw new IllegalStateException("Unknown timeout type " + timeoutStrategy.timeoutType() + ". Please implement for this switch statement.");
        };
    }

    public final boolean isLive() {
        return !isTimedOut() && this.currentState.isLive();
    }

    public <EVENT extends Record> boolean transit(final EVENT event, final EventMeta eventMeta) {
        final THIS saga = (THIS) this;
        return lookupPossibleTransition(event)
                .map(transition -> {
                    this.associatedEntities.add(eventMeta.objectId());
                    this.currentState = transition.transit(saga, event, eventMeta);
                    this.commandsToPublish.offer(new OnSuccessCommandData<>(saga, event, eventMeta, transition.onSuccessCommand()));
                    return true;
                })
                .orElse(false);
    }

    private <I extends Id<E, I>, E extends Entity<E, I>, EVENT extends Record> Optional<Transition<THIS, I, EVENT>> lookupPossibleTransition(EVENT event) {
        if (!isTimedOut()) {
            if (this.currentState.isAllowed(event)) {
                this.scheduledTimeout = updateTimeout();
                return this.currentState.lookForValidTransition(event);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isTimedOut() {
        return Instant.now().isAfter(this.scheduledTimeout);
    }

    final SagaId<THIS> getSagaId() {
        return sagaId;
    }

    final Instant getCreated() {
        return created;
    }

    final Set<Id> getAssociatedEntities() {
        return unmodifiableSet(associatedEntities);
    }

    final Instant getScheduledTimeout() {
        return this.scheduledTimeout;
    }

    final SagaState getCurrentState() {
        return this.currentState;
    }

    final void setCurrentState(SagaState currentState) {
        this.currentState = currentState;
    }

    final String getFieldDataAsJson() {
        throw new UnsupportedOperationException("");
//        return null;
    }

    final Saga<THIS> hydrateFieldDataFromJson(String json) {
        throw new UnsupportedOperationException("");
//        return this;
    }

    record OnSuccessCommandData<S extends Saga<S>, EVENT extends Record, I extends Id<?, I>>(Saga s, EVENT event,
                                                                                             EventMeta<I> meta,
                                                                                             Transition.CommandGenerator<S, I, EVENT> generator) {
        Optional<Command<? extends Id>> generate() {
            return generator.generateCommand((S) s, meta, event);
        }
    }
}

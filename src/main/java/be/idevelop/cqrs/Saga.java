package be.idevelop.cqrs;

import io.micronaut.core.annotation.Introspected;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

@SuppressWarnings({"rawtypes", "UnnecessaryDefault"})
@Introspected
public abstract class Saga<THIS extends Saga<THIS>> implements Entity<THIS, SagaId<THIS>> {

    public static final TimeoutStrategy NO_TIMEOUT = new TimeoutStrategy(Long.MAX_VALUE, ChronoUnit.DAYS, TimeoutType.NO_TIMEOUT);

    private final SagaId<THIS> sagaId;
    private final Instant created;
    private final List<HandledEvent> eventsToStore;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<HandledEvent> handledEvents;
    private final Set<Id> associatedEntities;
    private final AtomicInteger currentVersion;
    private final Queue<Runnable> onSuccessActions;
    private final TimeoutStrategy timeoutStrategy;

    private SagaState currentState;
    private Instant scheduledTimeout;

    public Saga(SagaId<THIS> sagaId, Instant created, SagaState beginState) {
        this(sagaId, created, beginState, NO_TIMEOUT);
    }

    public Saga(SagaId<THIS> sagaId, Instant created, SagaState beginState, TimeoutStrategy timeoutStrategy) {
        this.sagaId = sagaId;
        this.created = created;
        this.currentState = beginState;
        this.timeoutStrategy = timeoutStrategy;
        this.eventsToStore = new ArrayList<>();
        this.handledEvents = new ArrayList<>();
        this.associatedEntities = new LinkedHashSet<>();
        this.onSuccessActions = new LinkedList<>();
        this.currentVersion = new AtomicInteger(0);
        this.scheduledTimeout = initTimeout();
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

    public final <I extends Id<E, I>, E extends Entity<E, I>> void transit(I id, Record event, Runnable... onSuccessActions) {
        if (doTransit(id, event, false)) {
            Collections.addAll(this.onSuccessActions, onSuccessActions);
        }
    }

    private <I extends Id<E, I>, E extends Entity<E, I>> boolean doTransit(I id, Record event, boolean inReplayMode) {
        boolean result = false;
        if (!isTimedOut()) {
            if (this.currentState.isAllowed(event)) {
                this.associatedEntities.add(id);
                if (!inReplayMode) {
                    this.scheduledTimeout = updateTimeout();
                    this.eventsToStore.add(new HandledEvent(event, currentVersion.incrementAndGet(), Instant.now()));
                }
                this.currentState = this.currentState.transit(event);
                result = true;
            }
        }
        return result;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isTimedOut() {
        return Instant.now().isAfter(this.scheduledTimeout);
    }

    final void markSaved() {
        this.handledEvents.addAll(this.eventsToStore);
        this.eventsToStore.clear();
    }

    final void performOnSuccessActions() {
        while (!this.onSuccessActions.isEmpty()) {
            try {
                this.onSuccessActions.poll().run();
            } catch (Exception e) {
                throw new IllegalStateException("Could not execute on success action", e);
            }
        }
    }

    final void replayEvents(List<HandledEvent> handledEvents) {
        for (HandledEvent handledEvent : handledEvents) {
            this.doTransit(getSagaId(), handledEvent.event, true);
            this.currentVersion.set(handledEvent.version);
        }
        this.onSuccessActions.clear();
    }

    record HandledEvent(Record event, int version, Instant timestamp) {

    }

    final SagaId<THIS> getSagaId() {
        return sagaId;
    }

    final Instant getCreated() {
        return created;
    }

    final List<HandledEvent> getEventsToStore() {
        return unmodifiableList(eventsToStore);
    }

    final Set<Id> getAssociatedEntities() {
        return unmodifiableSet(associatedEntities);
    }

    final int getCurrentVersion() {
        return currentVersion.intValue();
    }

    final Instant getScheduledTimeout() {
        return this.scheduledTimeout;
    }
}

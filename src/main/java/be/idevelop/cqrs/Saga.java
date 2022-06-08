package be.idevelop.cqrs;

import io.micronaut.core.annotation.Introspected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import static java.util.Collections.unmodifiableSet;

@SuppressWarnings({"rawtypes", "UnnecessaryDefault", "unchecked"})
@Introspected
public abstract class Saga<THIS extends Saga<THIS>> implements Entity<THIS, SagaId<THIS>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Saga.class);
    public static final TimeoutStrategy NO_TIMEOUT = new TimeoutStrategy(Long.MAX_VALUE, ChronoUnit.DAYS, TimeoutType.NO_TIMEOUT);
    private final SagaId<THIS> sagaId;
    private final Instant created;
    private final List<SagaState> states;
    private SagaState currentState;
    private final Set<Id<?, ?>> linkedEntities;
    private final TimeoutStrategy timeoutStrategy;
    private final Instant scheduledTimeout;
    final Queue<Command<? extends Id<?, ?>>> commandsToPublish;

    public Saga(SagaId<THIS> sagaId, Instant created, SagaState... states) {
        this(sagaId, created, NO_TIMEOUT, states);
    }

    public Saga(SagaId<THIS> sagaId, Instant created, TimeoutStrategy timeoutStrategy, SagaState... states) {
        this.sagaId = sagaId;
        this.created = created;
        this.states = List.of(states);
        this.currentState = SagaState.START_STATE;
        this.timeoutStrategy = timeoutStrategy;
        this.linkedEntities = new LinkedHashSet<>();
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

    final void updateTimeout() {
        switch (timeoutStrategy.timeoutType()) {
            case SINCE_LAST_EVENT -> Instant.now().plus(timeoutStrategy.value(), timeoutStrategy.unit());
            case SINCE_START, NO_TIMEOUT -> {
            }
            default ->
                    throw new IllegalStateException("Unknown timeout type " + timeoutStrategy.timeoutType() + ". Please implement for this switch statement.");
        }
    }

    final boolean isLive() {
        return !isTimedOut() && this.currentState.isLive();
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

    final Set<Id> getLinkedEntities() {
        return unmodifiableSet(linkedEntities);
    }

    final Instant getScheduledTimeout() {
        return this.scheduledTimeout;
    }

    final SagaState getCurrentState() {
        return this.currentState;
    }

    final void setCurrentState(SagaState currentState) {
        if (isValidState(currentState)) {
            this.currentState = currentState;
        }
    }

    private boolean isValidState(SagaState newState) {
        return newState.equals(SagaState.END_STATE) || this.states.contains(newState);
    }

    abstract Map<String, Object> getFieldData();

    abstract void hydrateFieldData(Map<String, Object> fieldData);

    /**
     * Publish a new command on the command bus. This will be performed Async once the saga is stored successfully so it
     * doesn't block the handling of the current command with its events.
     *
     * @param command the command to publish.
     */
    final void publishCommand(Command command) {
        this.commandsToPublish.offer(command);
    }

    final <I extends Id<?, I>> void linkEntity(I objectId) {
        this.linkedEntities.add(objectId);
    }

    final <EVENT extends Record, I extends Id<?, I>, EM extends EventMessage<I, ? extends EVENT>> void handleEvent(CqrsSagaEventHandler<THIS, I, EVENT> cqrsSagaEventHandler, EM eventMessage) {
        SagaState newState = cqrsSagaEventHandler.onEvent((THIS) this, eventMessage.eventMeta(), eventMessage.event());
        LOGGER.info("Saga {} changing state from {} into {}", this.getSagaId(), getCurrentState().name(), newState.name());
        this.setCurrentState(newState);
    }
}

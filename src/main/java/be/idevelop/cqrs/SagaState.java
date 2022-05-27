package be.idevelop.cqrs;

import io.micronaut.core.annotation.Introspected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

@SuppressWarnings({"unchecked", "rawtypes"})
@Introspected
public interface SagaState<S extends Saga<S>> {

    Logger LOGGER = LoggerFactory.getLogger(SagaState.class);

    String name();

    Transition<S, ?, ?>[] transitions();

    default <I extends Id<?, I>, EVENT extends Record> Optional<Transition<S, I, EVENT>> lookForValidTransition(EVENT event) {
        if (isAllowed(event)) {
            for (Transition transition : this.transitions()) {
                if (transition.isAllowed(event)) {
                    return Optional.of(transition);
                }
            }
            LOGGER.info("Could not find a transition from state {} with input [{}}].", this.name(), event);
        }
        return Optional.empty();
    }

    default boolean isAllowed(Record event) {
        return Arrays.stream(transitions()).anyMatch(transition -> transition.isAllowed(event));
    }

    default boolean isLive() {
        return this.transitions().length > 0;
    }

    @SuppressWarnings("rawtypes")
    SagaState END_STATE = new SagaState() {
        @Override
        public boolean isAllowed(Record event) {
            return false;
        }

        @Override
        public String name() {
            return "END_STATE";
        }

        @Override
        public Optional<Transition> lookForValidTransition(Record record) {
            return Optional.empty();
        }

        @Override
        public Transition[] transitions() {
            return new Transition[0];
        }

    };
}

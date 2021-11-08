package be.idevelop.cqrs;

import io.micronaut.core.annotation.Introspected;

import java.util.Arrays;
import java.util.function.Function;

@Introspected
public interface SagaState {

    SagaState END_STATE = new EndState();

    String name();

    SagaTransition[] transitions();

    default boolean isAllowed(Object input) {
        return input != null && isLive() && Arrays.stream(this.transitions()).anyMatch(transition -> transition.isAllowed(input));
    }

    default SagaState transit(Object input) {
        if (isAllowed(input)) {
            for (SagaTransition transition : this.transitions()) {
                if (transition.isAllowed(input)) {
                    SagaState newState = transition.transit(input);
                    if(newState != this) {
                        return newState;
                    }
                }
            }
            throw new IllegalStateException("Could not transit from state" + this.name() + " with input [" + input + "].");
        } else {
            return this;
        }
    }

    default boolean isLive() {
        return this.transitions().length > 0;
    }

    final class EndState implements SagaState {

        @Override
        public boolean isAllowed(Object input) {
            return false;
        }

        @Override
        public String name() {
            return "END_STATE";
        }

        @Override
        public SagaTransition[] transitions() {
            return new SagaTransition[0];
        }

        @Override
        public SagaState transit(Object input) {
            return this;
        }

        @Override
        public boolean isLive() {
            return false;
        }
    }

    record SagaTransition(Function<Object, Boolean> predicate, SagaState newState) {

        public boolean isAllowed(Object input) {
            return input != null && predicate.apply(input);
        }

        public SagaState transit(Object input) {
            if (isAllowed(input)) {
                return newState;
            } else {
                return null;
            }
        }
    }
}

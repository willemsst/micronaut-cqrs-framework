package be.idevelop.cqrs;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public interface SagaState {

    String name();

    default boolean isLive() {
        return true;
    }

    SagaState START_STATE = () -> "START_STATE";

    SagaState END_STATE = new SagaState() {
        @Override
        public String name() {
            return "END_STATE";
        }

        @Override
        public boolean isLive() {
            return false;
        }
    };
}

package be.idevelop.cqrs;

import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static be.idevelop.cqrs.CreateNewTestObjectSaga.State.VALIDATE;
import static be.idevelop.cqrs.SagaState.END_STATE;

final class CreateNewTestObjectSaga extends Saga<CreateNewTestObjectSaga> {

    @Override
    Map<String, Object> getFieldData() {
        Map<String, Object> map = new HashMap<>();
        map.put("someField", someField);
        return map;
    }

    @Override
    void hydrateFieldData(Map<String, Object> fieldData) {
        this.someField = (boolean) fieldData.get("someField");
    }

    enum State implements SagaState {
        VALIDATE
    }

    private boolean someField;

    public CreateNewTestObjectSaga(SagaId<CreateNewTestObjectSaga> sagaId, Instant created) {
        super(sagaId, created, State.values());
    }

    @Singleton
    static class SagaEventHandlers {

        @SagaEventHandler(state = "START_STATE", event = TestCreatedEvent.class)
        SagaState onEvent(CreateNewTestObjectSaga saga, EventMeta<TestId> meta, TestCreatedEvent event) {
            saga.someField = true;
            saga.publishCommand(new ValidateTestObjectCommand(meta.objectId()));
            return VALIDATE;
        }

        @SagaEventHandler(state = "VALIDATE", event = TestValidatedEvent.class)
        SagaState onEvent(CreateNewTestObjectSaga saga, EventMeta<TestId> meta, TestValidatedEvent event) {
            if (saga.someField) {
                saga.someField = false;
            }
            return END_STATE;
        }
    }
}

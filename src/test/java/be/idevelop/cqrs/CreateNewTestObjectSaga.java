package be.idevelop.cqrs;

import java.time.Instant;
import java.util.Optional;

import static be.idevelop.cqrs.CreateNewTestObjectSaga.State.NEW;

final class CreateNewTestObjectSaga extends Saga<CreateNewTestObjectSaga> {

    private boolean someField;

    public CreateNewTestObjectSaga(SagaId<CreateNewTestObjectSaga> sagaId, Instant created) {
        super(sagaId, created, NEW);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    enum State implements SagaState<CreateNewTestObjectSaga> {
        NEW {
            @Override
            public Transition[] transitions() {
                return new Transition[]{
                        new Transition<CreateNewTestObjectSaga, TestId, TestCreatedEvent>(
                                (saga, meta, event) -> {
                                    saga.someField = true;
                                    return VALIDATE;
                                },
                                (saga, meta, event) -> Optional.of(new ValidateTestObjectCommand(meta.objectId()))
                        ),
                };
            }
        },
        VALIDATE {
            @Override
            public Transition[] transitions() {
                return new Transition[]{
                        new Transition<CreateNewTestObjectSaga, TestId, TestValidatedEvent>((saga, event, eventMeta) -> END_STATE)
                };
            }
        }
    }
}

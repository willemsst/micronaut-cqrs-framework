package be.idevelop.cqrs;

import jakarta.inject.Inject;

import java.time.Instant;

final class CreateNewTestObjectSaga extends Saga<CreateNewTestObjectSaga> {

    public CreateNewTestObjectSaga(SagaId<CreateNewTestObjectSaga> sagaId, Instant created) {
        super(sagaId, created, State.NEW);
    }

    enum State implements SagaState {
        NEW {
            @Override
            public SagaTransition[] transitions() {
                return new SagaTransition[]{
                        new SagaTransition(e -> e instanceof TestCreatedEvent, TEST_CREATED),
                        new SagaTransition(e -> e instanceof SagaExpiredEvent, END_STATE)
                };
            }
        },
        TEST_CREATED {
            @Override
            public SagaTransition[] transitions() {
                return new SagaTransition[]{
                        new SagaTransition(e -> e instanceof TestValidatedEvent, END_STATE),
                        new SagaTransition(e -> e instanceof SagaExpiredEvent, END_STATE)
                };
            }
        }
    }

    @SagaDefinition(saga = CreateNewTestObjectSaga.class)
    static class EventHandlers {

        @Inject
        private CommandBus commandBus;

        @SagaEventHandler(event = TestCreatedEvent.class)
        public void onEvent(CreateNewTestObjectSaga saga, EventMessage<TestId> eventMessage) {
            saga.transit(eventMessage.objectId(), eventMessage.event(), () -> commandBus.publish(new ValidateTestObjectCommand(eventMessage.objectId())));
        }
    }

}

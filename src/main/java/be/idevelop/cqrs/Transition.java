package be.idevelop.cqrs;

import io.micronaut.core.reflect.GenericTypeUtils;

import java.util.Objects;
import java.util.Optional;

record Transition<S extends Saga<S>, I extends Id<?, I>, EVENT extends Record>(
        TransitionLambda<S, I, EVENT> transition,
        CommandGenerator<S, I, EVENT> onSuccessCommand
) {

    public Transition(TransitionLambda<S, I, EVENT> transition) {
        this(transition, (s, i, event) -> Optional.empty());
    }

    public SagaState<S> transit(S saga, EVENT event, EventMeta<I> meta) {
        return this.transition.transit(saga, meta, event);
    }

    boolean isAllowed(Record event) {
        boolean result = false;
        Class<?>[] classes = GenericTypeUtils.resolveInterfaceTypeArguments(this.getClass(), Transition.class);
        if (classes.length == 3) {
            result = Objects.equals(classes[2], event.getClass());
        }
        return result;
    }

    interface TransitionLambda<S2 extends Saga<S2>, I2 extends Id<?, I2>, EVENT2 extends Record> {
        SagaState<S2> transit(S2 s, EventMeta<I2> meta, EVENT2 event);
    }

    interface CommandGenerator<S2 extends Saga<S2>, I2 extends Id<?, I2>, EVENT2 extends Record> {
        Optional<Command<? extends Id>> generateCommand(S2 s, EventMeta<I2> meta, EVENT2 event);
    }

}

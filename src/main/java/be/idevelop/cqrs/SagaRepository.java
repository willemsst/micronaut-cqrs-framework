package be.idevelop.cqrs;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static be.idevelop.cqrs.SagaState.END_STATE;
import static be.idevelop.cqrs.SagaState.START_STATE;

@Singleton
class SagaRepository {

    private static final String SAGA_ON_SUCCESS_ACTIONS_THREAD = "saga-on-success-actions";

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaRepository.class);

    private final ApplicationContext applicationContext;

    private final SagaStore sagaStore;

    @Inject
    public SagaRepository(ApplicationContext applicationContext, SagaStore sagaStore) {
        this.applicationContext = applicationContext;
        this.sagaStore = sagaStore;
    }

    @SuppressWarnings({"unchecked"})
    <I extends Id<A, I>, A extends AggregateRoot<A, I>, EM extends EventMessage<I, ? extends EVENT>, EVENT extends Record, S extends Saga<S>> Mono<EM> handleEventOnSagas(EM eventMessage) {
        return getCqrsSagaEventHandlers(eventMessage.event())
                .flatMap(cqrsSagaEventHandler ->
                        ((Flux<S>) findExistingOrCreateSagaForEventAndEventHandler(eventMessage.objectId(), cqrsSagaEventHandler))
                                .doOnNext(saga -> saga.handleEvent(cqrsSagaEventHandler, eventMessage))
                                .doOnNext(saga -> saga.linkEntity(eventMessage.objectId()))
                                .doOnNext(Saga::updateTimeout)
                                .doOnNext(this::storeSaga)
                )
                .reduce(eventMessage, (em, saga) -> em);
    }

    @SuppressWarnings("rawtypes")
    private <I extends Id<A, I>, A extends AggregateRoot<A, I>, EVENT extends Record> Flux<CqrsSagaEventHandler> getCqrsSagaEventHandlers(EVENT event) {
        var beanDefinitions = applicationContext.getBeanDefinitions(CqrsSagaEventHandler.class);
        return Flux.fromIterable(beanDefinitions)
                .filter(beanDefinition -> beanDefinition.isAnnotationPresent(SagaEventHandler.class))
                .filter(beanDefinition -> Objects.requireNonNull(beanDefinition.getAnnotation(SagaEventHandler.class)).getRequiredValue("event", Class.class) == event.getClass())
                .doOnNext(matching -> {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Found saga event handler {} for event: {}", matching, event);
                    }
                })
                .map(applicationContext::getBean)
                .cast(CqrsSagaEventHandler.class);
    }

    @SuppressWarnings("unchecked")
    private <I extends Id<A, I>, A extends AggregateRoot<A, I>, EVENT extends Record, S extends Saga<S>> Flux<S> findExistingOrCreateSagaForEventAndEventHandler(I objectId, CqrsSagaEventHandler<S, I, EVENT> cqrsSagaEventHandler) {
        BeanDefinition<? extends CqrsSagaEventHandler<S, I, EVENT>> beanDefinition = (BeanDefinition<? extends CqrsSagaEventHandler<S, I, EVENT>>) applicationContext.getBeanDefinition(cqrsSagaEventHandler.getClass());
        List<Argument<?>> typeArguments = beanDefinition.getTypeArguments(CqrsSagaEventHandler.class);
        Class<S> sagaClass = (Class<S>) typeArguments.get(0).getType();
        return Mono.justOrEmpty(beanDefinition)
                .flatMapIterable(BeanDefinition::getExecutableMethods)
                .filter(method -> method.getArguments().length == 3)
                .flatMap(method ->
                        Mono.justOrEmpty(method.getAnnotation(SagaEventHandler.class)).flatMapMany(annotation -> {
                            String state = annotation.getRequiredValue("state", String.class);
                            boolean createNew = START_STATE.name().equals(state);
                            if (createNew) {
                                return Mono.justOrEmpty(BeanIntrospector.SHARED.findIntrospection(sagaClass))
                                        .map(i -> i.instantiate(SagaId.createNew(sagaClass), Instant.now()));
                            } else {
                                return sagaStore.findAssociatedSagas(objectId, sagaClass)
                                        .filter(saga -> state.equals(saga.getCurrentState().name()))
                                        .filter(Saga::isLive);
                            }
                        })
                );
    }

    private <S extends Saga<S>> void storeSaga(S saga) {
        if (END_STATE.equals(saga.getCurrentState())) {
            this.sagaStore.deleteSaga(saga);
        } else {
            this.sagaStore.storeSaga(saga);
        }
        performOnSuccessActionsAsync(saga);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Async(SAGA_ON_SUCCESS_ACTIONS_THREAD)
    <S extends Saga<S>> void performOnSuccessActionsAsync(S saga) {
        this.applicationContext.getActiveBeanRegistrations(CommandBus.class).stream().findFirst().map(BeanRegistration::getBean)
                .ifPresent(commandBus -> {
                            while (!saga.commandsToPublish.isEmpty()) {
                                commandBus.publish(
                                        (Command) saga.commandsToPublish.poll()
                                );
                            }
                        }
                );
    }
}

package be.idevelop.cqrs;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Collection;

import static be.idevelop.cqrs.TaskExecutorFactory.SAGA_ON_SUCCESS_ACTIONS_THREAD;

@Singleton
class SagaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaRepository.class);

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private SagaStore sagaStore;

    @SuppressWarnings({"unchecked"})
    <I extends Id<A, I>, A extends AggregateRoot<A, I>, EM extends EventMessage<I, ? extends EVENT>, EVENT extends Record, S extends Saga<S>> Mono<EM> handleEventOnSagas(EM eventMessage) {
        Collection<Class<?>> sagaClasses = BeanIntrospector.SHARED.findIntrospectedTypes(x -> Saga.class.isAssignableFrom(x.getBeanType()) && !Modifier.isAbstract(x.getBeanType().getModifiers()));
        return Flux.fromIterable(sagaClasses)
                .map(sagaClass -> (Class<S>) sagaClass)
                .flatMap(sagaClass -> findAssociatedSagas(sagaClass, eventMessage.objectId())
                        .switchIfEmpty(Flux.defer(() -> Flux.just(createNewSaga(sagaClass))))
                        .doOnNext(saga -> {
                            if (saga.transit(eventMessage.event(), eventMessage.eventMeta())) {
                                this.storeSaga(saga);
                            }
                        }))
                .reduce(eventMessage, (em, method) -> em);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <I extends Id<A, I>, A extends AggregateRoot<A, I>, S extends Saga<S>> Flux<S> findAssociatedSagas(Class<S> sagaClass, I objectId) {
        return (Flux<S>) this.sagaStore.findAssociatedSagas(objectId)
                .filter(sagaClass::isInstance);
    }

    private static <S extends Saga<S>> S createNewSaga(Class<S> sagaClass) {
        BeanIntrospection<S> introspection = BeanIntrospection.getIntrospection(sagaClass);
        return introspection.instantiate(SagaId.createNew(sagaClass), Instant.now());
    }

    final <S extends Saga<S>> void storeSaga(S saga) {
        this.sagaStore.storeSaga(saga);

        performOnSuccessActionsAsync(saga);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Async(SAGA_ON_SUCCESS_ACTIONS_THREAD)
    <S extends Saga<S>> void performOnSuccessActionsAsync(S saga) {
        this.applicationContext.getActiveBeanRegistrations(CommandBus.class).stream().findFirst().map(BeanRegistration::getBean)
                .ifPresent(commandBus -> {
                    while (!saga.commandsToPublish.isEmpty()) {
                        saga.commandsToPublish.poll().generate().ifPresent(command -> commandBus.publish((Command) command));
                    }
                });
    }
}

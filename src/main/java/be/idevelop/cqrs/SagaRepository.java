package be.idevelop.cqrs;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.Qualifier;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static be.idevelop.cqrs.TaskExecutorFactory.SAGA_ON_SUCCESS_ACTIONS_THREAD;

@Singleton
class SagaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaRepository.class);

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private SagaStore sagaStore;

    @SuppressWarnings({"unchecked"})
    <I extends Id<A, I>, A extends AggregateRoot<A, I>, EM extends EventMessage<I, ? extends Record>, S extends Saga<S>> Mono<EM> handleEventOnSagas(EM eventMessage) {
        Qualifier<Object> byAnnotation = Qualifiers.byAnnotation(() -> SagaDefinition.class);
        return Flux.fromIterable(applicationContext.getBeanDefinitions(byAnnotation))
                .flatMap(sagaDefinition -> {
                            Class<S> sagaClass = lookupSagaClassForMethod(sagaDefinition);
                            return lookupAllMethodsForEventType(sagaDefinition, sagaClass, eventMessage)
                                    .flatMap(method -> {
                                        if (LOGGER.isTraceEnabled()) {
                                            LOGGER.trace("Found event handler {} for event: {}", method.getDescription(true), eventMessage.event());
                                        }
                                        return findAssociatedSagas(sagaClass, eventMessage.objectId())
                                                .cast(Saga.class)
                                                .switchIfEmpty(Flux.defer(() -> Flux.just(createNewSaga(sagaClass))))
                                                .doOnNext(saga -> {
                                                    Object[] arguments = prepareParametersForMethod(method, saga, eventMessage);
                                                    Object bean = applicationContext.getBean(method.getDeclaringType());
                                                    method.invoke(bean, arguments);
                                                    this.storeSaga(saga);
                                                })
                                                .reduce(method, (m, s) -> m);
                                    });
                        }
                )
                .reduce(eventMessage, (em, method) -> em);
    }

    @SuppressWarnings({"rawtypes"})
    private static <I extends Id<A, I>, A extends AggregateRoot<A, I>, EM extends EventMessage<I, ? extends Record>, S extends Saga<S>>
    Flux<ExecutableMethod> lookupAllMethodsForEventType(BeanDefinition<?> sagaDefinition, Class<S> sagaClass, EM eventMessage) {
        return Flux.fromIterable(sagaDefinition.getExecutableMethods())
                .filter(method -> method.hasAnnotation(SagaEventHandler.class))
                .filter(method -> isValidMethodForEvent(method, sagaClass, eventMessage))
                .cast(ExecutableMethod.class);
    }

    @SuppressWarnings("ConstantConditions")
    private static <I extends Id<A, I>, A extends AggregateRoot<A, I>, EM extends EventMessage<I, ? extends Record>> boolean isValidMethodForEvent(ExecutableMethod<?, ?> method, Class<?> sagaClass, EM eventMessage) {
        Class<?> eventClass = method.getAnnotation(SagaEventHandler.class).get("event", Class.class)
                .orElseThrow(() -> new IllegalStateException("Expected an Event class on the @SagaEventHandler annotation for method " + method.getDescription()));
        if (eventClass.isAssignableFrom(eventMessage.event().getClass())) {
            for (Argument<?> argument : method.getArguments()) {
                if (Saga.class.isAssignableFrom(argument.getType()) && !sagaClass.isAssignableFrom(argument.getType())) {
                    throw new IllegalStateException("Wrong saga class for this @SagaDefinition(saga=" + sagaClass + ")");
                }
                if (Event.class.isAssignableFrom(argument.getType()) && !eventClass.isAssignableFrom(argument.getType())) {
                    throw new IllegalStateException("Wrong event for this @SagaEventHandler(event=" + eventClass + ") method: " + method.getDescription());
                }
                if (EventMessage.class.isAssignableFrom(argument.getType())) {
                    for (Argument<?> typeParameter : argument.getTypeParameters()) {
                        if (Event.class.isAssignableFrom(typeParameter.getType()) && !typeParameter.getType().isAssignableFrom(eventClass)) {
                            throw new IllegalStateException("Wrong event for this @SagaEventHandler(event=" + eventClass + ") method: " + method.getDescription());
                        }
                    }
                }
            }
        } else {
            return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "CastCanBeRemovedNarrowingVariableType"})
    private <S extends Saga<S>> Class<S> lookupSagaClassForMethod
            (BeanDefinition<?> sagaBeanDefinition) {
        Class<?> sClass = sagaBeanDefinition.findAnnotation(SagaDefinition.class).flatMap(annotation -> annotation.get("saga", Class.class))
                .filter(Saga.class::isAssignableFrom)
                .orElseThrow(() -> new IllegalStateException("Expected a Saga class on the @SagaDefinition annotation on " + sagaBeanDefinition.getName()));
        return (Class<S>) sClass;
    }

    private static <I extends Id<A, I>, A extends AggregateRoot<A, I>, EM extends EventMessage<I, ? extends Record>, S extends Saga<S>> Object[] prepareParametersForMethod(ExecutableMethod<?, ?> method, S saga, EM eventMessage) {
        Object[] parameters = new Object[method.getArguments().length];
        for (int i = 0; i < method.getArguments().length; i++) {
            parameters[i] = prepareParameterForArgument(method.getArgumentTypes()[i], saga, eventMessage);
        }
        return parameters;
    }

    private static <I extends Id<A, I>, A extends AggregateRoot<A, I>, EM extends EventMessage<I, ? extends Record>, S extends Saga<S>> Object prepareParameterForArgument(Class<?> argumentType, S saga, EM eventMessage) {
        if (Saga.class.isAssignableFrom(argumentType)) {
            return saga;
        } else if (EventMessage.class.isAssignableFrom(argumentType)) {
            return eventMessage;
        } else if (Record.class.isAssignableFrom(argumentType) && argumentType.isAnnotationPresent(Event.class)) {
            return eventMessage.event();
        } else if (EventMeta.class.isAssignableFrom(argumentType)) {
            return eventMessage.eventMeta();
        } else {
            throw new IllegalStateException("Unsupported argument type " + argumentType);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <I extends Id<A, I>, A extends AggregateRoot<A, I>, S extends Saga<S>> Flux<S> findAssociatedSagas(Class<? extends Saga> sagaClass, I objectId) {
        return (Flux<S>) this.sagaStore.findAssociatedSagas(objectId)
                .filter(sagaClass::isInstance);
    }

    private static <S extends Saga<S>> S createNewSaga(Class<S> sagaClass) {
        BeanIntrospection<S> introspection = BeanIntrospection.getIntrospection(sagaClass);
        return introspection.instantiate(SagaId.createNew(sagaClass), Instant.now());
    }

    final <S extends Saga<S>> void storeSaga(S saga) {
        this.sagaStore.storeSaga(saga);

        saga.markSaved();
        performOnSuccessActionsAsync(saga);
    }

    @Async(SAGA_ON_SUCCESS_ACTIONS_THREAD)
    <S extends Saga<S>> void performOnSuccessActionsAsync(S saga) {
        saga.performOnSuccessActions();
    }
}

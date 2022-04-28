package be.idevelop.cqrs;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Factory
final class TaskExecutorFactory {

    static final String SAGA_ON_SUCCESS_ACTIONS_THREAD = "saga-on-success-actions";

    @Singleton
    @Named(SAGA_ON_SUCCESS_ACTIONS_THREAD)
    ExecutorService sagaSuccessActionsExecutorService() {
        return Executors.newSingleThreadExecutor(r -> new Thread(r, SAGA_ON_SUCCESS_ACTIONS_THREAD));
    }
}

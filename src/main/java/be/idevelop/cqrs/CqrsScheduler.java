package be.idevelop.cqrs;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Singleton
final class CqrsScheduler {

    private final int threadPoolSize;
    private final Scheduler[] schedulers;

    CqrsScheduler(@Value("${cqrs.scheduler.prefix}") String prefix, @Value("${cqrs.scheduler.poolSize}") int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;

        this.schedulers = new Scheduler[this.threadPoolSize];
        for (var i = 0; i < this.schedulers.length; i++) {
            this.schedulers[i] = Schedulers.fromExecutorService(Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(prefix + "-" + i)));
        }
    }

    <I extends Id<?, I>> Scheduler schedule(I objectId) {
        int index = objectId.hashCode() % this.threadPoolSize;
        return schedulers[index];
    }

    private record NamedThreadFactory(String name) implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, name);
        }
    }
}

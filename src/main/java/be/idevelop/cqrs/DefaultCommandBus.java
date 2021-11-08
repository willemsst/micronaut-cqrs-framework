package be.idevelop.cqrs;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.Qualifier;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Singleton
final class DefaultCommandBus implements CommandBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCommandBus.class);

    private final ApplicationContext applicationContext;

    private final CqrsScheduler cqrsScheduler;

    private final ObjectRepository objectRepository;

    private final Scheduler continuationScheduler = Schedulers.boundedElastic();

    @Inject
    DefaultCommandBus(ApplicationContext applicationContext, CqrsScheduler cqrsScheduler, ObjectRepository objectRepository) {
        this.applicationContext = applicationContext;
        this.cqrsScheduler = cqrsScheduler;
        this.objectRepository = objectRepository;
    }

    @Override
    public <I extends Id<A, I>, A extends AggregateRoot<A, I>> void publish(Command<I> command) {
        process(command).ignoreElement().subscribe();
    }

    @Override
    public <I extends Id<A, I>, A extends AggregateRoot<A, I>> Mono<I> publishAndWait(Command<I> command) {
        return process(command);
    }

    private <I extends Id<A, I>, A extends AggregateRoot<A, I>> Mono<I> process(Command<I> command) {
        Class<A> aggregateRootClass = command.objectId().getEntityClass();

        //noinspection unchecked
        return objectRepository.retrieve(command.objectId(), aggregateRootClass)
                .flatMap(aggregateRoot ->
                        getCqrsCommandHandlers(command)
                                .sort(OrderUtil.COMPARATOR)
                                .reduce(aggregateRoot,
                                        (a, handler) -> (A) handler.onCommand(a, command)
                                )
                )
                .flatMap(objectRepository::save)
                .map(aggregateRoot -> aggregateRoot.id)
                .subscribeOn(cqrsScheduler.schedule(command.objectId()))
                .publishOn(continuationScheduler);
    }

    @SuppressWarnings("rawtypes")
    <I extends Id<A, I>, A extends AggregateRoot<A, I>> Flux<CqrsCommandHandler> getCqrsCommandHandlers(Command<I> command) {
        Qualifier<CqrsCommandHandler> qualifier = Qualifiers.byTypeArguments(command.objectId().getEntityClass(), command.getClass());
        var beansOfType = applicationContext.getBeansOfType(CqrsCommandHandler.class, qualifier);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Found command handler {} for command: {} - {}", beansOfType, command, beansOfType.size());
        }
        return Flux.fromIterable(beansOfType);
    }
}

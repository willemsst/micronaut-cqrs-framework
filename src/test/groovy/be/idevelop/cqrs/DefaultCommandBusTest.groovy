package be.idevelop.cqrs

import io.micronaut.context.annotation.Replaces
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@MicronautTest
class DefaultCommandBusTest extends Specification {

    @Inject
    CommandBus commandBus

    def "Publish"() {
        given:
        def command = new TestCommand(new TestId())

        when:
        commandBus.publish(command)

        then:
        noExceptionThrown()
    }

    def "PublishAndWait"() {
        given:
        def command = new TestCommand(new TestId())

        when:
        def objectId = commandBus.publishAndWait(command).block()

        then:
        noExceptionThrown()
        assert objectId == command.objectId()
    }

    @MockBean
    @Replaces(EventRepository)
    static class InMemoryEventRepository implements EventRepository {

        @SuppressWarnings("rawtypes")
        final ConcurrentMap<Id, List> eventMessages = new ConcurrentHashMap<>();

        @Override
        <I extends Id<A, I>, A extends AggregateRoot<A, I>, E extends Event<I>> Flux<EventMessage<I, E>> retrieveEventMessages(I objectId, Class<A> claßß) {
            //noinspection unchecked
            List<EventMessage<I, E>> events = this.eventMessages.get(objectId)
            if (events == null || events.isEmpty()) {
                return Flux.empty()
            } else {
                //noinspection unchecked
                return Flux.fromIterable(this.eventMessages.get(objectId))
            }
        }

        @Override
        <I extends Id<A, I>, A extends AggregateRoot<A, I>> Mono<Boolean> saveEventMessages(List<EventMessage<I, ? extends Event<I>>> eventMessages, Class<A> claßß) {
            if (!eventMessages.isEmpty()) {
                //noinspection unchecked
                this.eventMessages.computeIfAbsent(eventMessages.get(0).eventMeta().objectId(), id -> new ArrayList<>()).addAll(eventMessages)
            }
            return Mono.just(true)
        }
    }

    @MockBean
    @Replaces(SagaStore)
    static class InMemorySagaStore extends AbstractSagaStore {

        private final Map<Id, List<Saga.HandledEvent>> sagaEvents = new LinkedHashMap<>()
        private final Map<Id, SagaData> sagas = new HashMap<>()

        @Override
        Flux<SagaData> doFindAssociatedSagas(Id id) {
            return Flux.fromIterable(sagas.values())
                    .filter(sagaData -> sagaData.associatedEntities().contains(id))
                    .filter(SagaData::live)
        }

        @Override
        void doStore(SagaData sagaData) {
            this.sagaEvents.putIfAbsent(sagaData.id(), new ArrayList<>())
            this.sagaEvents.get(sagaData.id()).addAll(sagaData.handledEvents())
            this.sagas.put(sagaData.id(), new SagaData(sagaData.id(), sagaData.live(), sagaData.associatedEntities(), sagaData.currentVersion(), sagaData.created(), sagaData.scheduledTimeout(), sagaData.sagaClassName(), sagaEvents.get(sagaData.id())))
        }
    }
}

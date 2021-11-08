package be.idevelop.cqrs


import spock.lang.Specification

class CqrsSchedulerTest extends Specification {

    def 'verify that always the same scheduler is used for the same object id'() {
        given:
        def scheduler = new CqrsScheduler('ABC', 3)
        def objectId1 = new ObjectId(1)
        def objectId2 = new ObjectId(2)
        def objectId3 = new ObjectId(3)

        when:
        def schedule1 = scheduler.schedule(objectId1)
        def schedule2 = scheduler.schedule(objectId2)
        def schedule3 = scheduler.schedule(objectId3)

        then:
        for (i in 0..<30000) {
            assert scheduler.schedule(objectId1) == schedule1
            assert scheduler.schedule(objectId2) == schedule2
            assert scheduler.schedule(objectId3) == schedule3
        }
    }

    private static class ObjectId implements Id {

        private final int value

        private ObjectId(int value) {
            this.value = value
        }

        boolean equals(o) {
            if (this.is(o)) return true
            if (getClass() != o.class) return false

            ObjectId objectId = (ObjectId) o

            if (value != objectId.value) return false

            return true
        }

        int hashCode() {
            return value
        }

        @Override
        Class getEntityClass() {
            return null
        }
    }
}

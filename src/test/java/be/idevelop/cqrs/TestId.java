package be.idevelop.cqrs;

public class TestId implements Id<TestAggregateRoot, TestId> {

    @Override
    public Class<TestAggregateRoot> getEntityClass() {
        return TestAggregateRoot.class;
    }
}

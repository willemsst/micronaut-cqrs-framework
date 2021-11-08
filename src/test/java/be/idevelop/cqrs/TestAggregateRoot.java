package be.idevelop.cqrs;

public class TestAggregateRoot extends AggregateRoot<TestAggregateRoot, TestId> {
    protected TestAggregateRoot(TestId id) {
        super(id);
    }

    public void init() {
        apply(new TestCreatedEvent());
    }

    public void validate() {
        apply(new TestValidatedEvent());
    }
}

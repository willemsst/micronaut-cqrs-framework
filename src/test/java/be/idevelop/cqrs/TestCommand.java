package be.idevelop.cqrs;

public class TestCommand implements Command<TestId> {

    final TestId testId;
    final CommandMeta<TestId> metadata;

    TestCommand(TestId testId) {
        this.testId = testId;
        this.metadata = new CommandMeta<>(testId);
    }

    @Override
    public CommandMeta<TestId> meta() {
        return metadata;
    }
}

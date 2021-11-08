package be.idevelop.cqrs;

public class ValidateTestObjectCommand implements Command<TestId> {

    final TestId testId;
    final CommandMeta<TestId> metadata;

    ValidateTestObjectCommand(TestId testId) {
        this.testId = testId;
        this.metadata = new CommandMeta<>(testId);
    }

    @Override
    public CommandMeta<TestId> meta() {
        return metadata;
    }
}

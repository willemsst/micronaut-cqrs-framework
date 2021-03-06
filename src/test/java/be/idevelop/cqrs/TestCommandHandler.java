package be.idevelop.cqrs;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TestCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCommandHandler.class);

    @CommandHandler
    TestAggregateRoot onCommand(TestAggregateRoot testObject, TestCommand command) {
        LOGGER.debug("Handling test command {} for {}", command, testObject);
        testObject.init();
        return testObject;
    }

    @CommandHandler
    TestAggregateRoot onCommand(TestAggregateRoot testObject, ValidateTestObjectCommand command) {
        LOGGER.debug("Handling test command {} for {}", command, testObject);
        testObject.validate();
        return testObject;
    }
}

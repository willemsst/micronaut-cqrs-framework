package be.idevelop.cqrs;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TestCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCommandHandler.class);

    @CommandHandler
    void onCommand(TestAggregateRoot testObject, TestCommand command) {
        LOGGER.debug("Handling test command {} for {}", command, testObject);
        testObject.init();
    }

    @CommandHandler
    void onCommand(TestAggregateRoot testObject, ValidateTestObjectCommand command) {
        LOGGER.debug("Handling test command {} for {}", command, testObject);
        testObject.validate();
    }
}

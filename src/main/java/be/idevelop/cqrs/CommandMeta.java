package be.idevelop.cqrs;

import java.time.Instant;

public record CommandMeta<I extends Id<?, I>>(CommandId commandId, I objectId, Instant timestamp) {

    public CommandMeta(I objectId) {
        this(CommandId.generateNew(), objectId, Instant.now());
    }
}

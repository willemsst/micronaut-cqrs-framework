package be.idevelop.cqrs;

import java.util.UUID;

final record CommandId(UUID id) {

    public static CommandId generateNew() {
        return new CommandId(UUID.randomUUID()); // TODO: user UUIDs that are sortable on time
    }
}

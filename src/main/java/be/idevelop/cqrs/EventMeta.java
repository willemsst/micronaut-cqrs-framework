package be.idevelop.cqrs;

import java.time.Instant;

public final record EventMeta<I extends Id<?, I>>(

        I objectId,

        long version,

        Instant timestamp) {

}

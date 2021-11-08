package be.idevelop.cqrs;

import java.util.concurrent.TimeUnit;

public @interface TimeoutSaga {
    int timeout();

    TimeUnit timeoutUnit();

    TimeoutType type();
}

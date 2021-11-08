package be.idevelop.cqrs;

import java.time.temporal.ChronoUnit;

public record TimeoutStrategy(long value, ChronoUnit unit, TimeoutType timeoutType) {
}

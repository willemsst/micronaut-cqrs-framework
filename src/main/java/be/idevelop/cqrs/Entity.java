package be.idevelop.cqrs;

public interface Entity<THIS extends Entity<THIS, I>, I extends Id<THIS, I>> {
}

package be.idevelop.cqrs;

public interface Id<A extends Entity<A, I>, I extends Id<A, I>> {

    Class<A> getEntityClass();

}

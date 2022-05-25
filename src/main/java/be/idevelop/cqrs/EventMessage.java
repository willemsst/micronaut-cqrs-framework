package be.idevelop.cqrs;

public record EventMessage<I extends Id<?, I>, E extends Record>(EventMeta<I> eventMeta, E event) {

    public I objectId() {
        return eventMeta.objectId();
    }

}

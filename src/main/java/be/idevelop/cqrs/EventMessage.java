package be.idevelop.cqrs;

public final record EventMessage<I extends Id<?, I>, E extends Event<I>>(EventMeta<I> eventMeta, E event) {

    public I objectId() {
        return eventMeta.objectId();
    }

}

package be.idevelop.cqrs;

public record EventMessage<I extends Id<?, I>>(EventMeta<I> eventMeta, Record event) {

    public I objectId() {
        return eventMeta.objectId();
    }

}

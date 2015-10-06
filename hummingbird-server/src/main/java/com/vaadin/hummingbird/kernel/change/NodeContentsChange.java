package com.vaadin.hummingbird.kernel.change;

public abstract class NodeContentsChange extends NodeChange {
    private Object key;

    public NodeContentsChange(Object key) {
        this.key = key;
    }

    /**
     * Returns the key of the object in the node which has changed
     *
     * @return the key of the object in the node which has changed
     */
    public Object getKey() {
        return key;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [key=" + key + "]";
    }

}

package com.vaadin.hummingbird.kernel.change;

public abstract class NodeDataChange extends NodeContentsChange {
    private Object value;

    public NodeDataChange(Object key, Object value) {
        super(key);
        this.value = value;
    }

    /**
     * Returns the new value of the object in the node which has changed
     *
     * @return the new value of the object in the node which has changed
     */
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [key=" + getKey() + ", value="
                + getValue() + "]";
    }

}

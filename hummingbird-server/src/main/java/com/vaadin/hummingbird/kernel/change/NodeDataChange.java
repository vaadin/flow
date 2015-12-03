package com.vaadin.hummingbird.kernel.change;

import java.util.Objects;

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

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj)
                && Objects.equals(value, ((NodeDataChange) obj).value);
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 37 + Objects.hashCode(value);
    }

}

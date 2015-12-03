package com.vaadin.hummingbird.kernel.change;

import java.util.Objects;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else {
            return Objects.equals(key, ((NodeContentsChange) obj).key);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

}

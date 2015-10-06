package com.vaadin.hummingbird.kernel.change;

public abstract class NodeListChange extends NodeContentsChange {
    private int index;
    private Object value;

    public NodeListChange(int index, Object key, Object value) {
        super(key);
        this.index = index;
        this.value = value;
    }

    /**
     * Returns the index of the modified value in the list
     *
     * @return the index of the modified value in the list
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the value of the changed object in the list
     *
     * @return the value of the changed object in the list
     */
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [key=" + getKey() + ", index="
                + index + ", value=" + value + "]";
    }

}

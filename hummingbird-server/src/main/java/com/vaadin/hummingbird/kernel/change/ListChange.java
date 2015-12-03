package com.vaadin.hummingbird.kernel.change;

import java.util.Objects;

public abstract class ListChange extends NodeChange {
    private int index;
    private Object value;

    public ListChange(int index, Object value) {
        super();
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
     * Sets the index of the modified value in the list
     *
     * @param index
     *            the index of the modified value in the list
     */
    public void setIndex(int index) {
        this.index = index;
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
        return getClass().getSimpleName() + " [index=" + index + ", value="
                + value + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        ListChange that = (ListChange) obj;
        return index == that.index && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return index * 37 + Objects.hashCode(value);
    }

}

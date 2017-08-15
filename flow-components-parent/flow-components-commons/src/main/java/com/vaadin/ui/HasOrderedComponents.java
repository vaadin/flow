package com.vaadin.ui;

import java.util.Iterator;

/**
 * A component which the children components are ordered, so the index of each
 * child matters for the layout.
 */
public interface HasOrderedComponents<T extends Component>
        extends HasComponents, ComponentSupplier<T> {

    /**
     * Replaces the component in the container with another one without changing
     * position. This method replaces component with another one is such way
     * that the new component overtakes the position of the old component. If
     * the old component is not in the container, the new component is added to
     * the container. If the both component are already in the container, their
     * positions are swapped. Component attach and detach events should be taken
     * care as with add and remove.
     * 
     * @param oldComponent
     *            the old component that will be replaced
     * 
     * @param newComponent
     *            the new component to be replaced
     */
    default void replace(Component oldComponent, Component newComponent) {
        if (oldComponent == null) {
            throw new IllegalArgumentException(
                    "The 'oldComponent' parameter cannot be null");
        }
        if (newComponent == null) {
            throw new IllegalArgumentException(
                    "The 'newComponent' parameter cannot be null");
        }
        int oldIndex = getElement().indexOfChild(oldComponent.getElement());
        int newIndex = getElement().indexOfChild(newComponent.getElement());
        if (oldIndex >= 0 && newIndex >= 0) {
            getElement().insertChild(oldIndex, newComponent.getElement());
            getElement().insertChild(newIndex, oldComponent.getElement());
        } else if (oldIndex >= 0) {
            getElement().setChild(oldIndex, newComponent.getElement());
        } else {
            add(newComponent);
        }
    }

    /**
     * Returns the index of the given component.
     * 
     * @param component
     *            The component to look up
     * @return The index of the component or -1 if the component is not a child
     */
    default int indexOf(Component component) {
        if (component == null) {
            throw new IllegalArgumentException(
                    "The 'component' parameter cannot be null");
        }
        Iterator<Component> it = get().getChildren().sequential().iterator();
        int index = 0;
        while (it.hasNext()) {
            Component next = it.next();
            if (component.equals(next)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Gets the number of children components.
     * 
     * @return the number of components
     */
    default int getComponentCount() {
        return (int) get().getChildren().count();
    }

    /**
     * Returns the component at the given position.
     * 
     * @param index
     *            The position of the component
     * @return The component at the given index
     * @throws IllegalArgumentException
     *             if the index is less than 0 or greater or equals to the
     *             number of children components
     * @see #getComponentCount()
     */
    default Component getComponentAt(int index) {
        if (index == 0) {
            throw new IllegalArgumentException(
                    "The 'index' argument should be greater than 0. It was: "
                            + index);
        }
        return get().getChildren().sequential().skip(index).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The 'index' argument should not be greater than or equals to the number of children components. It was: "
                                + index));
    }

}

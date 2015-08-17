package com.vaadin.ui;

import java.util.Iterator;

import com.vaadin.hummingbird.kernel.Element;

/**
 * An implementation helper for component containers which store their
 * children's elements as immediate childrens to the container's element.
 * Application developers should not use this class
 */
public class AbstractSimpleDOMComponentContainer
        extends AbstractComponentContainer {
    /**
     * Add a component into this container. The component is added to the right
     * or under the previous component.
     *
     * @param c
     *            the component to be added.
     */
    @Override
    public void addComponent(Component c) {
        assert c != null : "Cannot add null as a component";
        try {
            getElement().appendChild(c.getElement());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Component cannot be added inside it's own content", e);
        }
    }

    /**
     * Adds a component into this container. The component is added to the left
     * or on top of the other components.
     *
     * @param c
     *            the component to be added.
     */
    public void addComponentAsFirst(Component c) {
        addComponent(c, 0);
    }

    /**
     * Adds a component into indexed position in this container.
     *
     * @param c
     *            the component to be added.
     * @param index
     *            the index of the component position. The components currently
     *            in and after the position are shifted forwards.
     */
    public void addComponent(Component c, int index) {
        assert c != null : "Cannot add null as a component";

        getElement().insertChild(index, c.getElement());
    }

    /**
     * Removes the component from this container.
     *
     * @param c
     *            the component to be removed.
     */
    @Override
    public void removeComponent(Component c) {
        assert c != null : "Cannot remove null component";

        if (!hasChild(c)) {
            throw new IllegalArgumentException(
                    ComponentContainer.ERROR_NOT_A_CHILD);
        }

        c.getElement().removeFromParent(); // Fires detach
    }

    @Override
    public void replaceComponent(Component oldComponent,
            Component newComponent) {
        if (!hasChild(oldComponent)) {
            throw new IllegalArgumentException(ERROR_NOT_A_CHILD);
        }
        if (hasChild(newComponent)) {
            throw new IllegalArgumentException(
                    "The new component is already a child of this layout");
        }

        int insertIndex = getElement().getChildIndex(oldComponent.getElement());

        removeComponent(oldComponent);
        addComponent(newComponent, insertIndex);
    }

    /**
     * Checks if the given component is a child of this component container
     *
     * @param component
     *            The component to check
     * @return true if the component is a child of this component container,
     *         false otherwise
     */
    public boolean hasChild(Component component) {
        return getElement().hasChild(component.getElement());
    }

    /**
     * Gets the component container iterator for going trough all the components
     * in the container.
     *
     * @return the Iterator of the components inside the container.
     */
    @Override
    public Iterator<Component> iterator() {
        return new ElementBasedComponentIterator(getElement());
    }

    public static class ElementBasedComponentIterator
            implements Iterator<Component> {

        int index = 0;
        private Element element;

        public ElementBasedComponentIterator(Element element) {
            this.element = element;
        }

        @Override
        public boolean hasNext() {
            return element.getChildCount() > index;
        }

        @Override
        public Component next() {
            return element.getChild(index++).getComponent();
        }

    }

    /**
     * Returns the index of the given component.
     *
     * @param component
     *            The component to look up.
     * @return The index of the component or -1 if the component is not a child.
     */
    public int getComponentIndex(Component component) {
        return getElement().getChildIndex(component.getElement());
    }

    /**
     * Returns the component at the given position.
     *
     * @param index
     *            The position of the component.
     * @return The component at the given index.
     * @throws IndexOutOfBoundsException
     *             If the index is out of range.
     */
    public Component getComponent(int index) throws IndexOutOfBoundsException {
        assert index < getComponentCount() : "getComponent called with index "
                + index + " but there are only " + getComponentCount()
                + " children";
        return getElement().getChild(index).getComponent();
    }

    /**
     * Gets the number of contained components. Consistent with the iterator
     * returned by {@link #getComponentIterator()}.
     *
     * @return the number of contained components
     */
    @Override
    public int getComponentCount() {
        return getElement().getChildCount();
    }

    @Override
    public void removeAllComponents() {
        getElement().removeAllChildren();
    }
}

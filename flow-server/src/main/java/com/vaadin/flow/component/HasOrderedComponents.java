/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.component;

import java.util.Iterator;

import com.vaadin.flow.dom.Element;

/**
 * A component which the children components are ordered, so the index of each
 * child matters for the layout.
 * 
 * @param <T>
 *            the type of the component which implements the interface
 * @since 1.0
 */
public interface HasOrderedComponents<T extends Component>
        extends HasComponents {

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
     *            the old component that will be replaced. Can be
     *            <code>null</code>, which will make the newComponent to be
     *            added to the layout without replacing any other
     * 
     * @param newComponent
     *            the new component to be replaced. Can be <code>null</code>,
     *            which will make the oldComponent to be removed from the layout
     *            without adding any other
     */
    default void replace(Component oldComponent, Component newComponent) {
        if (oldComponent == null && newComponent == null) {
            // NO-OP
            return;
        }
        if (oldComponent == null) {
            add(newComponent);
        } else if (newComponent == null) {
            remove(oldComponent);
        } else {
            Element element = getElement();
            int oldIndex = element.indexOfChild(oldComponent.getElement());
            int newIndex = element.indexOfChild(newComponent.getElement());
            if (oldIndex >= 0 && newIndex >= 0) {
                element.insertChild(oldIndex, newComponent.getElement());
                element.insertChild(newIndex, oldComponent.getElement());
            } else if (oldIndex >= 0) {
                element.setChild(oldIndex, newComponent.getElement());
            } else {
                add(newComponent);
            }
        }
    }

    /**
     * Returns the index of the given component.
     * 
     * @param component
     *            the component to look up, can not be <code>null</code>
     * @return the index of the component or -1 if the component is not a child
     */
    default int indexOf(Component component) {
        if (component == null) {
            throw new IllegalArgumentException(
                    "The 'component' parameter cannot be null");
        }
        Iterator<Component> it = ((T) this).getChildren().sequential()
                .iterator();
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
        return (int) ((T) this).getChildren().count();
    }

    /**
     * Returns the component at the given position.
     * 
     * @param index
     *            the position of the component, must be greater than or equals
     *            to 0 and less than the number of children components
     * @return The component at the given index
     * @throws IllegalArgumentException
     *             if the index is less than 0 or greater than or equals to the
     *             number of children components
     * @see #getComponentCount()
     */
    default Component getComponentAt(int index) {
        if (index < 0) {
            throw new IllegalArgumentException(
                    "The 'index' argument should be greater than or equal to 0. It was: "
                            + index);
        }
        return ((T) this).getChildren().sequential().skip(index).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The 'index' argument should not be greater than or equals to the number of children components. It was: "
                                + index));
    }

}

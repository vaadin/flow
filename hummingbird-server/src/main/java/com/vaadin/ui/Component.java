package com.vaadin.ui;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import com.vaadin.hummingbird.dom.Element;

/**
 * A Component is a higher level abstraction of an {@link Element} or a
 * hierarchy of {@link Element}s.
 * <p>
 * A Component must attach itself to its element, as returned by
 * {@link #getElement()}.
 *
 * @author Vaadin
 * @since
 */
public interface Component {
    /**
     * Gets the root element of this component.
     * <p>
     * Each component must have exactly one root element. This element is
     * attached to the {@link Element} tree when this component is attached to a
     * parent component.
     *
     * @return the root element of this component.
     */
    Element getElement();

    /**
     * Gets the parent component of this component.
     * <p>
     * A component can only have one parent.
     *
     * @return the parent component
     */
    default Optional<Component> getParent() {
        Element parentElement = getElement().getParent();
        while (parentElement != null
                && !parentElement.getComponent().isPresent()) {
            parentElement = parentElement.getParent();
        }

        if (parentElement == null) {
            return Optional.empty();
        }

        return parentElement.getComponent();
    }

    /**
     * Gets the child components of this component.
     * <p>
     * The default implementation finds child components by traversing each
     * child {@link Element} tree.
     *
     * @return the child components of this component
     */
    default Stream<Component> getChildren() {
        Builder<Component> childComponents = Stream.builder();
        getElement().getChildren().forEach(childElement -> {
            Optional<Component> firstChild = ComponentUtil
                    .findFirstComponent(childElement);
            if (firstChild.isPresent()) {
                childComponents.add(firstChild.get());
            }
        });
        return childComponents.build();
    }

}

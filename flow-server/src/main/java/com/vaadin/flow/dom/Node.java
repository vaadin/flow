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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;

/**
 * Represents a node in the DOM.
 * <p>
 * Contains methods for updating and querying hierarchical structure.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <N>
 *            The narrowed type of the node
 */
public abstract class Node<N extends Node<N>> implements Serializable {

    static final String CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN = "Cannot %s element with index %d when there are %d children";

    static final String THE_CHILDREN_ARRAY_CANNOT_BE_NULL = "The children array cannot be null";

    private final ElementStateProvider stateProvider;
    private final StateNode node;

    /**
     * Private constructor for initializing with an existing node and state
     * provider.
     *
     * @param node
     *            the state node, not null
     * @param stateProvider
     *            the state provider, not null
     */
    protected Node(StateNode node, ElementStateProvider stateProvider) {
        assert node != null;
        assert stateProvider != null;

        if (!stateProvider.supports(node)) {
            throw new IllegalArgumentException(
                    stateProvider.getClass().getSimpleName()
                            + " does not support the given state node");
        }

        this.stateProvider = stateProvider;
        this.node = node;
    }

    /**
     * Gets the node this element is connected to.
     * <p>
     * This method is meant for internal use only.
     *
     * @return the node for this element
     */
    public StateNode getNode() {
        return node;
    }

    /**
     * Gets the state provider for this element.
     * <p>
     * This method is meant for internal use only.
     *
     * @return the state provider for this element
     */
    public ElementStateProvider getStateProvider() {
        return stateProvider;
    }

    /**
     * Gets the number of child elements.
     *
     * @return the number of child elements
     */
    public int getChildCount() {
        return getStateProvider().getChildCount(getNode());
    }

    /**
     * Returns the child element at the given position.
     *
     * @param index
     *            the index of the child element to return
     * @return the child element
     */
    public Element getChild(int index) {
        if (index < 0 || index >= getChildCount()) {
            throw new IllegalArgumentException(String.format(
                    CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN, "get",
                    index, getChildCount()));
        }

        return getStateProvider().getChild(getNode(), index);
    }

    /**
     * Gets all the children of this element.
     *
     * @return a stream of children
     */
    public Stream<Element> getChildren() {
        return IntStream.range(0, getChildCount()).mapToObj(this::getChild);
    }

    /**
     * Adds the given children as the last children of this element.
     *
     * @param children
     *            the element(s) to add
     * @return this element
     */
    public N appendChild(Element... children) {
        if (children == null) {
            throw new IllegalArgumentException(
                    THE_CHILDREN_ARRAY_CANNOT_BE_NULL);
        }

        insertChild(getChildCount(), children);

        return getSelf();
    }

    /**
     * Appends the given children as the virtual children of the element.
     * <p>
     * The virtual child is not really a child of the DOM element. The
     * client-side counterpart is created in the memory but it's not attached to
     * the DOM tree. The resulting element is referenced via the server side
     * {@link Element} in JS function call as usual.
     *
     * @param children
     *            the element(s) to add
     * @return this element
     */
    public N appendVirtualChild(Element... children) {
        if (children == null) {
            throw new IllegalArgumentException(
                    THE_CHILDREN_ARRAY_CANNOT_BE_NULL);
        }

        for (Element child : children) {
            if (child == null) {
                throw new IllegalArgumentException(
                        "Element to insert must not be null");
            }
            Node<?> parentNode = child.getParentNode();
            if (parentNode != null) {
                throw new IllegalArgumentException(
                        "Element to insert already has a parent and can't "
                                + "be added as a virtual child");
            }
            getStateProvider().appendVirtualChild(getNode(), child,
                    NodeProperties.IN_MEMORY_CHILD, null);
            ensureChildHasParent(child, true);
        }

        return getSelf();
    }

    /**
     * Removes the given children that have been attached as the virtual
     * children of this element.
     * <p>
     * The virtual child is not really a child of the DOM element. The
     * client-side counterpart is created in the memory but it's not attached to
     * the DOM tree. The resulting element is referenced via the server side
     * {@link Element} in JS function call as usual. *
     * 
     * @param children
     *            the element(s) to remove
     * @return this element
     */
    /*
     * The use case for removing virtual children is when exported Flow web
     * components are detached from their parent due to missing heart beats +
     * timeout.
     */
    public N removeVirtualChild(Element... children) {
        if (children == null) {
            throw new IllegalArgumentException(
                    THE_CHILDREN_ARRAY_CANNOT_BE_NULL);
        }

        if (getNode().hasFeature(VirtualChildrenList.class)) {
            VirtualChildrenList childrenList = getNode()
                    .getFeature(VirtualChildrenList.class);
            for (Element child : children) {
                if (child == null) {
                    throw new IllegalArgumentException(
                            "Element to remove must not be null");
                }
                int index = childrenList.indexOf(child.getNode());
                if (index == -1) {
                    throw new IllegalArgumentException(
                            "Trying to detach a virtual child element from parent that does not have it.");
                }
                childrenList.remove(index);
            }
        }

        return getSelf();
    }

    /**
     * Gets whether this element is a virtual child of its parent.
     *
     * @return <code>true</code> if the element has a parent and the element is
     *         a virtual child of it, <code>false</code> otherwise.
     */
    public boolean isVirtualChild() {
        Node<?> parentNode = getParentNode();
        if (parentNode == null) {
            return false;
        }
        if (!parentNode.getNode().hasFeature(VirtualChildrenList.class)
                || !parentNode.getNode()
                        .getFeatureIfInitialized(VirtualChildrenList.class)
                        .isPresent()) {
            return false;
        }
        Iterator<StateNode> iterator = parentNode.getNode()
                .getFeature(VirtualChildrenList.class).iterator();
        while (iterator.hasNext()) {
            StateNode child = iterator.next();
            if (getNode().equals(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts the given child element(s) at the given position.
     *
     * @param index
     *            the position at which to insert the new child
     * @param children
     *            the child element(s) to insert
     * @return this element
     */
    public N insertChild(int index, Element... children) {
        if (children == null) {
            throw new IllegalArgumentException(
                    THE_CHILDREN_ARRAY_CANNOT_BE_NULL);
        }
        if (index > getChildCount()) {
            throw new IllegalArgumentException(String.format(
                    CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN, "insert",
                    index, getChildCount()));
        }

        for (int i = 0, insertIndex = index; i < children.length; i++, insertIndex++) {
            Element child = children[i];
            if (child == null) {
                throw new IllegalArgumentException(
                        "Element to insert must not be null");
            }
            if (equals(child.getParentNode())) {
                int childIndex = indexOfChild(child);
                if (childIndex == insertIndex) {
                    // No-op of inserting to the current position
                    continue;
                } else if (childIndex < insertIndex) {
                    // Adjust target index if the new child is already our
                    // child,
                    // and we will be removing it from before the target index
                    insertIndex--;
                }
            }
            child.removeFromParent();
            getStateProvider().insertChild(node, insertIndex, child);
            ensureChildHasParent(child, true);
        }

        return getSelf();
    }

    /**
     * Returns the index of the specified {@code child} in the children list, or
     * -1 if this list does not contain the {@code child}.
     *
     * @param child
     *            the child element
     * @return index of the {@code child} or -1 if it's not a child
     */
    public int indexOfChild(Element child) {
        if (child == null) {
            throw new IllegalArgumentException(
                    "Child parameter cannot be null");
        }
        if (!equals(child.getParentNode())) {
            return -1;
        }
        for (int i = 0; i < getChildCount(); i++) {
            Element element = getChild(i);
            if (element.equals(child)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Replaces the child at the given position with the given child element.
     *
     * @param index
     *            the position of the child element to replace
     * @param child
     *            the child element to insert
     * @return this element
     */
    public N setChild(int index, Element child) {
        if (child == null) {
            throw new IllegalArgumentException("The child cannot be null");
        }
        int childCount = getChildCount();
        if (index < 0) {
            throw new IllegalArgumentException(String.format(
                    CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN, "set",
                    index, getChildCount()));
        } else if (index < childCount) {
            if (getChild(index).equals(child)) {
                // Already there
                return getSelf();
            }
            removeChild(index);
            insertChild(index, child);
        } else if (index == childCount) {
            insertChild(index, child);
        } else {
            throw new IllegalArgumentException(String.format(
                    CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN, "set",
                    index, getChildCount()));
        }
        return getSelf();
    }

    /**
     * Removes the given child element(s).
     *
     * @param children
     *            the child element(s) to remove
     * @return this element
     */
    public N removeChild(Element... children) {
        if (children == null) {
            throw new IllegalArgumentException(
                    THE_CHILDREN_ARRAY_CANNOT_BE_NULL);
        }

        for (Element child : children) {
            ensureChildHasParent(child, false);
            getStateProvider().removeChild(getNode(), child);
        }
        return getSelf();
    }

    /**
     * Removes the child at the given index.
     *
     * @param index
     *            the index of the child to remove
     * @return this element
     */
    public N removeChild(int index) {
        if (index < 0 || index >= getChildCount()) {
            throw new IllegalArgumentException(String.format(
                    CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN, "remove",
                    index, getChildCount()));

        }

        getStateProvider().removeChild(getNode(), index);
        return getSelf();
    }

    /**
     * Removes all child elements, including elements only present at the
     * client-side.
     *
     * @return this element
     */
    public N removeAllChildren() {
        getStateProvider().removeAllChildren(getNode());

        return getSelf();
    }

    /**
     * Gets the parent node.
     *
     * @return the parent node or null if this element does not have a parent
     */
    @SuppressWarnings("rawtypes")
    public Node getParentNode() {
        return getStateProvider().getParent(getNode());
    }

    /**
     * Gets the narrow typed reference to this object.
     *
     * @return this object casted to its type
     */
    protected abstract N getSelf();

    @Override
    public int hashCode() {
        return Objects.hash(getNode(), getStateProvider());
    }

    /**
     * Applies the {@code visitor} for the node.
     *
     * @param visitor
     *            the visitor to apply to the node
     *
     * @return this element
     */
    public N accept(NodeVisitor visitor) {
        getStateProvider().visit(getNode(), visitor);
        return getSelf();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Node<?> other = (Node<?>) obj;

        // Constructors guarantee that neither getNode() nor stateProvider is
        // null
        return other.getNode().equals(getNode())
                && other.getStateProvider().equals(getStateProvider());
    }

    /**
     * Ensures that the {@code child} has the correct parent.
     * <p>
     * Default implementation doesn't do anything. Subclasses may override the
     * method to implement their own behavior.
     *
     * @param child
     *            the element to check for its parent
     * @param internalCheck
     *            whether to use assertions or throw an exception on failure
     */
    protected void ensureChildHasParent(Element child, boolean internalCheck) {
        if (!Objects.equals(this, child.getParentNode())) {
            if (internalCheck) {
                assert false : "Child should have this element as a parent";
            } else {
                throw new IllegalArgumentException(
                        "Child should have this element as a parent");
            }
        }
    }

    /**
     * Attaches a child element with the given {@code tagName} which is the next
     * sibling for the {@code previousSibling}.
     * <p>
     * The {@code previousSibling} parameter value can be {@code null} which
     * means that the very first child with the given {@code tagName} will be
     * used to attach (if any).
     * <p>
     * This method may be used to get a server side element for the client side
     * DOM element which has been created on the client side aside of the
     * server.
     * <p>
     * The element is not returned right away since it may not exist at all on
     * the client side and its index in the children list is unknown. The
     * provided {@code callback} is used instead to provide the mapped
     * server-side element in case it has been found or report an error if it
     * doesn't exist.
     * <p>
     * This API is experimental and disabled for public usage.
     *
     * @param tagName
     *            the tag name of the element to attach, not {@code null}
     * @param previousSibling
     *            previous sibling, may be {@code null}
     * @param callback
     *            the callback which will be invoked with a server side element
     *            instance or an error will be reported, not {@code null}
     * @return this element
     */
    private N attachExistingElement(String tagName, Element previousSibling,
            ChildElementConsumer callback) {
        getStateProvider().attachExistingElement(getNode(), tagName,
                previousSibling, callback);
        return getSelf();
    }
}

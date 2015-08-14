/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.ui;

import java.util.Iterator;
import java.util.logging.Logger;

import com.vaadin.annotations.HTML;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.event.LayoutEvents.LayoutClickNotifier;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.EventId;

@SuppressWarnings("serial")
@HTML("vaadin://bower_components/iron-flex-layout/classes/iron-flex-layout.html")
public abstract class AbstractOrderedLayout extends AbstractComponentContainer
        implements ComponentContainer.AlignmentHandler,
        ComponentContainer.SpacingHandler, LayoutClickNotifier,
        ComponentContainer.MarginHandler {

    // private AbstractOrderedLayoutServerRpc rpc = new
    // AbstractOrderedLayoutServerRpc() {
    //
    // @Override
    // public void layoutClick(MouseEventDetails mouseDetails,
    // Connector clickedConnector) {
    // fireEvent(LayoutClickEvent.createEvent(AbstractOrderedLayout.this,
    // mouseDetails, clickedConnector));
    // }
    // };

    protected static final String CLASS_FLEX_CHILDREN = "flex-children";

    public static final Alignment ALIGNMENT_DEFAULT = Alignment.TOP_LEFT;

    private static final String ERROR_NOT_A_CHILD = "The given component is not a child of this layout";

    private Alignment defaultComponentAlignment = ALIGNMENT_DEFAULT;

    /* Child component alignments */

    /**
     * Constructs an empty AbstractOrderedLayout.
     */
    public AbstractOrderedLayout() {
        getElement().addClass("layout");
        getElement().addClass(CLASS_FLEX_CHILDREN);
    }

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
            throw new IllegalArgumentException(ERROR_NOT_A_CHILD);
        }

        c.getElement().removeFromParent(); // Fires detach
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

    /* Documented in superclass */
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
        int expandRatio = getExpandRatio(oldComponent);
        Alignment alignment = getComponentAlignment(oldComponent);

        removeComponent(oldComponent);
        addComponent(newComponent, insertIndex);

        setExpandRatio(newComponent, expandRatio);
        setComponentAlignment(newComponent, alignment);
    }

    @Override
    public void setComponentAlignment(Component childComponent,
            Alignment alignment) {
        if (!hasChild(childComponent)) {
            throw new IllegalArgumentException(
                    "The component is not a child of this layout");
        }

        Alignment currentAlignment = getComponentAlignment(childComponent);

        childComponent.getElement()
                .removeClass(currentAlignment.getClassName());
        if (!alignment.getClassName().equals("")) {
            childComponent.getElement().addClass(alignment.getClassName());
        }
    }

    @Override
    public Alignment getComponentAlignment(Component childComponent) {
        if (!hasChild(childComponent)) {
            throw new IllegalArgumentException(ERROR_NOT_A_CHILD);
        }

        for (Alignment a : Alignment.values()) {
            if (!a.getClassName().equals("")
                    && childComponent.getElement().hasClass(a.getClassName())) {
                return a;
            }
        }
        return defaultComponentAlignment;
    }

    /**
     * <p>
     * This method is used to control how excess space in layout is distributed
     * among components. Excess space may exist if layout is sized and contained
     * non relatively sized components don't consume all available space.
     *
     * <p>
     * Example how to distribute 1:3 (33%) for component1 and 2:3 (67%) for
     * component2 :
     *
     * <code>
     * layout.setExpandRatio(component1, 1);<br>
     * layout.setExpandRatio(component2, 2);
     * </code>
     *
     * <p>
     * If no ratios have been set, the excess space is distributed evenly among
     * all components.
     *
     * <p>
     * Note, that width or height (depending on orientation) needs to be defined
     * for this method to have any effect.
     *
     * @see Sizeable
     *
     * @param component
     *            the component in this layout which expand ratio is to be set
     * @param ratio
     */
    public void setExpandRatio(Component component, int ratio) {
        if (ratio < 0 || ratio > 10) {
            throw new IllegalArgumentException(
                    "Only expand ratios between 0 and 10 are supported");
        }
        if (!hasChild(component)) {
            throw new IllegalArgumentException(ERROR_NOT_A_CHILD);
        }

        String oldClass = "flex-" + getExpandRatio(component);
        component.getElement().removeClass(oldClass);

        if (ratio != 0) {
            String newClass = "flex-" + ratio;
            component.getElement().addClass(newClass);
            getElement().removeClass(CLASS_FLEX_CHILDREN);
        } else {
            if (!anyExpandsSet()) {
                getElement().addClass(CLASS_FLEX_CHILDREN);
            }

        }
    }

    private boolean anyExpandsSet() {
        for (Component c : this) {
            if (getExpandRatio(c) != 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the expand ratio of given component.
     *
     * @param component
     *            which expand ratios is requested
     * @return expand ratio of given component, 0.0f by default.
     */
    public int getExpandRatio(Component component) {
        if (!hasChild(component)) {
            throw new IllegalArgumentException(ERROR_NOT_A_CHILD);
        }

        Element childElement = component.getElement();
        String childClass = childElement.getAttribute("class");
        if (childClass == null) {
            return 0;
        }

        String[] classes = childClass.split(" ");
        for (String cls : classes) {
            if (cls.startsWith("flex-")) {
                try {
                    return Integer.parseInt(cls.substring("flex-".length()));
                } catch (NumberFormatException e) {
                    // Some other class, like "flex-foo", keep looking
                }
            }
        }

        // String newAttr = Arrays.stream(childClass.split(" ")).filter(c -> {
        // return !c.startsWith("flex-");
        // }).collect(Collectors.joining(" "));
        //
        // newAttr+= " flex-";
        // newAttr+=
        // if (component.getElement().getAttribute("class"))
        return 0;
    }

    @Override
    public void addLayoutClickListener(LayoutClickListener listener) {
        addListener(EventId.LAYOUT_CLICK_EVENT_IDENTIFIER,
                LayoutClickEvent.class, listener,
                LayoutClickListener.clickMethod);
    }

    @Override
    public void removeLayoutClickListener(LayoutClickListener listener) {
        removeListener(EventId.LAYOUT_CLICK_EVENT_IDENTIFIER,
                LayoutClickEvent.class, listener);
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
        return getElement().getChild(index).getComponent();
    }

    @Override
    public Alignment getDefaultComponentAlignment() {
        return defaultComponentAlignment;
    }

    @Override
    public void setDefaultComponentAlignment(Alignment defaultAlignment) {
        defaultComponentAlignment = defaultAlignment;
    }

    private void applyLayoutSettings(Component target, Alignment alignment,
            int expandRatio) {
        setComponentAlignment(target, alignment);
        setExpandRatio(target, expandRatio);
    }

    private static Logger getLogger() {
        return Logger.getLogger(AbstractOrderedLayout.class.getName());
    }
}

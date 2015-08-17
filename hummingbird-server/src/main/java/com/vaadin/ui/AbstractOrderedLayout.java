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
public abstract class AbstractOrderedLayout extends SimpleDOMComponentContainer
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

    private Alignment defaultComponentAlignment = ALIGNMENT_DEFAULT;

    /* Child component alignments */

    /**
     * Constructs an empty AbstractOrderedLayout.
     */
    public AbstractOrderedLayout() {
        getElement().addClass("layout");
        getElement().addClass(CLASS_FLEX_CHILDREN);
    }

    @Override
    public void replaceComponent(Component oldComponent,
            Component newComponent) {
        int expandRatio = getExpandRatio(oldComponent);
        Alignment alignment = getComponentAlignment(oldComponent);
        super.replaceComponent(oldComponent, newComponent);
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

    @Override
    public Alignment getDefaultComponentAlignment() {
        return defaultComponentAlignment;
    }

    @Override
    public void setDefaultComponentAlignment(Alignment defaultAlignment) {
        defaultComponentAlignment = defaultAlignment;
    }

    private static Logger getLogger() {
        return Logger.getLogger(AbstractOrderedLayout.class.getName());
    }
}

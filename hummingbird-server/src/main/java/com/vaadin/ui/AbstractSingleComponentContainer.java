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

import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.AbstractSimpleDOMComponentContainer.ElementBasedComponentIterator;

/**
 * Abstract base class for component containers that have only one child
 * component.
 *
 * For component containers that support multiple children, inherit
 * {@link AbstractComponentContainer} instead of this class.
 *
 * @since 7.0
 */
public abstract class AbstractSingleComponentContainer extends AbstractComponent
        implements SingleComponentContainer {

    @Override
    public int getComponentCount() {
        return getElement().getChildCount();
    }

    @Override
    public Iterator<Component> iterator() {
        return new ElementBasedComponentIterator(getElement());
    }

    @Override
    public Component getContent() {
        if (getElement().getChildCount() == 0) {
            return null;
        } else {
            return getElement().getChild(0).getComponent();
        }
    }

    /**
     * Sets the content of this container. The content is a component that
     * serves as the outermost item of the visual contents.
     *
     * The content must always be set, either with a constructor parameter or by
     * calling this method.
     *
     * Previous versions of Vaadin used a {@link VerticalLayout} with margins
     * enabled as the default content but that is no longer the case.
     *
     * @param content
     *            a component (typically a layout) to use as content
     */
    @Override
    public void setContent(Component content) {
        getElement().removeAllChildren();
        if (content != null) {
            getElement().appendChild(content.getElement());
        }
    }

    /**
     * Utility method for removing a component from its parent (if possible).
     *
     * @param content
     *            component to remove
     */
    // TODO move utility method elsewhere?
    public static void removeFromParent(Component content)
            throws IllegalArgumentException {
        // Verify the appropriate session is locked
        UI parentUI = content.getUI();
        if (parentUI != null) {
            VaadinSession parentSession = parentUI.getSession();
            if (parentSession != null && !parentSession.hasLock()) {
                String message = "Cannot remove from parent when the session is not locked.";
                if (VaadinService.isOtherSessionLocked(parentSession)) {
                    message += " Furthermore, there is another locked session, indicating that the component might be about to be moved from one session to another.";
                }
                throw new IllegalStateException(message);
            }
        }

        content.getElement().removeFromParent();
    }

}
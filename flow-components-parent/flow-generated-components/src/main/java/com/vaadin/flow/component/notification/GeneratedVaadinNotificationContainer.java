/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component.notification;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * The container element for all notifications.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.NotificationContainer#UNKNOWN",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-notification-container")
@HtmlImport("frontend://bower_components/vaadin-notification/src/vaadin-notification.html")
public abstract class GeneratedVaadinNotificationContainer<R extends GeneratedVaadinNotificationContainer<R>>
        extends Component implements HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True when the container is opened
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code opened} property from the webcomponent
     */
    protected boolean isOpenedBoolean() {
        return getElement().getProperty("opened", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True when the container is opened
     * </p>
     * 
     * @param opened
     *            the boolean value to set
     */
    protected void setOpened(boolean opened) {
        getElement().setProperty("opened", opened);
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'top-stretch'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     */
    protected void addToTopStretch(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "top-stretch");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'top-start'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     */
    protected void addToTopStart(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "top-start");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'top-center'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     */
    protected void addToTopCenter(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "top-center");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'top-end'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     */
    protected void addToTopEnd(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "top-end");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'middle'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     */
    protected void addToMiddle(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "middle");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'bottom-start'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     */
    protected void addToBottomStart(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "bottom-start");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'bottom-center'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     */
    protected void addToBottomCenter(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "bottom-center");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'bottom-end'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     */
    protected void addToBottomEnd(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "bottom-end");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'bottom-stretch'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     */
    protected void addToBottomStretch(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "bottom-stretch");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Removes the given child components from this component.
     * 
     * @param components
     *            The components to remove.
     * @throws IllegalArgumentException
     *             if any of the components is not a child of this component.
     */
    protected void remove(Component... components) {
        for (Component component : components) {
            if (getElement().equals(component.getElement().getParent())) {
                component.getElement().removeAttribute("slot");
                getElement().removeChild(component.getElement());
            } else {
                throw new IllegalArgumentException("The given component ("
                        + component + ") is not a child of this component");
            }
        }
    }

    /**
     * Removes all contents from this component, this includes child components,
     * text content as well as child elements that have been added directly to
     * this component using the {@link Element} API.
     */
    protected void removeAll() {
        getElement().getChildren()
                .forEach(child -> child.removeAttribute("slot"));
        getElement().removeAllChildren();
    }
}
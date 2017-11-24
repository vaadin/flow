/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.paper.toolbar;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-toolbar#2.0.0", "Flow#1.0-SNAPSHOT" })
@Tag("paper-toolbar")
@HtmlImport("frontend://bower_components/paper-toolbar/paper-toolbar.html")
public class GeneratedPaperToolbar<R extends GeneratedPaperToolbar<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Controls how the items are aligned horizontally when they are placed at
     * the bottom. Options are {@code start}, {@code center}, {@code end},
     * {@code justified} and {@code around}.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code bottomJustify} property from the webcomponent
     */
    public String getBottomJustify() {
        return getElement().getProperty("bottomJustify");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Controls how the items are aligned horizontally when they are placed at
     * the bottom. Options are {@code start}, {@code center}, {@code end},
     * {@code justified} and {@code around}.
     * </p>
     * 
     * @param bottomJustify
     *            the String value to set
     */
    public void setBottomJustify(String bottomJustify) {
        getElement().setProperty("bottomJustify",
                bottomJustify == null ? "" : bottomJustify);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Controls how the items are aligned horizontally. Options are
     * {@code start}, {@code center}, {@code end}, {@code justified} and
     * {@code around}.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code justify} property from the webcomponent
     */
    public String getJustify() {
        return getElement().getProperty("justify");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Controls how the items are aligned horizontally. Options are
     * {@code start}, {@code center}, {@code end}, {@code justified} and
     * {@code around}.
     * </p>
     * 
     * @param justify
     *            the String value to set
     */
    public void setJustify(String justify) {
        getElement().setProperty("justify", justify == null ? "" : justify);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Controls how the items are aligned horizontally when they are placed in
     * the middle. Options are {@code start}, {@code center}, {@code end},
     * {@code justified} and {@code around}.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code middleJustify} property from the webcomponent
     */
    public String getMiddleJustify() {
        return getElement().getProperty("middleJustify");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Controls how the items are aligned horizontally when they are placed in
     * the middle. Options are {@code start}, {@code center}, {@code end},
     * {@code justified} and {@code around}.
     * </p>
     * 
     * @param middleJustify
     *            the String value to set
     */
    public void setMiddleJustify(String middleJustify) {
        getElement().setProperty("middleJustify",
                middleJustify == null ? "" : middleJustify);
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'top'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     * @return this instance, for method chaining
     */
    public R addToTop(com.vaadin.ui.Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "top");
            getElement().appendChild(component.getElement());
        }
        return get();
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
     * @return this instance, for method chaining
     */
    public R addToMiddle(com.vaadin.ui.Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "middle");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'bottom'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     * @return this instance, for method chaining
     */
    public R addToBottom(com.vaadin.ui.Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "bottom");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Removes the given child components from this component.
     * 
     * @param components
     *            The components to remove.
     * @throws IllegalArgumentException
     *             if any of the components is not a child of this component.
     */
    public void remove(com.vaadin.ui.Component... components) {
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
    public void removeAll() {
        getElement().getChildren()
                .forEach(child -> child.removeAttribute("slot"));
        getElement().removeAllChildren();
    }
}
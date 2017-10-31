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
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * <em>This element has been deprecated in favor of <a href="https://github.com/PolymerElements/app-layout">app-layout</a>.</em>
 * </p>
 * <p>
 * Material design: <a
 * href="https://www.google.com/design/spec/components/toolbars.html"
 * >Toolbars</a>
 * </p>
 * <p>
 * {@code paper-toolbar} is a horizontal bar containing items that can be used
 * for label, navigation, search and actions. The items placed inside the
 * {@code paper-toolbar} are projected into a
 * {@code class=&quot;horizontal center layout&quot;} container inside of
 * {@code paper-toolbar}'s Shadow DOM. You can use flex attributes to control
 * the items' sizing.
 * </p>
 * <p>
 * Example:
 * </p>
 * <p>
 * {@code }`html <paper-toolbar> <paper-icon-button icon="menu"
 * on-tap="menuAction"></paper-icon-button>
 * </p>
 * <div class="title">Title</div> <paper-icon-button icon="more-vert"
 * on-tap="moreAction"></paper-icon-button> </paper-toolbar> {@code }`
 * <p>
 * {@code paper-toolbar} has a standard height, but can made be taller by
 * setting {@code tall} class on the {@code paper-toolbar}. This will make the
 * toolbar 3x the normal height.
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;paper-toolbar class=&quot;tall&quot;&gt; &lt;paper-icon-button icon=&quot;menu&quot;&gt;&lt;/paper-icon-button&gt; &lt;/paper-toolbar&gt; {@code }</code>
 * </p>
 * <p>
 * Apply {@code medium-tall} class to make the toolbar medium tall. This will
 * make the toolbar 2x the normal height.
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;paper-toolbar class=&quot;medium-tall&quot;&gt; &lt;paper-icon-button icon=&quot;menu&quot;&gt;&lt;/paper-icon-button&gt; &lt;/paper-toolbar&gt; {@code }</code>
 * </p>
 * <p>
 * When {@code tall}, items can pin to either the top (default), middle or
 * bottom. Use {@code middle} slot for middle content and {@code bottom} slot
 * for bottom content.
 * </p>
 * <p>
 * {@code }`html <paper-toolbar class="tall"> <paper-icon-button
 * icon="menu"></paper-icon-button>
 * </p>
 * <div slot="middle" class="title">Middle Title</div> <div slot="bottom"
 * class="title">Bottom Title</div> </paper-toolbar> {@code }`
 * <p>
 * For {@code medium-tall} toolbar, the middle and bottom contents overlap and
 * are pinned to the bottom. But {@code middleJustify} and {@code bottomJustify}
 * attributes are still honored separately.
 * </p>
 * <p>
 * To make an element completely fit at the bottom of the toolbar, use
 * {@code fit} along with {@code bottom}.
 * </p>
 * <p>
 * {@code }`html <paper-toolbar class="tall">
 * </p>
 * <div id="progressBar" slot="bottom" class="fit"></div> </paper-toolbar>
 * {@code }`
 * <p>
 * When inside a {@code paper-header-panel} element with
 * {@code mode=&quot;waterfall-tall&quot;}, the class {@code .animate} is
 * toggled to animate the height change in the toolbar.
 * </p>
 * <h3>Styling</h3>
 * <p>
 * The following custom properties and mixins are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Custom property</th>
 * <th>Description</th>
 * <th>Default</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code --paper-toolbar-title}</td>
 * <td>Mixin applied to the title of the toolbar</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-toolbar-background}</td>
 * <td>Toolbar background color</td>
 * <td>{@code --primary-color}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-toolbar-color}</td>
 * <td>Toolbar foreground color</td>
 * <td>{@code --dark-theme-text-color}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-toolbar-height}</td>
 * <td>Custom height for toolbar</td>
 * <td>{@code 64px}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-toolbar-sm-height}</td>
 * <td>Custom height for small screen toolbar</td>
 * <td>{@code 56px}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-toolbar}</td>
 * <td>Mixin applied to the toolbar</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-toolbar-content}</td>
 * <td>Mixin applied to the content section of the toolbar</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-toolbar-medium}</td>
 * <td>Mixin applied to medium height toolbar</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-toolbar-tall}</td>
 * <td>Mixin applied to tall height toolbar</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-toolbar-transition}</td>
 * <td>Transition applied to the {@code .animate} class</td>
 * <td>{@code height 0.18s ease-in}</td>
 * </tr>
 * </tbody>
 * </table>
 * <h3>Accessibility</h3>
 * <p>
 * {@code <paper-toolbar>} has {@code role=&quot;toolbar&quot;} by default. Any
 * elements with the class {@code title} will be used as the label of the
 * toolbar via {@code aria-labelledby}.
 * </p>
 * <h3>Breaking change in 2.0</h3>
 * <p>
 * In Polymer 1.x, default content used to be distribuited automatically to the
 * top toolbar. In v2, the you must set {@code slot=&quot;top&quot;} on the
 * default content to distribuite the content to the top toolbar.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-toolbar#2.0.0", "Flow#1.0-SNAPSHOT" })
@Tag("paper-toolbar")
@HtmlImport("frontend://bower_components/paper-toolbar/paper-toolbar.html")
public class GeneratedPaperToolbar<R extends GeneratedPaperToolbar<R>>
        extends Component implements ComponentSupplier<R>, HasStyle {

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
    public void setBottomJustify(java.lang.String bottomJustify) {
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
    public void setJustify(java.lang.String justify) {
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
    public void setMiddleJustify(java.lang.String middleJustify) {
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
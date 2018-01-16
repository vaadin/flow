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
package com.vaadin.flow.component.splitlayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasClickListeners;
import com.vaadin.flow.component.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.dom.Element;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-split-layout>} is a Polymer element implementing a split
 * layout for two content elements with a draggable splitter between them.
 * </p>
 * <p>
 * &lt;vaadin-split-layout&gt; &lt;div&gt;First content element&lt;/div&gt;
 * &lt;div&gt;Second content element&lt;/div&gt; &lt;/vaadin-split-layout&gt;
 * </p>
 * <h3>Horizontal and Vertical Layouts</h3>
 * <p>
 * By default, the split's orientation is horizontal, meaning that the content
 * elements are positioned side by side in a flex container with a horizontal
 * layout.
 * </p>
 * <p>
 * You can change the split mode to vertical by setting the {@code orientation}
 * attribute to {@code &quot;vertical&quot;}:
 * </p>
 * <p>
 * &lt;vaadin-split-layout orientation=&quot;vertical&quot;&gt;
 * &lt;div&gt;Content on the top&lt;/div&gt; &lt;div&gt;Content on the
 * bottom&lt;/div&gt; &lt;/vaadin-split-layout&gt;
 * </p>
 * <h3>Layouts Combination</h3>
 * <p>
 * For the layout contents, we usually use {@code <div>} elements in the
 * examples, although you can use any other elements as well.
 * </p>
 * <p>
 * For instance, in order to have a nested vertical split layout inside a
 * horizontal one, you can include {@code <vaadin-split-layout>} as a content
 * element inside another split layout:
 * </p>
 * <p>
 * &lt;vaadin-split-layout&gt; &lt;div&gt;First content element&lt;/div&gt;
 * &lt;vaadin-split-layout orientation=&quot;vertical&quot;&gt;
 * &lt;div&gt;Second content element&lt;/div&gt; &lt;div&gt;Third content
 * element&lt;/div&gt; &lt;/vaadin-split-layout&gt; &lt;/vaadin-split-layout&gt;
 * </p>
 * <p>
 * You can also trigger the vertical mode in JavaScript by setting the property:
 * {@code splitLayout.orientation = &quot;vertical&quot;;}.
 * </p>
 * <h3>Split Layout Element Height</h3>
 * <p>
 * {@code <vaadin-split-layout>} element itself is a flex container. It does not
 * inherit the parent height by default, but rather sets its height depending on
 * the content.
 * </p>
 * <p>
 * You can use CSS to set the fixed height for the split layout, as usual with
 * any block element:
 * </p>
 * <p>
 * &lt;vaadin-split-layout style=&quot;height: 200px;&quot;&gt; &lt;div&gt;First
 * content element&lt;/div&gt; &lt;div&gt;Second content element&lt;/div&gt;
 * &lt;/vaadin-split-layout&gt;
 * </p>
 * <p>
 * It is possible to define percentage height as well. Note that you have to set
 * the parent height in order to make percentages work correctly. In the
 * following example, the {@code <body>} is resized to fill the entire viewport,
 * and the {@code <vaadin-split-layout>} element is set to take 100% height of
 * the {@code <body>}:
 * </p>
 * <p>
 * &lt;body style=&quot;height: 100vh; margin: 0;&quot;&gt;
 * &lt;vaadin-split-layout style=&quot;height: 100%;&quot;&gt;
 * &lt;div&gt;First&lt;/div&gt; &lt;div&gt;Second&lt;/div&gt;
 * &lt;/vaadin-split-layout&gt; &lt;/body&gt;
 * </p>
 * <p>
 * Alternatively, you can use a flexbox layout to make
 * {@code <vaadin-split-layout>} fill up the parent:
 * </p>
 * <p>
 * &lt;body style=&quot;height: 100vh; margin: 0; display: flex;&quot;&gt;
 * &lt;vaadin-split-layout style=&quot;flex: 1;&quot;&gt;
 * &lt;div&gt;First&lt;/div&gt; &lt;div&gt;Second&lt;/div&gt;
 * &lt;/vaadin-split-layout&gt; &lt;/body&gt;
 * </p>
 * <h3>Initial Splitter Position</h3>
 * <p>
 * The initial splitter position is determined from the sizes of the content
 * elements inside the split layout. Therefore, changing {@code width} on the
 * content elements affects the initial splitter position for the horizontal
 * layouts, while {@code height} affects the vertical ones.
 * </p>
 * <p>
 * Note that when the total size of the content elements does not fit the
 * layout, the content elements are scaled proportionally.
 * </p>
 * <p>
 * When setting initial sizes with relative units, such as percentages, it is
 * recommended to assign the size for both content elements:
 * </p>
 * <p>
 * &lt;vaadin-split-layout&gt; &lt;div style=&quot;width: 75%;&quot;&gt;Three
 * fourths&lt;/div&gt; &lt;div style=&quot;width: 25%;&quot;&gt;One
 * fourth&lt;/div&gt; &lt;/vaadin-split-layout&gt;
 * </p>
 * <h3>Size Limits</h3>
 * <p>
 * The {@code min-width}/{@code min-height}, and {@code max-width}/
 * {@code max-height} CSS size values for the content elements are respected and
 * used to limit the splitter position when it is dragged.
 * </p>
 * <p>
 * It is preferred to set the limits only for a single content element, in order
 * to avoid size conflicts:
 * </p>
 * <p>
 * &lt;vaadin-split-layout&gt; &lt;div style=&quot;min-width: 50px; max-width:
 * 150px;&quot;&gt;First&lt;/div&gt; &lt;div&gt;Second&lt;/div&gt;
 * &lt;/vaadin-split-layout&gt;
 * </p>
 * <h3>Resize Notification</h3>
 * <p>
 * This element implements {@code IronResizableBehavior} to notify the nested
 * resizables when the splitter is dragged. In order to define a resizable and
 * receive that notification in a nested element, include
 * {@code IronResizableBehavior} and listen for the {@code iron-resize} event.
 * </p>
 * <h3>Styling</h3>
 * <p>
 * The following shadow DOM parts are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Part name</th>
 * <th>Description</th>
 * <th>Theme for Element</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code splitter}</td>
 * <td>Split element</td>
 * <td>vaadin-split-layout</td>
 * </tr>
 * <tr>
 * <td>{@code handle}</td>
 * <td>The handle of the splitter</td>
 * <td>vaadin-split-layout</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.SplitLayoutElement#4.0.0-alpha4",
        "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-split-layout")
@HtmlImport("frontend://bower_components/vaadin-split-layout/src/vaadin-split-layout.html")
public class GeneratedVaadinSplitLayout<R extends GeneratedVaadinSplitLayout<R>>
        extends Component
        implements HasStyle, HasClickListeners<R>, ComponentSupplier<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The split layout's orientation. Possible values are:
     * {@code horizontal|vertical}.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code orientation} property from the webcomponent
     */
    public String getOrientation() {
        return getElement().getProperty("orientation");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The split layout's orientation. Possible values are:
     * {@code horizontal|vertical}.
     * </p>
     * 
     * @param orientation
     *            the String value to set
     */
    public void setOrientation(String orientation) {
        getElement().setProperty("orientation",
                orientation == null ? "" : orientation);
    }

    @DomEvent("iron-resize")
    public static class IronResizeEvent<R extends GeneratedVaadinSplitLayout<R>>
            extends ComponentEvent<R> {
        public IronResizeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code iron-resize} events fired by the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addIronResizeListener(
            ComponentEventListener<IronResizeEvent<R>> listener) {
        return addListener(IronResizeEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'primary'.
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
    public R addToPrimary(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "primary");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'secondary'.
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
    public R addToSecondary(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "secondary");
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
    public void remove(Component... components) {
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
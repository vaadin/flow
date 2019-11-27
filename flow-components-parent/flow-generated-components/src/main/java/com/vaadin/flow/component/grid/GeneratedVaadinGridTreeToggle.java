/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.component.grid;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasTheme;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.Component;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-grid-tree-toggle>} is a helper element for the
 * {@code <vaadin-grid>} that provides toggle and level display functionality
 * for the item tree.
 * </p>
 * <h4>Example:</h4>
 * <p>
 * &lt;vaadin-grid-column&gt; &lt;template class=&quot;header&quot;&gt;Package
 * name&lt;/template&gt; &lt;template&gt; &lt;vaadin-grid-tree-toggle
 * leaf=&quot;[[!item.hasChildren]]&quot; expanded=&quot;{{expanded}}&quot;
 * level=&quot;[[level]]&quot;&gt; [[item.name]]
 * &lt;/vaadin-grid-tree-toggle&gt; &lt;/template&gt;
 * &lt;/vaadin-grid-column&gt;
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
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code toggle}</td>
 * <td>The tree toggle icon</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * The following state attributes are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Attribute</th>
 * <th>Description</th>
 * <th>Part name</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code expanded}</td>
 * <td>When present, the toggle is expanded</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code leaf}</td>
 * <td>When present, the toggle is not expandable, i. e., the current item is a
 * leaf</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * The following custom CSS properties are available on the
 * {@code <vaadin-grid-tree-toggle>} element:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Custom CSS property</th>
 * <th>Description</th>
 * <th>Default</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code --vaadin-grid-tree-toggle-level-offset}</td>
 * <td>Visual offset step for each tree sublevel</td>
 * <td>{@code 1em}</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.GridTreeToggleElement#5.2.1",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-grid-tree-toggle")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid-tree-toggle.html")
public abstract class GeneratedVaadinGridTreeToggle<R extends GeneratedVaadinGridTreeToggle<R>>
        extends Component implements HasStyle, HasTheme {

    /**
     * Adds theme variants to the component.
     * 
     * @param variants
     *            theme variants to add
     */
    public void addThemeVariants(GridTreeToggleVariant... variants) {
        getThemeNames().addAll(
                Stream.of(variants).map(GridTreeToggleVariant::getVariantName)
                        .collect(Collectors.toList()));
    }

    /**
     * Removes theme variants from the component.
     * 
     * @param variants
     *            theme variants to remove
     */
    public void removeThemeVariants(GridTreeToggleVariant... variants) {
        getThemeNames().removeAll(
                Stream.of(variants).map(GridTreeToggleVariant::getVariantName)
                        .collect(Collectors.toList()));
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Current level of the tree represented with a horizontal offset of the
     * toggle button.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code level} property from the webcomponent
     */
    protected double getLevelDouble() {
        return getElement().getProperty("level", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Current level of the tree represented with a horizontal offset of the
     * toggle button.
     * </p>
     * 
     * @param level
     *            the double value to set
     */
    protected void setLevel(double level) {
        getElement().setProperty("level", level);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Hides the toggle icon and disables toggling a tree sublevel.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code leaf} property from the webcomponent
     */
    protected boolean isLeafBoolean() {
        return getElement().getProperty("leaf", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Hides the toggle icon and disables toggling a tree sublevel.
     * </p>
     * 
     * @param leaf
     *            the boolean value to set
     */
    protected void setLeaf(boolean leaf) {
        getElement().setProperty("leaf", leaf);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Sublevel toggle state.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'expanded-changed' event happens.
     * </p>
     * 
     * @return the {@code expanded} property from the webcomponent
     */
    @Synchronize(property = "expanded", value = "expanded-changed")
    protected boolean isExpandedBoolean() {
        return getElement().getProperty("expanded", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Sublevel toggle state.
     * </p>
     * 
     * @param expanded
     *            the boolean value to set
     */
    protected void setExpanded(boolean expanded) {
        getElement().setProperty("expanded", expanded);
    }

    public static class ExpandedChangeEvent<R extends GeneratedVaadinGridTreeToggle<R>>
            extends ComponentEvent<R> {
        private final boolean expanded;

        public ExpandedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.expanded = source.isExpandedBoolean();
        }

        public boolean isExpanded() {
            return expanded;
        }
    }

    /**
     * Adds a listener for {@code expanded-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addExpandedChangeListener(
            ComponentEventListener<ExpandedChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("expanded",
                        event -> listener.onComponentEvent(
                                new ExpandedChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }
}
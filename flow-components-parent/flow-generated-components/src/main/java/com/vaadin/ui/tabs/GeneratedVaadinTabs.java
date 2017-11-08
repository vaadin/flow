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
package com.vaadin.ui.tabs;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.HasComponents;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-tabs>} is a Polymer 2 element for easy switching between
 * different view
 * </p>
 * <p>
 * {@code }
 * <code>&lt;vaadin-tabs selected=&quot;4&quot;&gt; &lt;vaadin-tab&gt;Page 1&lt;/vaadin-tab&gt; &lt;vaadin-tab&gt;Page 2&lt;/vaadin-tab&gt; &lt;vaadin-tab&gt;Page 3&lt;/vaadin-tab&gt; &lt;vaadin-tab&gt;Page 4&lt;/vaadin-tab&gt; &lt;/vaadin-tabs&gt; {@code }</code>
 * </p>
 * <h3>Styling</h3>
 * <p>
 * <a href=
 * "https://cdn.vaadin.com/vaadin-valo-theme/0.3.1/demo/customization.html"
 * >Generic styling/theming documentation</a>
 * </p>
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
 * <td>{@code back-button}</td>
 * <td>Button for moving the scroll back</td>
 * </tr>
 * <tr>
 * <td>{@code tabs}</td>
 * <td>The tabs container</td>
 * </tr>
 * <tr>
 * <td>{@code forward-button}</td>
 * <td>Button for moving the scroll forward</td>
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
 * <td>{@code orientation}</td>
 * <td>Set when orientation of tabs, valid values are {@code horizontal</td><td>
 * vertical}.</td>
 * </tr>
 * <tr>
 * <td>{@code overflow}</td>
 * <td>It's set to {@code start}, {@code end}, none or both.</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.TabsElement#1.0.0-beta1", "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-tabs")
@HtmlImport("frontend://bower_components/vaadin-tabs/vaadin-tabs.html")
public class GeneratedVaadinTabs<R extends GeneratedVaadinTabs<R>> extends
        Component implements ComponentSupplier<R>, HasStyle, HasComponents {

    public void focus() {
        getElement().callFunction("focus");
    }

    /**
     * Adds the given components as children of this component.
     * 
     * @param components
     *            the components to add
     * @see HasComponents#add(Component...)
     */
    public GeneratedVaadinTabs(com.vaadin.ui.Component... components) {
        add(components);
    }

    /**
     * Default constructor.
     */
    public GeneratedVaadinTabs() {
    }
}
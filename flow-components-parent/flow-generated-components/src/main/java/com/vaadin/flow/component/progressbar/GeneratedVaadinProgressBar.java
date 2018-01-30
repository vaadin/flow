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
package com.vaadin.flow.component.progressbar;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-progress-bar>} is a Polymer 2 progress bar.
 * </p>
 * <p>
 * &lt;vaadin-progress-bar min=&quot;0&quot; max=&quot;1&quot;
 * value=&quot;0.5&quot;&gt; &lt;/vaadin-progress-bar&gt;
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
 * <td>{@code bar}</td>
 * <td>Progress-bar's background</td>
 * </tr>
 * <tr>
 * <td>{@code value}</td>
 * <td>Progress-bar's foreground</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 * <p>
 * The following custom properties are available:
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
 * <td>{@code --vaadin-progress-value}</td>
 * <td>current progress value (between 0 and 1)</td>
 * <td>0</td>
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
 * <td>{@code indeterminate}</td>
 * <td>Set to an indeterminate progress bar</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.ProgressBarElement#1.0.0-alpha12",
        "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-progress-bar")
@HtmlImport("frontend://bower_components/vaadin-progress-bar/src/vaadin-progress-bar.html")
public abstract class GeneratedVaadinProgressBar<R extends GeneratedVaadinProgressBar<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Current progress value.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code value} property from the webcomponent
     */
    protected double getValueDouble() {
        return getElement().getProperty("value", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Current progress value.
     * </p>
     * 
     * @param value
     *            the double value to set
     */
    protected void setValue(double value) {
        getElement().setProperty("value", value);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Minimum bound of the progress bar.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code min} property from the webcomponent
     */
    protected double getMinDouble() {
        return getElement().getProperty("min", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Minimum bound of the progress bar.
     * </p>
     * 
     * @param min
     *            the double value to set
     */
    protected void setMin(double min) {
        getElement().setProperty("min", min);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Maximum bound of the progress bar.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code max} property from the webcomponent
     */
    protected double getMaxDouble() {
        return getElement().getProperty("max", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Maximum bound of the progress bar.
     * </p>
     * 
     * @param max
     *            the double value to set
     */
    protected void setMax(double max) {
        getElement().setProperty("max", max);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Indeterminate state of the progress bar. This property takes precedence
     * over other state properties (min, max, value).
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code indeterminate} property from the webcomponent
     */
    protected boolean isIndeterminateBoolean() {
        return getElement().getProperty("indeterminate", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Indeterminate state of the progress bar. This property takes precedence
     * over other state properties (min, max, value).
     * </p>
     * 
     * @param indeterminate
     *            the boolean value to set
     */
    protected void setIndeterminate(boolean indeterminate) {
        getElement().setProperty("indeterminate", indeterminate);
    }
}
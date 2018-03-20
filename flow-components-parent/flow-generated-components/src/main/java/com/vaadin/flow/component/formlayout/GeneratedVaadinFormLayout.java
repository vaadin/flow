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
package com.vaadin.flow.component.formlayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import elemental.json.JsonObject;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-form-layout>} is a Polymer 2 element providing configurable
 * responsive layout for form elements.
 * </p>
 * <p>
 * &lt;vaadin-form-layout&gt;
 * </p>
 * <p>
 * &lt;vaadin-form-item&gt; &lt;label slot=&quot;label&quot;&gt;First
 * Name&lt;/label&gt; &lt;input class=&quot;full-width&quot;
 * value=&quot;Jane&quot;&gt; &lt;/vaadin-form-item&gt;
 * </p>
 * <p>
 * &lt;vaadin-form-item&gt; &lt;label slot=&quot;label&quot;&gt;Last
 * Name&lt;/label&gt; &lt;input class=&quot;full-width&quot;
 * value=&quot;Doe&quot;&gt; &lt;/vaadin-form-item&gt;
 * </p>
 * <p>
 * &lt;vaadin-form-item&gt; &lt;label
 * slot=&quot;label&quot;&gt;Email&lt;/label&gt; &lt;input
 * class=&quot;full-width&quot; value=&quot;jane.doe@example.com&quot;&gt;
 * &lt;/vaadin-form-item&gt;
 * </p>
 * <p>
 * &lt;/vaadin-form-layout&gt;
 * </p>
 * <p>
 * It supports any child elements as layout items.
 * </p>
 * <p>
 * By default, it makes a layout of two columns if the element width is equal or
 * wider than 40em, and a single column layout otherwise.
 * </p>
 * <p>
 * The number of columns and the responsive behavior are customizable with the
 * {@code responsiveSteps} property.
 * </p>
 * <h3>Spanning Items on Multiple Columns</h3>
 * <p>
 * You can use {@code colspan} attribute on the items. In the example below, the
 * first text field spans on two columns:
 * </p>
 * <p>
 * &lt;vaadin-form-layout&gt;
 * </p>
 * <p>
 * &lt;vaadin-form-item colspan=&quot;2&quot;&gt; &lt;label
 * slot=&quot;label&quot;&gt;Address&lt;/label&gt; &lt;input
 * class=&quot;full-width&quot;&gt; &lt;/vaadin-form-item&gt;
 * </p>
 * <p>
 * &lt;vaadin-form-item&gt; &lt;label slot=&quot;label&quot;&gt;First
 * Name&lt;/label&gt; &lt;input class=&quot;full-width&quot;
 * value=&quot;Jane&quot;&gt; &lt;/vaadin-form-item&gt;
 * </p>
 * <p>
 * &lt;vaadin-form-item&gt; &lt;label slot=&quot;label&quot;&gt;Last
 * Name&lt;/label&gt; &lt;input class=&quot;full-width&quot;
 * value=&quot;Doe&quot;&gt; &lt;/vaadin-form-item&gt;
 * </p>
 * <p>
 * &lt;/vaadin-form-layout&gt;
 * </p>
 * <h3>Explicit New Row</h3>
 * <p>
 * Use the {@code <br>} line break element to wrap the items on a new row:
 * </p>
 * <p>
 * &lt;vaadin-form-layout&gt;
 * </p>
 * <p>
 * &lt;vaadin-form-item&gt; &lt;label
 * slot=&quot;label&quot;&gt;Email&lt;/label&gt; &lt;input
 * class=&quot;full-width&quot;&gt; &lt;/vaadin-form-item&gt;
 * </p>
 * <p>
 * &lt;br&gt;
 * </p>
 * <p>
 * &lt;vaadin-form-item&gt; &lt;label slot=&quot;label&quot;&gt;Confirm
 * Email&lt;/label&gt; &lt;input class=&quot;full-width&quot;&gt;
 * &lt;/vaadin-form-item&gt;
 * </p>
 * <p>
 * &lt;/vaadin-form-layout&gt;
 * </p>
 * <h3>CSS Properties Reference</h3>
 * <p>
 * The following custom CSS properties are available on the
 * {@code <vaadin-form-layout>} element:
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
 * <td>{@code --vaadin-form-layout-column-spacing}</td>
 * <td>Length of the spacing between columns</td>
 * <td>{@code 2em}</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.FormLayoutElement#2.0.0-beta1",
        "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-form-layout")
@HtmlImport("frontend://bower_components/vaadin-form-layout/src/vaadin-form-layout.html")
public abstract class GeneratedVaadinFormLayout<R extends GeneratedVaadinFormLayout<R>>
        extends Component implements HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Allows specifying a responsive behavior with the number of columns and
     * the label position depending on the layout width.
     * </p>
     * <p>
     * Format: array of objects, each object defines one responsive step with
     * {@code minWidth} CSS length, {@code columns} number, and optional
     * {@code labelsPosition} string of {@code &quot;aside&quot;} or
     * {@code &quot;top&quot;}. At least one item is required.
     * </p>
     * <h4>Examples</h4>
     * <p>
     * {@code javascript formLayout.responsiveSteps = [ columns: 1}]; // The
     * layout is always a single column, labels aside.}
     * </p>
     * <p>
     * {@code javascript
    formLayout.responsiveSteps = [
     * 
     * {minWidth: 0, columns: 1}, {minWidth: '40em', columns: 2} ]; // Sets two
     * responsive steps: // 1. When the layout width is &lt; 40em, one column,
     * labels aside. // 2. Width &gt;= 40em, two columns, labels aside.}
     * </p>
     * <p>
     * {@code javascript
    formLayout.responsiveSteps = [
     * 
     * {minWidth: 0, columns: 1, labelsPosition: 'top'}, {minWidth: '20em',
     * columns: 1}, {minWidth: '40em', columns: 2} ]; // Default value. Three
     * responsive steps: // 1. Width &lt; 20em, one column, labels on top. // 2.
     * 20em &lt;= width &lt; 40em, one column, labels aside. // 3. Width &gt;=
     * 40em, two columns, labels aside.}
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code responsiveSteps} property from the webcomponent
     */
    protected JsonObject getResponsiveStepsJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("responsiveSteps");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Allows specifying a responsive behavior with the number of columns and
     * the label position depending on the layout width.
     * </p>
     * <p>
     * Format: array of objects, each object defines one responsive step with
     * {@code minWidth} CSS length, {@code columns} number, and optional
     * {@code labelsPosition} string of {@code &quot;aside&quot;} or
     * {@code &quot;top&quot;}. At least one item is required.
     * </p>
     * <h4>Examples</h4>
     * <p>
     * {@code javascript formLayout.responsiveSteps = [ columns: 1}]; // The
     * layout is always a single column, labels aside.}
     * </p>
     * <p>
     * {@code javascript
    formLayout.responsiveSteps = [
     * 
     * {minWidth: 0, columns: 1}, {minWidth: '40em', columns: 2} ]; // Sets two
     * responsive steps: // 1. When the layout width is &lt; 40em, one column,
     * labels aside. // 2. Width &gt;= 40em, two columns, labels aside.}
     * </p>
     * <p>
     * {@code javascript
    formLayout.responsiveSteps = [
     * 
     * {minWidth: 0, columns: 1, labelsPosition: 'top'}, {minWidth: '20em',
     * columns: 1}, {minWidth: '40em', columns: 2} ]; // Default value. Three
     * responsive steps: // 1. Width &lt; 20em, one column, labels on top. // 2.
     * 20em &lt;= width &lt; 40em, one column, labels aside. // 3. Width &gt;=
     * 40em, two columns, labels aside.}
     * </p>
     * 
     * @param responsiveSteps
     *            the JsonObject value to set
     */
    protected void setResponsiveSteps(JsonObject responsiveSteps) {
        getElement().setPropertyJson("responsiveSteps", responsiveSteps);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set custom CSS property values and update the layout.
     * </p>
     * 
     * @param _Args
     *            Missing documentation!
     */
    protected void updateStyles(JsonObject _Args) {
        getElement().callFunction("updateStyles", _Args);
    }
}
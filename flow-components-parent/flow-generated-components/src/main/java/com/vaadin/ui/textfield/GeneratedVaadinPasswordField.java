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
package com.vaadin.ui.textfield;

import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-password-field>} is a Polymer 2 element for password field
 * control in forms.
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-password-field label=&quot;Password&quot;&gt; &lt;/vaadin-password-field&gt; {@code }</code>
 * </p>
 * <h3>Styling</h3>
 * <p>
 * <a href=
 * "https://cdn.vaadin.com/vaadin-valo-theme/0.3.1/demo/customization.html"
 * >Generic styling/theming documentation</a>
 * </p>
 * <p>
 * See vaadin-text-field.html for the styling documentation
 * </p>
 * <p>
 * In addition to vaadin-text-field parts, here's the list of
 * vaadin-password-field specific parts
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Part name</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code reveal-button}</td>
 * <td>The eye icon which toggles the password visibility</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * In addition to vaadin-text-field state attributes, here's the list of
 * vaadin-password-field specific attributes
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
 * <td>{@code password-visible}</td>
 * <td>Set when the password is visible</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.PasswordFieldElement#1.2.0-alpha2",
        "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-password-field")
@HtmlImport("frontend://bower_components/vaadin-text-field/vaadin-password-field.html")
public class GeneratedVaadinPasswordField<R extends GeneratedVaadinPasswordField<R>>
        extends GeneratedVaadinTextField<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to hide the eye icon which toggles the password visibility.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code revealButtonHidden} property from the webcomponent
     */
    public boolean isRevealButtonHidden() {
        return getElement().getProperty("revealButtonHidden", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to hide the eye icon which toggles the password visibility.
     * </p>
     * 
     * @param revealButtonHidden
     *            the boolean value to set
     */
    public void setRevealButtonHidden(boolean revealButtonHidden) {
        getElement().setProperty("revealButtonHidden", revealButtonHidden);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the password is visible ([type=text]).
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code passwordVisible} property from the webcomponent
     */
    public boolean isPasswordVisible() {
        return getElement().getProperty("passwordVisible", false);
    }
}
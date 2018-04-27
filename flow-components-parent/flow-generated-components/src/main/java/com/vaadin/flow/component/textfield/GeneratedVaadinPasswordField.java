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
package com.vaadin.flow.component.textfield;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableBiFunction;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-password-field>} is a Polymer 2 element for password field
 * control in forms.
 * </p>
 * <p>
 * &lt;vaadin-password-field label=&quot;Password&quot;&gt;
 * &lt;/vaadin-password-field&gt;
 * </p>
 * <h3>Styling</h3>
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
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.PasswordFieldElement#2.0.1",
        "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-password-field")
@HtmlImport("frontend://bower_components/vaadin-text-field/src/vaadin-password-field.html")
public abstract class GeneratedVaadinPasswordField<R extends GeneratedVaadinPasswordField<R, T>, T>
        extends GeneratedVaadinTextField<R, T> implements HasStyle {

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
    protected boolean isRevealButtonHiddenBoolean() {
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
    protected void setRevealButtonHidden(boolean revealButtonHidden) {
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
    protected boolean isPasswordVisibleBoolean() {
        return getElement().getProperty("passwordVisible", false);
    }

    /**
     * Constructor.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param elementPropertyType
     *            the type of the element property
     * @param presentationToModel
     *            a function that converts a string value to a model value
     * @param modelToPresentation
     *            a function that converts a model value to a string value
     * @param <P>
     *            the property type
     */
    public <P> GeneratedVaadinPasswordField(T initialValue, T defaultValue,
            Class<P> elementPropertyType,
            SerializableFunction<P, T> presentationToModel,
            SerializableFunction<T, P> modelToPresentation) {
        super(initialValue, defaultValue, elementPropertyType,
                presentationToModel, modelToPresentation);
    }

    /**
     * Constructor.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param acceptNullValues
     *            whether <code>null</code> is accepted as a model value
     */
    public GeneratedVaadinPasswordField(T initialValue, T defaultValue,
            boolean acceptNullValues) {
        super(initialValue, defaultValue, acceptNullValues);
    }

    /**
     * Constructor.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param elementPropertyType
     *            the type of the element property
     * @param presentationToModel
     *            a function that accepts this component and a property value
     *            and returns a model value
     * @param modelToPresentation
     *            a function that accepts this component and a model value and
     *            returns a property value
     * @param <P>
     *            the property type
     */
    public <P> GeneratedVaadinPasswordField(T initialValue, T defaultValue,
            Class<P> elementPropertyType,
            SerializableBiFunction<R, P, T> presentationToModel,
            SerializableBiFunction<R, T, P> modelToPresentation) {
        super(initialValue, defaultValue, elementPropertyType,
                presentationToModel, modelToPresentation);
    }

    /**
     * Default constructor.
     */
    public GeneratedVaadinPasswordField() {
        this(null, null, null, (SerializableFunction) null,
                (SerializableFunction) null);
    }
}
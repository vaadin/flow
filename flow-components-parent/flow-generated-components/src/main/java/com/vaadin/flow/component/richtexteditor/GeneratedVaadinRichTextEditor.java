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
package com.vaadin.flow.component.richtexteditor;

import javax.annotation.Generated;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;

import elemental.json.JsonArray;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-rich-text-editor>} is a Web Component for rich text editing.
 * It provides a set of toolbar controls to apply formatting on the content,
 * which is stored and can be accessed as HTML5 or JSON string.
 * </p>
 * <p>
 * {@code <vaadin-rich-text-editor></vaadin-rich-text-editor>}
 * </p>
 * <p>
 * Vaadin Rich Text Editor focuses on the structure, not the styling of content.
 * Therefore, the semantic HTML5 tags such as
 * <h1>, <strong> and
 * <ul>
 * are used, and CSS usage is limited to most common cases, like horizontal text
 * alignment.
 * </p>
 * <h3>Styling</h3>
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
 * <td>{@code disabled}</td>
 * <td>Set to a disabled text editor</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code readonly}</td>
 * <td>Set to a readonly text editor</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
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
 * <td>{@code toolbar}</td>
 * <td>The toolbar wrapper</td>
 * </tr>
 * <tr>
 * <td>{@code content}</td>
 * <td>The content wrapper</td>
 * </tr>
 * <tr>
 * <td>{@code group}</td>
 * <td>The group for toolbar controls</td>
 * </tr>
 * <tr>
 * <td>{@code undo-button}</td>
 * <td>The &quot;undo&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code redo-button}</td>
 * <td>The &quot;redo&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code bold-button}</td>
 * <td>The &quot;bold&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code italic-button}</td>
 * <td>The &quot;italic&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code underline-button}</td>
 * <td>The &quot;underline&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code strike-button}</td>
 * <td>The &quot;strike-through&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code h1-button}</td>
 * <td>The &quot;header 1&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code h2-button}</td>
 * <td>The &quot;header 2&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code h3-button}</td>
 * <td>The &quot;header 3&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code subscript-button}</td>
 * <td>The &quot;subscript&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code superscript-button}</td>
 * <td>The &quot;superscript&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code list-ordered-button}</td>
 * <td>The &quot;ordered list&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code list-bullet-button}</td>
 * <td>The &quot;bullet list&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code align-left-button}</td>
 * <td>The &quot;left align&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code align-center-button}</td>
 * <td>The &quot;center align&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code align-right-button}</td>
 * <td>The &quot;right align&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code image-button}</td>
 * <td>The &quot;image&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code blockquote-button}</td>
 * <td>The &quot;blockquote&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code code-block-button}</td>
 * <td>The &quot;code block&quot; button</td>
 * </tr>
 * <tr>
 * <td>{@code clean-button}</td>
 * <td>The &quot;clean formatting&quot; button</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.RichTextEditorElement#1.0.0-alpha3",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-rich-text-editor")
@HtmlImport("frontend://bower_components/vaadin-rich-text-editor/src/vaadin-rich-text-editor.html")
public abstract class GeneratedVaadinRichTextEditor<R extends GeneratedVaadinRichTextEditor<R, T>, T>
        extends AbstractSinglePropertyField<R, T>
        implements HasStyle, HasTheme {

    /**
     * Adds theme variants to the component.
     *
     * @param variants
     *            theme variants to add
     */
    public void addThemeVariants(RichTextEditorVariant... variants) {
        getThemeNames().addAll(
                Stream.of(variants).map(RichTextEditorVariant::getVariantName)
                        .collect(Collectors.toList()));
    }

    /**
     * Removes theme variants from the component.
     *
     * @param variants
     *            theme variants to remove
     */
    public void removeThemeVariants(RichTextEditorVariant... variants) {
        getThemeNames().removeAll(
                Stream.of(variants).map(RichTextEditorVariant::getVariantName)
                        .collect(Collectors.toList()));
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * HTML representation of the rich text editor content.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'html-value-changed' event happens.
     * </p>
     *
     * @return the {@code htmlValue} property from the webcomponent
     */
    @Synchronize(property = "htmlValue", value = "html-value-changed")
    protected String getHtmlValueString() {
        return getElement().getProperty("htmlValue");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true, the user can not modify, nor copy the editor content.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return the {@code disabled} property from the webcomponent
     */
    protected boolean isDisabledBoolean() {
        return getElement().getProperty("disabled", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true, the user can not modify, nor copy the editor content.
     * </p>
     *
     * @param disabled
     *            the boolean value to set
     */
    protected void setDisabled(boolean disabled) {
        getElement().setProperty("disabled", disabled);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true, the user can not modify the editor content, but can copy it.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return the {@code readonly} property from the webcomponent
     */
    protected boolean isReadonlyBoolean() {
        return getElement().getProperty("readonly", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true, the user can not modify the editor content, but can copy it.
     * </p>
     *
     * @param readonly
     *            the boolean value to set
     */
    protected void setReadonly(boolean readonly) {
        getElement().setProperty("readonly", readonly);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An object used to localize this component. The properties are used e.g.
     * as the tooltips for the editor toolbar buttons.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return the {@code i18n} property from the webcomponent
     */
    protected JsonArray getI18nJsonArray() {
        return (JsonArray) getElement().getPropertyRaw("i18n");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An object used to localize this component. The properties are used e.g.
     * as the tooltips for the editor toolbar buttons.
     * </p>
     *
     * @param i18n
     *            the JsonArray value to set
     */
    protected void setI18n(JsonArray i18n) {
        getElement().setPropertyJson("i18n", i18n);
    }

    @DomEvent("change")
    public static class ChangeEvent<R extends GeneratedVaadinRichTextEditor<R, ?>>
            extends ComponentEvent<R> {
        public ChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code change} events fired by the webcomponent.
     *
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addChangeListener(
            ComponentEventListener<ChangeEvent<R>> listener) {
        return addListener(ChangeEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Constructs a new GeneratedVaadinRichTextEditor component with the given
     * arguments.
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
    public <P> GeneratedVaadinRichTextEditor(T initialValue, T defaultValue,
            Class<P> elementPropertyType,
            SerializableFunction<P, T> presentationToModel,
            SerializableFunction<T, P> modelToPresentation) {
        super("value", defaultValue, elementPropertyType, presentationToModel,
                modelToPresentation);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Constructs a new GeneratedVaadinRichTextEditor component with the given
     * arguments.
     *
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param acceptNullValues
     *            whether <code>null</code> is accepted as a model value
     */
    public GeneratedVaadinRichTextEditor(T initialValue, T defaultValue,
            boolean acceptNullValues) {
        super("value", defaultValue, acceptNullValues);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Constructs a new GeneratedVaadinRichTextEditor component with the given
     * arguments.
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
    public <P> GeneratedVaadinRichTextEditor(T initialValue, T defaultValue,
            Class<P> elementPropertyType,
            SerializableBiFunction<R, P, T> presentationToModel,
            SerializableBiFunction<R, T, P> modelToPresentation) {
        super("value", defaultValue, elementPropertyType, presentationToModel,
                modelToPresentation);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Default constructor.
     */
    public GeneratedVaadinRichTextEditor() {
        this(null, null, null, (SerializableFunction) null,
                (SerializableFunction) null);
    }
}

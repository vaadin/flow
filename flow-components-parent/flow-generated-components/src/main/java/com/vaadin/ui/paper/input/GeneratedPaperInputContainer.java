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
package com.vaadin.ui.paper.input;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Synchronize;
import elemental.json.JsonObject;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <paper-input-container>} is a container for a {@code <label>}, an
 * {@code <iron-input>} or {@code <textarea>} and optional add-on elements such
 * as an error message or character counter, used to implement Material Design
 * text fields.
 * </p>
 * <p>
 * For example:
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-input-container&gt;
 *   &lt;label slot=&quot;label&quot;&gt;Your name&lt;/label&gt;
 *   &lt;iron-input slot=&quot;input&quot;&gt;
 *     &lt;input&gt;
 *   &lt;/iron-input&gt;
 *   // In Polymer 1.0, you would use {@code &lt;input is=&quot;iron-input&quot; slot=&quot;input&quot;&gt;} instead of the above.
 * &lt;/paper-input-container&gt;
 * </code>
 * </pre>
 * <p>
 * You can style the nested <input> however you want; if you want it to look
 * like a Material Design input, you can style it with the
 * --paper-input-container-shared-input-style mixin.
 * </p>
 * <p>
 * Do not wrap {@code <paper-input-container>} around elements that already
 * include it, such as {@code <paper-input>}. Doing so may cause events to
 * bounce infinitely between the container and its contained element.
 * </p>
 * <h3>Listening for input changes</h3>
 * <p>
 * By default, it listens for changes on the {@code bind-value} attribute on its
 * children nodes and perform tasks such as auto-validating and label styling
 * when the {@code bind-value} changes. You can configure the attribute it
 * listens to with the {@code attr-for-value} attribute.
 * </p>
 * <h3>Using a custom input element</h3>
 * <p>
 * You can use a custom input element in a {@code <paper-input-container>}, for
 * example to implement a compound input field like a social security number
 * input. The custom input element should have the {@code paper-input-input}
 * class, have a {@code notify:true} value property and optionally implements
 * {@code Polymer.IronValidatableBehavior} if it is validatable.
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-input-container attr-for-value=&quot;ssn-value&quot;&gt;
 *   &lt;label slot=&quot;label&quot;&gt;Social security number&lt;/label&gt;
 *   &lt;ssn-input slot=&quot;input&quot; class=&quot;paper-input-input&quot;&gt;&lt;/ssn-input&gt;
 * &lt;/paper-input-container&gt;
 * </code>
 * </pre>
 * <p>
 * If you're using a {@code <paper-input-container>} imperatively, it's
 * important to make sure that you attach its children (the {@code iron-input}
 * and the optional {@code label}) before you attach the
 * {@code <paper-input-container>} itself, so that it can be set up correctly.
 * </p>
 * <h3>Validation</h3>
 * <p>
 * If the {@code auto-validate} attribute is set, the input container will
 * validate the input and update the container styling when the input value
 * changes.
 * </p>
 * <h3>Add-ons</h3>
 * <p>
 * Add-ons are child elements of a {@code <paper-input-container>} with the
 * {@code add-on} attribute and implements the
 * {@code Polymer.PaperInputAddonBehavior} behavior. They are notified when the
 * input value or validity changes, and may implement functionality such as
 * error messages or character counters. They appear at the bottom of the input.
 * </p>
 * <h3>Prefixes and suffixes</h3>
 * <p>
 * These are child elements of a {@code <paper-input-container>} with the
 * {@code prefix} or {@code suffix} attribute, and are displayed inline with the
 * input, before or after.
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-input-container&gt;
 *   &lt;div slot=&quot;prefix&quot;&gt;$&lt;/div&gt;
 *   &lt;label slot=&quot;label&quot;&gt;Total&lt;/label&gt;
 *   &lt;iron-input slot=&quot;input&quot;&gt;
 *     &lt;input&gt;
 *   &lt;/iron-input&gt;
 *   // In Polymer 1.0, you would use {@code &lt;input is=&quot;iron-input&quot; slot=&quot;input&quot;&gt;} instead of the above.
 *   &lt;paper-icon-button slot=&quot;suffix&quot; icon=&quot;clear&quot;&gt;&lt;/paper-icon-button&gt;
 * &lt;/paper-input-container&gt;
 * </code>
 * </pre>
 * 
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
 * <td>{@code --paper-input-container-color}</td>
 * <td>Label and underline color when the input is not focused</td>
 * <td>{@code --secondary-text-color}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-focus-color}</td>
 * <td>Label and underline color when the input is focused</td>
 * <td>{@code --primary-color}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-invalid-color}</td>
 * <td>Label and underline color when the input is is invalid</td>
 * <td>{@code --error-color}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-input-color}</td>
 * <td>Input foreground color</td>
 * <td>{@code --primary-text-color}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container}</td>
 * <td>Mixin applied to the container</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-disabled}</td>
 * <td>Mixin applied to the container when it's disabled</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-label}</td>
 * <td>Mixin applied to the label</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-label-focus}</td>
 * <td>Mixin applied to the label when the input is focused</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-label-floating}</td>
 * <td>Mixin applied to the label when floating</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-input}</td>
 * <td>Mixin applied to the input</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-input-focus}</td>
 * <td>Mixin applied to the input when focused</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-input-invalid}</td>
 * <td>Mixin applied to the input when invalid</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-input-webkit-spinner}</td>
 * <td>Mixin applied to the webkit spinner</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-input-webkit-clear}</td>
 * <td>Mixin applied to the webkit clear button</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-ms-clear}</td>
 * <td>Mixin applied to the Internet Explorer clear button</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-underline}</td>
 * <td>Mixin applied to the underline</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-underline-focus}</td>
 * <td>Mixin applied to the underline when the input is focused</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-container-underline-disabled}</td>
 * <td>Mixin applied to the underline when the input is disabled</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-prefix}</td>
 * <td>Mixin applied to the input prefix</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-input-suffix}</td>
 * <td>Mixin applied to the input suffix</td>
 * <td>{@code</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * This element is {@code display:block} by default, but you can set the
 * {@code inline} attribute to make it {@code display:inline-block}.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-input-container#2.0.2", "Flow#1.0-SNAPSHOT" })
@Tag("paper-input-container")
@HtmlImport("frontend://bower_components/paper-input/paper-input-container.html")
public class GeneratedPaperInputContainer<R extends GeneratedPaperInputContainer<R>>
        extends Component implements ComponentSupplier<R>, HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable the floating label. The label disappears when the
     * input value is not null.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noLabelFloat} property from the webcomponent
     */
    public boolean isNoLabelFloat() {
        return getElement().getProperty("noLabelFloat", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable the floating label. The label disappears when the
     * input value is not null.
     * </p>
     * 
     * @param noLabelFloat
     *            the boolean value to set
     */
    public void setNoLabelFloat(boolean noLabelFloat) {
        getElement().setProperty("noLabelFloat", noLabelFloat);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to always float the floating label.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code alwaysFloatLabel} property from the webcomponent
     */
    public boolean isAlwaysFloatLabel() {
        return getElement().getProperty("alwaysFloatLabel", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to always float the floating label.
     * </p>
     * 
     * @param alwaysFloatLabel
     *            the boolean value to set
     */
    public void setAlwaysFloatLabel(boolean alwaysFloatLabel) {
        getElement().setProperty("alwaysFloatLabel", alwaysFloatLabel);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The attribute to listen for value changes on.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code attrForValue} property from the webcomponent
     */
    public String getAttrForValue() {
        return getElement().getProperty("attrForValue");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The attribute to listen for value changes on.
     * </p>
     * 
     * @param attrForValue
     *            the String value to set
     */
    public void setAttrForValue(java.lang.String attrForValue) {
        getElement().setProperty("attrForValue",
                attrForValue == null ? "" : attrForValue);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to auto-validate the input value when it changes.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autoValidate} property from the webcomponent
     */
    public boolean isAutoValidate() {
        return getElement().getProperty("autoValidate", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to auto-validate the input value when it changes.
     * </p>
     * 
     * @param autoValidate
     *            the boolean value to set
     */
    public void setAutoValidate(boolean autoValidate) {
        getElement().setProperty("autoValidate", autoValidate);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the input is invalid. This property is set automatically when the
     * input value changes if auto-validating, or when the
     * {@code iron-input-validate} event is heard from a child.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code invalid} property from the webcomponent
     */
    public boolean isInvalid() {
        return getElement().getProperty("invalid", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the input is invalid. This property is set automatically when the
     * input value changes if auto-validating, or when the
     * {@code iron-input-validate} event is heard from a child.
     * </p>
     * 
     * @param invalid
     *            the boolean value to set
     */
    public void setInvalid(boolean invalid) {
        getElement().setProperty("invalid", invalid);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the input has focus.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'focused-changed' event happens.
     * </p>
     * 
     * @return the {@code focused} property from the webcomponent
     */
    @Synchronize(property = "focused", value = "focused-changed")
    public boolean isFocused() {
        return getElement().getProperty("focused", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Call this to update the state of add-ons.
     * </p>
     * 
     * @param state
     *            Add-on state.
     */
    protected void updateAddons(JsonObject state) {
        getElement().callFunction("updateAddons", state);
    }

    @DomEvent("focused-changed")
    public static class FocusedChangeEvent<R extends GeneratedPaperInputContainer<R>>
            extends ComponentEvent<R> {
        public FocusedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addFocusedChangeListener(
            ComponentEventListener<FocusedChangeEvent<R>> listener) {
        return addListener(FocusedChangeEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'prefix'.
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
    public R addToPrefix(com.vaadin.ui.Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "prefix");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'label'.
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
    public R addToLabel(com.vaadin.ui.Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "label");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'input'.
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
    public R addToInput(com.vaadin.ui.Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "input");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'suffix'.
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
    public R addToSuffix(com.vaadin.ui.Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "suffix");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'add-on'.
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
    public R addToAddOn(com.vaadin.ui.Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "add-on");
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
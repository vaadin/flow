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
package com.vaadin.flow.component.datepicker;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Synchronize;
import elemental.json.JsonObject;
import com.vaadin.flow.component.NotSupported;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.component.AbstractSinglePropertyField;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-date-picker-light>} is a customizable version of the
 * {@code <vaadin-date-picker>} providing only the scrollable month calendar
 * view and leaving the input field definition to the user.
 * </p>
 * <p>
 * To create a custom input field, you need to add a child element which has a
 * two-way data-bindable property representing the input value. The property
 * name is expected to be {@code bindValue} by default. See the example below
 * for a simplest possible example using an {@code <input>} element extended
 * with {@code iron-input}.
 * </p>
 * <p>
 * &lt;vaadin-date-picker-light&gt; &lt;iron-input&gt; &lt;input/&gt;
 * &lt;/iron-input&gt; &lt;/vaadin-date-picker-light&gt;
 * </p>
 * <p>
 * If you are using other custom input fields like {@code <paper-input>}, you
 * need to define the name of value property using the {@code attrForValue}
 * property.
 * </p>
 * <p>
 * &lt;vaadin-date-picker-light attr-for-value=&quot;value&quot;&gt;
 * &lt;paper-input label=&quot;Birthday&quot;&gt; &lt;/paper-input&gt;
 * &lt;/vaadin-date-picker-light&gt;
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
 * <td>{@code overlay-content}</td>
 * <td>The overlay element</td>
 * <td>vaadin-date-picker-light</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 * <p>
 * In addition to {@code <vaadin-date-picker-light>} itself, the following
 * internal components are themable:
 * </p>
 * <ul>
 * <li>{@code <vaadin-date-picker-overlay>}</li>
 * <li>{@code <vaadin-date-picker-overlay-content>}</li>
 * <li>{@code <vaadin-month-calendar>}</li>
 * </ul>
 * <p>
 * Note: the {@code theme} attribute value set on
 * {@code <vaadin-date-picker-light>} is propagated to the internal themable
 * components listed above.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.DatePickerLightElement#3.3.0",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-date-picker-light")
@HtmlImport("frontend://bower_components/vaadin-date-picker/src/vaadin-date-picker-light.html")
public abstract class GeneratedVaadinDatePickerLight<R extends GeneratedVaadinDatePickerLight<R, T>, T>
        extends AbstractSinglePropertyField<R, T> implements HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to mark the input as required.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code required} property from the webcomponent
     */
    protected boolean isRequiredBoolean() {
        return getElement().getProperty("required", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to mark the input as required.
     * </p>
     * 
     * @param required
     *            the boolean value to set
     */
    protected void setRequired(boolean required) {
        getElement().setProperty("required", required);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The name of this element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code name} property from the webcomponent
     */
    protected String getNameString() {
        return getElement().getProperty("name");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The name of this element.
     * </p>
     * 
     * @param name
     *            the String value to set
     */
    protected void setName(String name) {
        getElement().setProperty("name", name == null ? "" : name);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Date which should be visible when there is no value selected.
     * </p>
     * <p>
     * The same date formats as for the {@code value} property are supported.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code initialPosition} property from the webcomponent
     */
    protected String getInitialPositionString() {
        return getElement().getProperty("initialPosition");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Date which should be visible when there is no value selected.
     * </p>
     * <p>
     * The same date formats as for the {@code value} property are supported.
     * </p>
     * 
     * @param initialPosition
     *            the String value to set
     */
    protected void setInitialPosition(String initialPosition) {
        getElement().setProperty("initialPosition",
                initialPosition == null ? "" : initialPosition);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The label for this element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code label} property from the webcomponent
     */
    protected String getLabelString() {
        return getElement().getProperty("label");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The label for this element.
     * </p>
     * 
     * @param label
     *            the String value to set
     */
    protected void setLabel(String label) {
        getElement().setProperty("label", label == null ? "" : label);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set true to open the date selector overlay.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'opened-changed' event happens.
     * </p>
     * 
     * @return the {@code opened} property from the webcomponent
     */
    @Synchronize(property = "opened", value = "opened-changed")
    protected boolean isOpenedBoolean() {
        return getElement().getProperty("opened", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set true to open the date selector overlay.
     * </p>
     * 
     * @param opened
     *            the boolean value to set
     */
    protected void setOpened(boolean opened) {
        getElement().setProperty("opened", opened);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set true to display ISO-8601 week numbers in the calendar. Notice that
     * displaying week numbers is only supported when
     * {@code i18n.firstDayOfWeek} is 1 (Monday).
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code showWeekNumbers} property from the webcomponent
     */
    protected boolean isShowWeekNumbersBoolean() {
        return getElement().getProperty("showWeekNumbers", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set true to display ISO-8601 week numbers in the calendar. Notice that
     * displaying week numbers is only supported when
     * {@code i18n.firstDayOfWeek} is 1 (Monday).
     * </p>
     * 
     * @param showWeekNumbers
     *            the boolean value to set
     */
    protected void setShowWeekNumbers(boolean showWeekNumbers) {
        getElement().setProperty("showWeekNumbers", showWeekNumbers);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The object used to localize this component. To change the default
     * localization, replace the entire <em>i18n</em> object or just the
     * property you want to modify.
     * </p>
     * <p>
     * The object has the following JSON structure and default values:
     * </p>
     * 
     * <pre>
     * <code>        {
     * 	          // An array with the full names of months starting
     * 	          // with January.
     * 	          monthNames: [
     * 	            'January', 'February', 'March', 'April', 'May',
     * 	            'June', 'July', 'August', 'September',
     * 	            'October', 'November', 'December'
     * 	          ],
     * 
     * 	          // An array of weekday names starting with Sunday. Used
     * 	          // in screen reader announcements.
     * 	          weekdays: [
     * 	            'Sunday', 'Monday', 'Tuesday', 'Wednesday',
     * 	            'Thursday', 'Friday', 'Saturday'
     * 	          ],
     * 
     * 	          // An array of short weekday names starting with Sunday.
     * 	          // Displayed in the calendar.
     * 	          weekdaysShort: [
     * 	            'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'
     * 	          ],
     * 
     * 	          // An integer indicating the first day of the week
     * 	          // (0 = Sunday, 1 = Monday, etc.).
     * 	          firstDayOfWeek: 0,
     * 
     * 	          // Used in screen reader announcements along with week
     * 	          // numbers, if they are displayed.
     * 	          week: 'Week',
     * 
     * 	          // Translation of the Calendar icon button title.
     * 	          calendar: 'Calendar',
     * 
     * 	          // Translation of the Clear icon button title.
     * 	          clear: 'Clear',
     * 
     * 	          // Translation of the Today shortcut button text.
     * 	          today: 'Today',
     * 
     * 	          // Translation of the Cancel button text.
     * 	          cancel: 'Cancel',
     * 
     * 	          // A function to format given {@code Object} as
     * 	          // date string. Object is in the format {@code { day: ..., month: ..., year: ... }}
     * 	          formatDate: d =&gt; {
     * 	            // returns a string representation of the given
     * 	            // object in 'MM/DD/YYYY' -format
     * 	          },
     * 
     * 	          // A function to parse the given text to an {@code Object} in the format {@code { day: ..., month: ..., year: ... }}.
     * 	          // Must properly parse (at least) text
     * 	          // formatted by {@code formatDate}.
     * 	          // Setting the property to null will disable
     * 	          // keyboard input feature.
     * 	          parseDate: text =&gt; {
     * 	            // Parses a string in 'MM/DD/YY', 'MM/DD' or 'DD' -format to
     * 	            // an {@code Object} in the format {@code { day: ..., month: ..., year: ... }}.
     * 	          }
     * 
     * 	          // A function to format given {@code monthName} and
     * 	          // {@code fullYear} integer as calendar title string.
     * 	          formatTitle: (monthName, fullYear) =&gt; {
     * 	            return monthName + ' ' + fullYear;
     * 	          }
     * 	        }&lt;p&gt;This property is not synchronized automatically from the client side, so the returned value may not be the same as in client side.
     * 	</code>
     * </pre>
     * 
     * @return the {@code i18n} property from the webcomponent
     */
    protected JsonObject getI18nJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("i18n");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The object used to localize this component. To change the default
     * localization, replace the entire <em>i18n</em> object or just the
     * property you want to modify.
     * </p>
     * <p>
     * The object has the following JSON structure and default values:
     * </p>
     * 
     * <pre>
     * <code>        {
     * 	          // An array with the full names of months starting
     * 	          // with January.
     * 	          monthNames: [
     * 	            'January', 'February', 'March', 'April', 'May',
     * 	            'June', 'July', 'August', 'September',
     * 	            'October', 'November', 'December'
     * 	          ],
     * 
     * 	          // An array of weekday names starting with Sunday. Used
     * 	          // in screen reader announcements.
     * 	          weekdays: [
     * 	            'Sunday', 'Monday', 'Tuesday', 'Wednesday',
     * 	            'Thursday', 'Friday', 'Saturday'
     * 	          ],
     * 
     * 	          // An array of short weekday names starting with Sunday.
     * 	          // Displayed in the calendar.
     * 	          weekdaysShort: [
     * 	            'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'
     * 	          ],
     * 
     * 	          // An integer indicating the first day of the week
     * 	          // (0 = Sunday, 1 = Monday, etc.).
     * 	          firstDayOfWeek: 0,
     * 
     * 	          // Used in screen reader announcements along with week
     * 	          // numbers, if they are displayed.
     * 	          week: 'Week',
     * 
     * 	          // Translation of the Calendar icon button title.
     * 	          calendar: 'Calendar',
     * 
     * 	          // Translation of the Clear icon button title.
     * 	          clear: 'Clear',
     * 
     * 	          // Translation of the Today shortcut button text.
     * 	          today: 'Today',
     * 
     * 	          // Translation of the Cancel button text.
     * 	          cancel: 'Cancel',
     * 
     * 	          // A function to format given {@code Object} as
     * 	          // date string. Object is in the format {@code { day: ..., month: ..., year: ... }}
     * 	          formatDate: d =&gt; {
     * 	            // returns a string representation of the given
     * 	            // object in 'MM/DD/YYYY' -format
     * 	          },
     * 
     * 	          // A function to parse the given text to an {@code Object} in the format {@code { day: ..., month: ..., year: ... }}.
     * 	          // Must properly parse (at least) text
     * 	          // formatted by {@code formatDate}.
     * 	          // Setting the property to null will disable
     * 	          // keyboard input feature.
     * 	          parseDate: text =&gt; {
     * 	            // Parses a string in 'MM/DD/YY', 'MM/DD' or 'DD' -format to
     * 	            // an {@code Object} in the format {@code { day: ..., month: ..., year: ... }}.
     * 	          }
     * 
     * 	          // A function to format given {@code monthName} and
     * 	          // {@code fullYear} integer as calendar title string.
     * 	          formatTitle: (monthName, fullYear) =&gt; {
     * 	            return monthName + ' ' + fullYear;
     * 	          }
     * 	        }
     * 	</code>
     * </pre>
     * 
     * @param i18n
     *            the JsonObject value to set
     */
    protected void setI18n(JsonObject i18n) {
        getElement().setPropertyJson("i18n", i18n);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The earliest date that can be selected. All earlier dates will be
     * disabled.
     * </p>
     * <p>
     * Supported date formats:
     * </p>
     * <ul>
     * <li>ISO 8601 {@code &quot;YYYY-MM-DD&quot;} (default)</li>
     * <li>6-digit extended ISO 8601 {@code &quot;+YYYYYY-MM-DD&quot;},
     * {@code &quot;-YYYYYY-MM-DD&quot;}
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.</li>
     * </ul>
     * 
     * @return the {@code min} property from the webcomponent
     */
    protected String getMinString() {
        return getElement().getProperty("min");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The earliest date that can be selected. All earlier dates will be
     * disabled.
     * </p>
     * <p>
     * Supported date formats:
     * </p>
     * <ul>
     * <li>ISO 8601 {@code &quot;YYYY-MM-DD&quot;} (default)</li>
     * <li>6-digit extended ISO 8601 {@code &quot;+YYYYYY-MM-DD&quot;},
     * {@code &quot;-YYYYYY-MM-DD&quot;}</li>
     * </ul>
     * 
     * @param min
     *            the String value to set
     */
    protected void setMin(String min) {
        getElement().setProperty("min", min == null ? "" : min);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The latest date that can be selected. All later dates will be disabled.
     * </p>
     * <p>
     * Supported date formats:
     * </p>
     * <ul>
     * <li>ISO 8601 {@code &quot;YYYY-MM-DD&quot;} (default)</li>
     * <li>6-digit extended ISO 8601 {@code &quot;+YYYYYY-MM-DD&quot;},
     * {@code &quot;-YYYYYY-MM-DD&quot;}
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.</li>
     * </ul>
     * 
     * @return the {@code max} property from the webcomponent
     */
    protected String getMaxString() {
        return getElement().getProperty("max");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The latest date that can be selected. All later dates will be disabled.
     * </p>
     * <p>
     * Supported date formats:
     * </p>
     * <ul>
     * <li>ISO 8601 {@code &quot;YYYY-MM-DD&quot;} (default)</li>
     * <li>6-digit extended ISO 8601 {@code &quot;+YYYYYY-MM-DD&quot;},
     * {@code &quot;-YYYYYY-MM-DD&quot;}</li>
     * </ul>
     * 
     * @param max
     *            the String value to set
     */
    protected void setMax(String max) {
        getElement().setProperty("max", max == null ? "" : max);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Name of the two-way data-bindable property representing the value of the
     * custom input field.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code attrForValue} property from the webcomponent
     */
    protected String getAttrForValueString() {
        return getElement().getProperty("attrForValue");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Name of the two-way data-bindable property representing the value of the
     * custom input field.
     * </p>
     * 
     * @param attrForValue
     *            the String value to set
     */
    protected void setAttrForValue(String attrForValue) {
        getElement().setProperty("attrForValue",
                attrForValue == null ? "" : attrForValue);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Opens the dropdown.
     * </p>
     */
    protected void open() {
        getElement().callFunction("open");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Closes the dropdown.
     * </p>
     */
    protected void close() {
        getElement().callFunction("close");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if {@code value} is valid, and sets the {@code invalid} flag
     * appropriately.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param value
     *            Value to validate. Optional, defaults to user's input value.
     */
    @NotSupported
    protected void validate(String value) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if the current input value satisfies all constraints (if
     * any)
     * </p>
     * <p>
     * Override the {@code checkValidity} method for custom validations.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param value
     *            Value to validate. Optional, defaults to the selected date.
     */
    @NotSupported
    protected void checkValidity(String value) {
    }

    @DomEvent("change")
    public static class ChangeEvent<R extends GeneratedVaadinDatePickerLight<R, ?>>
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

    public static class OpenedChangeEvent<R extends GeneratedVaadinDatePickerLight<R, ?>>
            extends ComponentEvent<R> {
        private final boolean opened;

        public OpenedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.opened = source.isOpenedBoolean();
        }

        public boolean isOpened() {
            return opened;
        }
    }

    /**
     * Adds a listener for {@code opened-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addOpenedChangeListener(
            ComponentEventListener<OpenedChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("opened",
                        event -> listener.onComponentEvent(
                                new OpenedChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    /**
     * Constructs a new GeneratedVaadinDatePickerLight component with the given
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
    public <P> GeneratedVaadinDatePickerLight(T initialValue, T defaultValue,
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
     * Constructs a new GeneratedVaadinDatePickerLight component with the given
     * arguments.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param acceptNullValues
     *            whether <code>null</code> is accepted as a model value
     */
    public GeneratedVaadinDatePickerLight(T initialValue, T defaultValue,
            boolean acceptNullValues) {
        super("value", defaultValue, acceptNullValues);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Constructs a new GeneratedVaadinDatePickerLight component with the given
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
    public <P> GeneratedVaadinDatePickerLight(T initialValue, T defaultValue,
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
    public GeneratedVaadinDatePickerLight() {
        this(null, null, null, (SerializableFunction) null,
                (SerializableFunction) null);
    }
}
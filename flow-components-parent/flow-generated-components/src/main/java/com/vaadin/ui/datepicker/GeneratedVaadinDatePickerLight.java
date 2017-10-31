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
package com.vaadin.ui.datepicker;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.ui.common.NotSupported;
import com.vaadin.ui.common.HasComponents;

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
 * {@code }
 * <code>html &lt;vaadin-date-picker-light&gt; &lt;iron-input&gt; &lt;input/&gt; &lt;/iron-input&gt; &lt;/vaadin-date-picker-light&gt; {@code }</code>
 * </p>
 * <p>
 * If you are using other custom input fields like {@code <paper-input>}, you
 * need to define the name of value property using the {@code attrForValue}
 * property.
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-date-picker-light attr-for-value=&quot;value&quot;&gt; &lt;paper-input label=&quot;Birthday&quot;&gt; &lt;/paper-input&gt; &lt;/vaadin-date-picker-light&gt; {@code }</code>
 * </p>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: Vaadin.DatePickerLightElement#2.0.5",
		"Flow#1.0-SNAPSHOT"})
@Tag("vaadin-date-picker-light")
@HtmlImport("frontend://bower_components/vaadin-date-picker/vaadin-date-picker-light.html")
public class GeneratedVaadinDatePickerLight<R extends GeneratedVaadinDatePickerLight<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle, HasComponents {

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The value for this element.
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
	 * @return the {@code value} property from the webcomponent
	 */
	public String getValue() {
		return getElement().getProperty("value");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The value for this element.
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
	 * @param value
	 *            the String value to set
	 */
	public void setValue(java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
	}

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
	public boolean isRequired() {
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
	public void setRequired(boolean required) {
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
	public String getName() {
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
	public void setName(java.lang.String name) {
		getElement().setProperty("name", name == null ? "" : name);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Indicates whether this date picker has a value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code hasValue} property from the webcomponent
	 */
	public boolean hasValue() {
		return getElement().getProperty("hasValue", false);
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
	public String getInitialPosition() {
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
	public void setInitialPosition(java.lang.String initialPosition) {
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
	public String getLabel() {
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
	public void setLabel(java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set true to open the date selector overlay.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code opened} property from the webcomponent
	 */
	public boolean isOpened() {
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
	public void setOpened(boolean opened) {
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
	public boolean isShowWeekNumbers() {
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
	public void setShowWeekNumbers(boolean showWeekNumbers) {
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
	 * 	          // A function to format given {@code Date} object as
	 * 	          // date string.
	 * 	          formatDate: d =&gt; {
	 * 	            // returns a string representation of the given
	 * 	            // Date object in 'MM/DD/YYYY' -format
	 * 	          },
	 * 
	 * 	          // A function to parse the given text to a {@code Date}
	 * 	          // object. Must properly parse (at least) text
	 * 	          // formatted by {@code formatDate}.
	 * 	          // Setting the property to null will disable
	 * 	          // keyboard input feature.
	 * 	          parseDate: text =&gt; {
	 * 	            // Parses a string in 'MM/DD/YY', 'MM/DD' or 'DD' -format to
	 * 	            // a Date object
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
	protected JsonObject protectedGetI18n() {
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
	 * 	          // A function to format given {@code Date} object as
	 * 	          // date string.
	 * 	          formatDate: d =&gt; {
	 * 	            // returns a string representation of the given
	 * 	            // Date object in 'MM/DD/YYYY' -format
	 * 	          },
	 * 
	 * 	          // A function to parse the given text to a {@code Date}
	 * 	          // object. Must properly parse (at least) text
	 * 	          // formatted by {@code formatDate}.
	 * 	          // Setting the property to null will disable
	 * 	          // keyboard input feature.
	 * 	          parseDate: text =&gt; {
	 * 	            // Parses a string in 'MM/DD/YY', 'MM/DD' or 'DD' -format to
	 * 	            // a Date object
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
	protected void setI18n(elemental.json.JsonObject i18n) {
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
	public String getMin() {
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
	public void setMin(java.lang.String min) {
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
	public String getMax() {
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
	public void setMax(java.lang.String max) {
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
	public String getAttrForValue() {
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
	public void setAttrForValue(java.lang.String attrForValue) {
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
	public void open() {
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
	public void close() {
		getElement().callFunction("close");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Returns true if {@code value} is valid, and sets the {@code invalid} flag
	 * appropriatelly.
	 * </p>
	 * <p>
	 * This function is not supported by Flow because it returns a
	 * <code>boolean</code>. Functions with return types different than void are
	 * not supported at this moment.
	 * 
	 * @param value
	 *            Missing documentation!
	 */
	@NotSupported
	protected void validate(elemental.json.JsonObject value) {
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
	 * 
	 * @param value
	 *            Missing documentation!
	 */
	protected void checkValidity(elemental.json.JsonObject value) {
		getElement().callFunction("checkValidity", value);
	}

	/**
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public GeneratedVaadinDatePickerLight(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public GeneratedVaadinDatePickerLight() {
	}
}
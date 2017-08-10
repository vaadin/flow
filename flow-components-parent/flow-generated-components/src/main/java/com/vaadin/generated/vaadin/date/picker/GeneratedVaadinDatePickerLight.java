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
package com.vaadin.generated.vaadin.date.picker;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.components.NotSupported;
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <vaadin-date-picker-light>} is a customizable version of the
 * {@code <vaadin-date-picker>} providing only the scrollable month calendar
 * view and leaving the input field definition to the user.
 * 
 * To create a custom input field, you need to add a child element which has a
 * two-way data-bindable property representing the input value. The property
 * name is expected to be {@code bindValue} by default. See the example below
 * for a simplest possible example using an {@code <input>} element extended
 * with {@code iron-input}.
 * 
 * {@code }`html <vaadin-date-picker-light> <iron-input> <input/> </iron-input>
 * </vaadin-date-picker-light> {@code }`
 * 
 * If you are using other custom input fields like {@code <paper-input>}, you
 * need to define the name of value property using the {@code attrForValue}
 * property.
 * 
 * {@code }`html <vaadin-date-picker-light attr-for-value="value"> <paper-input
 * label="Birthday"> </paper-input> </vaadin-date-picker-light> {@code }`
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#0.1-SNAPSHOT",
		"WebComponent: Vaadin.DatePickerLightElement#2.0.1",
		"Flow#0.1-SNAPSHOT"})
@Tag("vaadin-date-picker-light")
@HtmlImport("frontend://bower_components/vaadin-date-picker/vaadin-date-picker-light.html")
public class GeneratedVaadinDatePickerLight<R extends GeneratedVaadinDatePickerLight<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle, HasComponents {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The value for this element.
	 * 
	 * Supported date formats: - ISO 8601 {@code "YYYY-MM-DD"} (default) -
	 * 6-digit extended ISO 8601 {@code "+YYYYYY-MM-DD"},
	 * {@code "-YYYYYY-MM-DD"}
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getValue() {
		return getElement().getProperty("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The value for this element.
	 * 
	 * Supported date formats: - ISO 8601 {@code "YYYY-MM-DD"} (default) -
	 * 6-digit extended ISO 8601 {@code "+YYYYYY-MM-DD"},
	 * {@code "-YYYYYY-MM-DD"}
	 * 
	 * @param value
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setValue(java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to mark the input as required.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isRequired() {
		return getElement().getProperty("required", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to mark the input as required.
	 * 
	 * @param required
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setRequired(boolean required) {
		getElement().setProperty("required", required);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of this element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getName() {
		return getElement().getProperty("name");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of this element.
	 * 
	 * @param name
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setName(java.lang.String name) {
		getElement().setProperty("name", name == null ? "" : name);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Indicates whether this date picker has a value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean hasValue() {
		return getElement().getProperty("hasValue", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Date which should be visible when there is no value selected.
	 * 
	 * The same date formats as for the {@code value} property are supported.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getInitialPosition() {
		return getElement().getProperty("initialPosition");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Date which should be visible when there is no value selected.
	 * 
	 * The same date formats as for the {@code value} property are supported.
	 * 
	 * @param initialPosition
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setInitialPosition(java.lang.String initialPosition) {
		getElement().setProperty("initialPosition",
				initialPosition == null ? "" : initialPosition);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The label for this element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getLabel() {
		return getElement().getProperty("label");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The label for this element.
	 * 
	 * @param label
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setLabel(java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set true to open the date selector overlay.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isOpened() {
		return getElement().getProperty("opened", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set true to open the date selector overlay.
	 * 
	 * @param opened
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setOpened(boolean opened) {
		getElement().setProperty("opened", opened);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set true to display ISO-8601 week numbers in the calendar. Notice that
	 * displaying week numbers is only supported when
	 * {@code i18n.firstDayOfWeek} is 1 (Monday).
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isShowWeekNumbers() {
		return getElement().getProperty("showWeekNumbers", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set true to display ISO-8601 week numbers in the calendar. Notice that
	 * displaying week numbers is only supported when
	 * {@code i18n.firstDayOfWeek} is 1 (Monday).
	 * 
	 * @param showWeekNumbers
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setShowWeekNumbers(boolean showWeekNumbers) {
		getElement().setProperty("showWeekNumbers", showWeekNumbers);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The object used to localize this component. To change the default
	 * localization, replace the entire _i18n_ object or just the property you
	 * want to modify.
	 * 
	 * The object has the following JSON structure and default values:
	 * 
	 * { // An array with the full names of months starting // with January.
	 * monthNames: [ 'January', 'February', 'March', 'April', 'May', 'June',
	 * 'July', 'August', 'September', 'October', 'November', 'December' ],
	 * 
	 * // An array of weekday names starting with Sunday. Used // in screen
	 * reader announcements. weekdays: [ 'Sunday', 'Monday', 'Tuesday',
	 * 'Wednesday', 'Thursday', 'Friday', 'Saturday' ],
	 * 
	 * // An array of short weekday names starting with Sunday. // Displayed in
	 * the calendar. weekdaysShort: [ 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri',
	 * 'Sat' ],
	 * 
	 * // An integer indicating the first day of the week // (0 = Sunday, 1 =
	 * Monday, etc.). firstDayOfWeek: 0,
	 * 
	 * // Used in screen reader announcements along with week // numbers, if
	 * they are displayed. week: 'Week',
	 * 
	 * // Translation of the Calendar icon button title. calendar: 'Calendar',
	 * 
	 * // Translation of the Clear icon button title. clear: 'Clear',
	 * 
	 * // Translation of the Today shortcut button text. today: 'Today',
	 * 
	 * // Translation of the Cancel button text. cancel: 'Cancel',
	 * 
	 * // A function to format given {@code Date} object as // date string.
	 * formatDate: d => { // returns a string representation of the given //
	 * Date object in 'MM/DD/YYYY' -format },
	 * 
	 * // A function to parse the given text to a {@code Date}
	 * // object. Must properly parse (at least) text // formatted by
	 * {@code formatDate}. // Setting the property to null will disable //
	 * keyboard input feature. parseDate: text => { // Parses a string in
	 * 'MM/DD/YY', 'MM/DD' or 'DD' -format to // a Date object }
	 * 
	 * // A function to format given {@code monthName} and // {@code fullYear}
	 * integer as calendar title string. formatTitle: (monthName, fullYear) => {
	 * return monthName + ' ' + fullYear; } }
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonObject protectedGetI18n() {
		return (JsonObject) getElement().getPropertyRaw("i18n");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The object used to localize this component. To change the default
	 * localization, replace the entire _i18n_ object or just the property you
	 * want to modify.
	 * 
	 * The object has the following JSON structure and default values:
	 * 
	 * { // An array with the full names of months starting // with January.
	 * monthNames: [ 'January', 'February', 'March', 'April', 'May', 'June',
	 * 'July', 'August', 'September', 'October', 'November', 'December' ],
	 * 
	 * // An array of weekday names starting with Sunday. Used // in screen
	 * reader announcements. weekdays: [ 'Sunday', 'Monday', 'Tuesday',
	 * 'Wednesday', 'Thursday', 'Friday', 'Saturday' ],
	 * 
	 * // An array of short weekday names starting with Sunday. // Displayed in
	 * the calendar. weekdaysShort: [ 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri',
	 * 'Sat' ],
	 * 
	 * // An integer indicating the first day of the week // (0 = Sunday, 1 =
	 * Monday, etc.). firstDayOfWeek: 0,
	 * 
	 * // Used in screen reader announcements along with week // numbers, if
	 * they are displayed. week: 'Week',
	 * 
	 * // Translation of the Calendar icon button title. calendar: 'Calendar',
	 * 
	 * // Translation of the Clear icon button title. clear: 'Clear',
	 * 
	 * // Translation of the Today shortcut button text. today: 'Today',
	 * 
	 * // Translation of the Cancel button text. cancel: 'Cancel',
	 * 
	 * // A function to format given {@code Date} object as // date string.
	 * formatDate: d => { // returns a string representation of the given //
	 * Date object in 'MM/DD/YYYY' -format },
	 * 
	 * // A function to parse the given text to a {@code Date}
	 * // object. Must properly parse (at least) text // formatted by
	 * {@code formatDate}. // Setting the property to null will disable //
	 * keyboard input feature. parseDate: text => { // Parses a string in
	 * 'MM/DD/YY', 'MM/DD' or 'DD' -format to // a Date object }
	 * 
	 * // A function to format given {@code monthName} and // {@code fullYear}
	 * integer as calendar title string. formatTitle: (monthName, fullYear) => {
	 * return monthName + ' ' + fullYear; } }
	 * 
	 * @param i18n
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	protected R setI18n(elemental.json.JsonObject i18n) {
		getElement().setPropertyJson("i18n", i18n);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The earliest date that can be selected. All earlier dates will be
	 * disabled.
	 * 
	 * Supported date formats: - ISO 8601 {@code "YYYY-MM-DD"} (default) -
	 * 6-digit extended ISO 8601 {@code "+YYYYYY-MM-DD"},
	 * {@code "-YYYYYY-MM-DD"}
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getMin() {
		return getElement().getProperty("min");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The earliest date that can be selected. All earlier dates will be
	 * disabled.
	 * 
	 * Supported date formats: - ISO 8601 {@code "YYYY-MM-DD"} (default) -
	 * 6-digit extended ISO 8601 {@code "+YYYYYY-MM-DD"},
	 * {@code "-YYYYYY-MM-DD"}
	 * 
	 * @param min
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setMin(java.lang.String min) {
		getElement().setProperty("min", min == null ? "" : min);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The latest date that can be selected. All later dates will be disabled.
	 * 
	 * Supported date formats: - ISO 8601 {@code "YYYY-MM-DD"} (default) -
	 * 6-digit extended ISO 8601 {@code "+YYYYYY-MM-DD"},
	 * {@code "-YYYYYY-MM-DD"}
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getMax() {
		return getElement().getProperty("max");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The latest date that can be selected. All later dates will be disabled.
	 * 
	 * Supported date formats: - ISO 8601 {@code "YYYY-MM-DD"} (default) -
	 * 6-digit extended ISO 8601 {@code "+YYYYYY-MM-DD"},
	 * {@code "-YYYYYY-MM-DD"}
	 * 
	 * @param max
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setMax(java.lang.String max) {
		getElement().setProperty("max", max == null ? "" : max);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the two-way data-bindable property representing the value of the
	 * custom input field.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAttrForValue() {
		return getElement().getProperty("attrForValue");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the two-way data-bindable property representing the value of the
	 * custom input field.
	 * 
	 * @param attrForValue
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setAttrForValue(java.lang.String attrForValue) {
		getElement().setProperty("attrForValue",
				attrForValue == null ? "" : attrForValue);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Opens the dropdown.
	 */
	public void open() {
		getElement().callFunction("open");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Closes the dropdown.
	 */
	public void close() {
		getElement().callFunction("close");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if {@code value} is valid, and sets the {@code invalid} flag
	 * appropriatelly.
	 * 
	 * @param value
	 *            Missing documentation!
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void validate(elemental.json.JsonObject value) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if the current input value satisfies all constraints (if
	 * any)
	 * 
	 * Override the {@code checkValidity} method for custom validations.
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
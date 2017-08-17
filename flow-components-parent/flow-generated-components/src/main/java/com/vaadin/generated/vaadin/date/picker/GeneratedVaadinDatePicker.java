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
import com.vaadin.ui.HasSize;
import com.vaadin.ui.HasClickListeners;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <vaadin-date-picker>} is a date selection field which includes a
 * scrollable month calendar view. {@code }`html <vaadin-date-picker
 * label="Birthday"></vaadin-date-picker> {@code }` {@code }`js datePicker.value =
 * '2016-03-02'; {@code }` When the selected {@code value} is changed, a
 * {@code value-changed} event is triggered.
 * 
 * 
 * ### Styling
 * 
 * The following shadow DOM parts are available for styling:
 * 
 * Part name | Description | Theme for Element
 * ----------------|----------------|---------------- {@code text-field} | Input
 * element | vaadin-date-picker {@code clear-button} | Clear button |
 * vaadin-date-picker {@code toggle-button} | Toggle button | vaadin-date-picker
 * {@code overlay} | The overlay element | vaadin-date-picker {@code overlay} |
 * The overlay element | vaadin-date-picker-light {@code overlay-header} |
 * Fullscreen mode header | vaadin-date-picker-overlay {@code label} |
 * Fullscreen mode value/label | vaadin-date-picker-overlay {@code clear-button}
 * | Fullscreen mode clear button | vaadin-date-picker-overlay
 * {@code toggle-button} | Fullscreen mode toggle button |
 * vaadin-date-picker-overlay {@code years-toggle-button} | Fullscreen mode
 * years scroller toggle | vaadin-date-picker-overlay {@code months} | Months
 * scroller | vaadin-date-picker-overlay {@code years} | Years scroller |
 * vaadin-date-picker-overlay {@code toolbar} | Footer bar with buttons |
 * vaadin-date-picker-overlay {@code today-button} | Today button |
 * vaadin-date-picker-overlay {@code cancel-button} | Cancel button |
 * vaadin-date-picker-overlay {@code month} | Month calendar |
 * vaadin-date-picker-overlay {@code year-number} | Year number |
 * vaadin-date-picker-overlay {@code year-separator} | Year separator |
 * vaadin-date-picker-overlay {@code month-header} | Month title |
 * vaadin-month-calendar {@code weekdays} | Weekday container |
 * vaadin-month-calendar {@code weekday} | Weekday element |
 * vaadin-month-calendar {@code week-numbers} | Week numbers container |
 * vaadin-month-calendar {@code week-number} | Week number element |
 * vaadin-month-calendar {@code date} | Date element | vaadin-month-calendar
 * 
 * If you want to replace the default input field with a custom implementation,
 * you should use the [{@code <vaadin-date-picker-light>}
 * ](#vaadin-date-picker-light) element.
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#0.1-SNAPSHOT",
		"WebComponent: Vaadin.DatePickerElement#2.0.1", "Flow#0.1-SNAPSHOT"})
@Tag("vaadin-date-picker")
@HtmlImport("frontend://bower_components/vaadin-date-picker/vaadin-date-picker.html")
public class GeneratedVaadinDatePicker<R extends GeneratedVaadinDatePicker<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			HasSize,
			HasClickListeners<R> {

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
	public String getValueAsString() {
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
	 * @param valueAsString
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setValueAsString(java.lang.String valueAsString) {
		getElement().setProperty("value",
				valueAsString == null ? "" : valueAsString);
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
	public String getMinAsString() {
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
	 * @param minAsString
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setMinAsString(java.lang.String minAsString) {
		getElement().setProperty("min", minAsString == null ? "" : minAsString);
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
	public String getMaxAsString() {
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
	 * @param maxAsString
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setMaxAsString(java.lang.String maxAsString) {
		getElement().setProperty("max", maxAsString == null ? "" : maxAsString);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable this element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isDisabled() {
		return getElement().getProperty("disabled", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable this element.
	 * 
	 * @param disabled
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The error message to display when the input is invalid.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getErrorMessage() {
		return getElement().getProperty("errorMessage");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The error message to display when the input is invalid.
	 * 
	 * @param errorMessage
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setErrorMessage(java.lang.String errorMessage) {
		getElement().setProperty("errorMessage",
				errorMessage == null ? "" : errorMessage);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getPlaceholder() {
		return getElement().getProperty("placeholder");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
	 * 
	 * @param placeholder
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setPlaceholder(java.lang.String placeholder) {
		getElement().setProperty("placeholder",
				placeholder == null ? "" : placeholder);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to make this element read-only.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isReadonly() {
		return getElement().getProperty("readonly", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to make this element read-only.
	 * 
	 * @param readonly
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setReadonly(boolean readonly) {
		getElement().setProperty("readonly", readonly);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is set to true when the control value invalid.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'invalid-changed' event happens.
	 */
	@Synchronize(property = "invalid", value = "invalid-changed")
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is set to true when the control value invalid.
	 * 
	 * @param invalid
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
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

	@DomEvent("invalid-changed")
	public static class InvalidChangeEvent<R extends GeneratedVaadinDatePicker<R>>
			extends
				ComponentEvent<R> {
		public InvalidChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addInvalidChangeListener(
			ComponentEventListener<InvalidChangeEvent<R>> listener) {
		return addListener(InvalidChangeEvent.class,
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
		getElement().getChildren().forEach(
				child -> child.removeAttribute("slot"));
		getElement().removeAllChildren();
	}
}
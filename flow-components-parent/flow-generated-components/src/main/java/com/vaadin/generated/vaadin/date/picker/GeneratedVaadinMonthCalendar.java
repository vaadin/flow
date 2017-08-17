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
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

@Generated({"Generator: com.vaadin.generator.ComponentGenerator#0.1-SNAPSHOT",
		"WebComponent: MonthCalendarElement#2.0.1", "Flow#0.1-SNAPSHOT"})
@Tag("vaadin-month-calendar")
@HtmlImport("frontend://bower_components/vaadin-date-picker/vaadin-month-calendar.html")
public class GeneratedVaadinMonthCalendar<R extends GeneratedVaadinMonthCalendar<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle, HasSize {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A {@code Date} object defining the month to be displayed. Only year and
	 * month properties are actually used.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonObject protectedGetMonth() {
		return (JsonObject) getElement().getPropertyRaw("month");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A {@code Date} object defining the month to be displayed. Only year and
	 * month properties are actually used.
	 * 
	 * @param month
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	protected R setMonth(elemental.json.JsonObject month) {
		getElement().setPropertyJson("month", month);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A {@code Date} object for the currently selected date.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonObject protectedGetSelectedDate() {
		return (JsonObject) getElement().getPropertyRaw("selectedDate");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A {@code Date} object for the currently selected date.
	 * 
	 * @param selectedDate
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	protected R setSelectedDate(elemental.json.JsonObject selectedDate) {
		getElement().setPropertyJson("selectedDate", selectedDate);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A {@code Date} object for the currently focused date.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonObject protectedGetFocusedDate() {
		return (JsonObject) getElement().getPropertyRaw("focusedDate");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A {@code Date} object for the currently focused date.
	 * 
	 * @param focusedDate
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	protected R setFocusedDate(elemental.json.JsonObject focusedDate) {
		getElement().setPropertyJson("focusedDate", focusedDate);
		return get();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isShowWeekNumbers() {
		return getElement().getProperty("showWeekNumbers", false);
	}

	/**
	 * @param showWeekNumbers
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setShowWeekNumbers(boolean showWeekNumbers) {
		getElement().setProperty("showWeekNumbers", showWeekNumbers);
		return get();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonObject protectedGetI18n() {
		return (JsonObject) getElement().getPropertyRaw("i18n");
	}

	/**
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
	 * Flag stating whether taps on the component should be ignored.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isIgnoreTaps() {
		return getElement().getProperty("ignoreTaps", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Flag stating whether taps on the component should be ignored.
	 * 
	 * @param ignoreTaps
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setIgnoreTaps(boolean ignoreTaps) {
		getElement().setProperty("ignoreTaps", ignoreTaps);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The earliest date that can be selected. All earlier dates will be
	 * disabled.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonObject protectedGetMinDate() {
		return (JsonObject) getElement().getPropertyRaw("minDate");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The earliest date that can be selected. All earlier dates will be
	 * disabled.
	 * 
	 * @param minDate
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	protected R setMinDate(elemental.json.JsonObject minDate) {
		getElement().setPropertyJson("minDate", minDate);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The latest date that can be selected. All later dates will be disabled.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonObject protectedGetMaxDate() {
		return (JsonObject) getElement().getPropertyRaw("maxDate");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The latest date that can be selected. All later dates will be disabled.
	 * 
	 * @param maxDate
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	protected R setMaxDate(elemental.json.JsonObject maxDate) {
		getElement().setPropertyJson("maxDate", maxDate);
		return get();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isDisabled() {
		return getElement().getProperty("disabled", false);
	}

	@DomEvent("selected-date-changed")
	public static class SelectedDateChangeEvent<R extends GeneratedVaadinMonthCalendar<R>>
			extends
				ComponentEvent<R> {
		public SelectedDateChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addSelectedDateChangeListener(
			ComponentEventListener<SelectedDateChangeEvent<R>> listener) {
		return addListener(SelectedDateChangeEvent.class,
				(ComponentEventListener) listener);
	}
}
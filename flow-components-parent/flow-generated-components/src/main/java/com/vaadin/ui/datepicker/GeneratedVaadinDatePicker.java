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
import com.vaadin.ui.common.HasClickListeners;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.common.NotSupported;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-date-picker>} is a date selection field which includes a
 * scrollable month calendar view. {@code }
 * <code>html &lt;vaadin-date-picker label=&quot;Birthday&quot;&gt;&lt;/vaadin-date-picker&gt; {@code }</code>
 * {@code }<code>js datePicker.value = '2016-03-02'; {@code }</code> When the
 * selected {@code value} is changed, a {@code value-changed} event is
 * triggered.
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
 * <td>{@code text-field}</td>
 * <td>Input element</td>
 * <td>vaadin-date-picker</td>
 * </tr>
 * <tr>
 * <td>{@code clear-button}</td>
 * <td>Clear button</td>
 * <td>vaadin-date-picker</td>
 * </tr>
 * <tr>
 * <td>{@code toggle-button}</td>
 * <td>Toggle button</td>
 * <td>vaadin-date-picker</td>
 * </tr>
 * <tr>
 * <td>{@code overlay}</td>
 * <td>The overlay element</td>
 * <td>vaadin-date-picker</td>
 * </tr>
 * <tr>
 * <td>{@code overlay}</td>
 * <td>The overlay element</td>
 * <td>vaadin-date-picker-light</td>
 * </tr>
 * <tr>
 * <td>{@code overlay-header}</td>
 * <td>Fullscreen mode header</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code label}</td>
 * <td>Fullscreen mode value/label</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code clear-button}</td>
 * <td>Fullscreen mode clear button</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code toggle-button}</td>
 * <td>Fullscreen mode toggle button</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code years-toggle-button}</td>
 * <td>Fullscreen mode years scroller toggle</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code months}</td>
 * <td>Months scroller</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code years}</td>
 * <td>Years scroller</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code toolbar}</td>
 * <td>Footer bar with buttons</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code today-button}</td>
 * <td>Today button</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code cancel-button}</td>
 * <td>Cancel button</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code month}</td>
 * <td>Month calendar</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code year-number}</td>
 * <td>Year number</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code year-separator}</td>
 * <td>Year separator</td>
 * <td>vaadin-date-picker-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code month-header}</td>
 * <td>Month title</td>
 * <td>vaadin-month-calendar</td>
 * </tr>
 * <tr>
 * <td>{@code weekdays}</td>
 * <td>Weekday container</td>
 * <td>vaadin-month-calendar</td>
 * </tr>
 * <tr>
 * <td>{@code weekday}</td>
 * <td>Weekday element</td>
 * <td>vaadin-month-calendar</td>
 * </tr>
 * <tr>
 * <td>{@code week-numbers}</td>
 * <td>Week numbers container</td>
 * <td>vaadin-month-calendar</td>
 * </tr>
 * <tr>
 * <td>{@code week-number}</td>
 * <td>Week number element</td>
 * <td>vaadin-month-calendar</td>
 * </tr>
 * <tr>
 * <td>{@code date}</td>
 * <td>Date element</td>
 * <td>vaadin-month-calendar</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * If you want to replace the default input field with a custom implementation,
 * you should use the <a href="#vaadin-date-picker-light">
 * {@code <vaadin-date-picker-light>}</a> element.
 * </p>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: Vaadin.DatePickerElement#2.0.5", "Flow#1.0-SNAPSHOT"})
@Tag("vaadin-date-picker")
@HtmlImport("frontend://bower_components/vaadin-date-picker/vaadin-date-picker.html")
public class GeneratedVaadinDatePicker<R extends GeneratedVaadinDatePicker<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			HasClickListeners<R> {

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
	public String getValueAsString() {
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
	 * @param valueAsString
	 *            the String value to set
	 */
	public void setValueAsString(java.lang.String valueAsString) {
		getElement().setProperty("value",
				valueAsString == null ? "" : valueAsString);
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
	public String getMinAsString() {
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
	 * @param minAsString
	 *            the String value to set
	 */
	public void setMinAsString(java.lang.String minAsString) {
		getElement().setProperty("min", minAsString == null ? "" : minAsString);
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
	public String getMaxAsString() {
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
	 * @param maxAsString
	 *            the String value to set
	 */
	public void setMaxAsString(java.lang.String maxAsString) {
		getElement().setProperty("max", maxAsString == null ? "" : maxAsString);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to disable this element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code disabled} property from the webcomponent
	 */
	public boolean isDisabled() {
		return getElement().getProperty("disabled", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to disable this element.
	 * </p>
	 * 
	 * @param disabled
	 *            the boolean value to set
	 */
	public void setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The error message to display when the input is invalid.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code errorMessage} property from the webcomponent
	 */
	public String getErrorMessage() {
		return getElement().getProperty("errorMessage");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The error message to display when the input is invalid.
	 * </p>
	 * 
	 * @param errorMessage
	 *            the String value to set
	 */
	public void setErrorMessage(java.lang.String errorMessage) {
		getElement().setProperty("errorMessage",
				errorMessage == null ? "" : errorMessage);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code placeholder} property from the webcomponent
	 */
	public String getPlaceholder() {
		return getElement().getProperty("placeholder");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
	 * </p>
	 * 
	 * @param placeholder
	 *            the String value to set
	 */
	public void setPlaceholder(java.lang.String placeholder) {
		getElement().setProperty("placeholder",
				placeholder == null ? "" : placeholder);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to make this element read-only.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code readonly} property from the webcomponent
	 */
	public boolean isReadonly() {
		return getElement().getProperty("readonly", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to make this element read-only.
	 * </p>
	 * 
	 * @param readonly
	 *            the boolean value to set
	 */
	public void setReadonly(boolean readonly) {
		getElement().setProperty("readonly", readonly);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This property is set to true when the control value invalid.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'invalid-changed' event happens.
	 * </p>
	 * 
	 * @return the {@code invalid} property from the webcomponent
	 */
	@Synchronize(property = "invalid", value = "invalid-changed")
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This property is set to true when the control value invalid.
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
/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.vaadin.annotations.Tag;
import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.validator.DateRangeValidator;
import com.vaadin.event.FieldEvents;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.shared.ui.datefield.TextualDateFieldState;

/**
 * <p>
 * A date editor component that can be bound to any {@link Property} that is
 * compatible with <code>java.util.Date</code>.
 * </p>
 * <p>
 * Since <code>DateField</code> extends <code>AbstractField</code> it implements
 * the {@link com.vaadin.data.Buffered}interface.
 * </p>
 * <p>
 * A <code>DateField</code> is in write-through mode by default, so
 * {@link com.vaadin.ui.AbstractField#setWriteThrough(boolean)}must be called to
 * enable buffering.
 * </p>
 *
 * @author Vaadin Ltd.
 * @since 3.0
 */
@SuppressWarnings("serial")
@Tag("input")
public class DateField extends AbstractField<Date>
        implements FieldEvents.BlurNotifier, FieldEvents.FocusNotifier {

    /**
     * Specified smallest modifiable unit for the date field.
     */
    private Resolution resolution = Resolution.DAY;

    /**
     * The internal calendar to be used in java.utl.Date conversions.
     */
    private transient Calendar calendar;

    /**
     * Overridden format string
     */
    private String dateFormat;

    private boolean lenient = false;

    private String dateString = null;

    /**
     * Was the last entered string parsable? If this flag is false, datefields
     * internal validator does not pass.
     */
    private boolean uiHasValidDateString = true;

    /**
     * Determines if week numbers are shown in the date selector.
     */
    private boolean showISOWeekNumbers = false;

    private String currentParseErrorMessage;

    private String defaultParseErrorMessage = "Date format not recognized";

    private TimeZone timeZone = null;

    private static Map<Resolution, String> variableNameForResolution = new HashMap<Resolution, String>();

    private String dateOutOfRangeMessage = "Date is out of allowed range";

    private static final DateFormat elementDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd");

    private DateRangeValidator currentRangeValidator;

    /**
     * Determines whether the ValueChangeEvent should be fired. Used to prevent
     * firing the event when UI has invalid string until uiHasValidDateString
     * flag is set
     */
    private boolean preventValueChangeEvent = false;

    static {
        variableNameForResolution.put(Resolution.SECOND, "sec");
        variableNameForResolution.put(Resolution.MINUTE, "min");
        variableNameForResolution.put(Resolution.HOUR, "hour");
        variableNameForResolution.put(Resolution.DAY, "day");
        variableNameForResolution.put(Resolution.MONTH, "month");
        variableNameForResolution.put(Resolution.YEAR, "year");
    }

    /* Constructors */

    /**
     * Constructs an empty <code>DateField</code> with no caption.
     */
    public DateField() {
        getElement().setAttribute("type", "date");

        // Always "immediate"
        getElement().addEventData("change", "value");
        getElement().addEventListener("change", e -> {
            String valueString = e.getString("value");
            if (valueString == null || valueString.isEmpty()) {
                setValue(null);
            } else {
                try {
                    setValue(elementDateFormat.parse(valueString));
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }
            }
        });
    }

    /**
     * Constructs an empty <code>DateField</code> with caption.
     *
     * @param caption
     *            the caption of the datefield.
     */
    public DateField(String caption) {
        this();
        setCaption(caption);
    }

    /**
     * Constructs a new <code>DateField</code> that's bound to the specified
     * <code>Property</code> and has the given caption <code>String</code>.
     *
     * @param caption
     *            the caption <code>String</code> for the editor.
     * @param dataSource
     *            the Property to be edited with this editor.
     */
    public DateField(String caption, Property dataSource) {
        this(dataSource);
        setCaption(caption);
    }

    /**
     * Constructs a new <code>DateField</code> that's bound to the specified
     * <code>Property</code> and has no caption.
     *
     * @param dataSource
     *            the Property to be edited with this editor.
     */
    public DateField(Property dataSource) throws IllegalArgumentException {
        this();
        if (!Date.class.isAssignableFrom(dataSource.getType())) {
            throw new IllegalArgumentException(
                    "Can't use " + dataSource.getType().getName()
                            + " typed property as datasource");
        }

        setPropertyDataSource(dataSource);
    }

    /**
     * Constructs a new <code>DateField</code> with the given caption and
     * initial text contents. The editor constructed this way will not be bound
     * to a Property unless
     * {@link com.vaadin.data.Property.Viewer#setPropertyDataSource(Property)}
     * is called to bind it.
     *
     * @param caption
     *            the caption <code>String</code> for the editor.
     * @param value
     *            the Date value.
     */
    public DateField(String caption, Date value) {
        this();
        setValue(value);
        setCaption(caption);
    }

    /* Component basic features */

    @Override
    protected boolean shouldHideErrors() {
        return super.shouldHideErrors() && uiHasValidDateString;
    }

    @Override
    protected TextualDateFieldState getState() {
        return (TextualDateFieldState) super.getState();
    }

    @Override
    protected TextualDateFieldState getState(boolean markAsDirty) {
        return (TextualDateFieldState) super.getState(markAsDirty);
    }

    /**
     * Sets the start range for this component. If the value is set before this
     * date (taking the resolution into account), the component will not
     * validate. If <code>startDate</code> is set to <code>null</code>, any
     * value before <code>endDate</code> will be accepted by the range
     *
     * @param startDate
     *            - the allowed range's start date
     */
    public void setRangeStart(Date startDate) {
        if (startDate != null && getState().rangeEnd != null
                && startDate.after(getState().rangeEnd)) {
            throw new IllegalStateException(
                    "startDate cannot be later than endDate");
        }

        // Create a defensive copy against issues when using java.sql.Date (and
        // also against mutable Date).
        getState().rangeStart = startDate != null
                ? new Date(startDate.getTime()) : null;
        updateRangeValidator();
    }

    /**
     * Sets the current error message if the range validation fails.
     *
     * @param dateOutOfRangeMessage
     *            - Localizable message which is shown when value (the date) is
     *            set outside allowed range
     */
    public void setDateOutOfRangeMessage(String dateOutOfRangeMessage) {
        this.dateOutOfRangeMessage = dateOutOfRangeMessage;
        updateRangeValidator();
    }

    /**
     * Gets the end range for a certain resolution. The range is inclusive, so
     * if rangeEnd is set to zero milliseconds past year n and resolution is set
     * to YEAR, any date in year n will be accepted. Resolutions lower than DAY
     * will be interpreted on a DAY level. That is, everything below DATE is
     * cleared
     *
     * @param forResolution
     *            - the range conforms to the resolution
     * @return
     */
    private Date getRangeEnd(Resolution forResolution) {
        // We need to set the correct resolution for the dates,
        // otherwise the range validator will complain

        Date rangeEnd = getState(false).rangeEnd;
        if (rangeEnd == null) {
            return null;
        }

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(rangeEnd);

        if (forResolution == Resolution.YEAR) {
            // Adding one year (minresolution) and clearing the rest.
            endCal.set(endCal.get(Calendar.YEAR) + 1, 0, 1, 0, 0, 0);
        } else if (forResolution == Resolution.MONTH) {
            // Adding one month (minresolution) and clearing the rest.
            endCal.set(endCal.get(Calendar.YEAR),
                    endCal.get(Calendar.MONTH) + 1, 1, 0, 0, 0);
        } else {
            endCal.set(endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH),
                    endCal.get(Calendar.DATE) + 1, 0, 0, 0);
        }
        // removing one millisecond will now get the endDate to return to
        // current resolution's set time span (year or month)
        endCal.set(Calendar.MILLISECOND, -1);
        return endCal.getTime();
    }

    /**
     * Gets the start range for a certain resolution. The range is inclusive, so
     * if <code>rangeStart</code> is set to one millisecond before year n and
     * resolution is set to YEAR, any date in year n - 1 will be accepted.
     * Lowest supported resolution is DAY.
     *
     * @param forResolution
     *            - the range conforms to the resolution
     * @return
     */
    private Date getRangeStart(Resolution forResolution) {
        if (getState(false).rangeStart == null) {
            return null;
        }
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(getState(false).rangeStart);

        if (forResolution == Resolution.YEAR) {
            startCal.set(startCal.get(Calendar.YEAR), 0, 1, 0, 0, 0);
        } else if (forResolution == Resolution.MONTH) {
            startCal.set(startCal.get(Calendar.YEAR),
                    startCal.get(Calendar.MONTH), 1, 0, 0, 0);
        } else {
            startCal.set(startCal.get(Calendar.YEAR),
                    startCal.get(Calendar.MONTH), startCal.get(Calendar.DATE),
                    0, 0, 0);
        }

        startCal.set(Calendar.MILLISECOND, 0);
        return startCal.getTime();
    }

    private void updateRangeValidator() {
        if (currentRangeValidator != null) {
            removeValidator(currentRangeValidator);
            currentRangeValidator = null;
        }
        if (getRangeStart() != null || getRangeEnd() != null) {
            currentRangeValidator = new DateRangeValidator(
                    dateOutOfRangeMessage, getRangeStart(resolution),
                    getRangeEnd(resolution), null);
            addValidator(currentRangeValidator);
        }
    }

    /**
     * Sets the end range for this component. If the value is set after this
     * date (taking the resolution into account), the component will not
     * validate. If <code>endDate</code> is set to <code>null</code>, any value
     * after <code>startDate</code> will be accepted by the range.
     *
     * @param endDate
     *            - the allowed range's end date (inclusive, based on the
     *            current resolution)
     */
    public void setRangeEnd(Date endDate) {
        if (endDate != null && getState().rangeStart != null
                && getState().rangeStart.after(endDate)) {
            throw new IllegalStateException(
                    "endDate cannot be earlier than startDate");
        }

        // Create a defensive copy against issues when using java.sql.Date (and
        // also against mutable Date).
        getState().rangeEnd = endDate != null ? new Date(endDate.getTime())
                : null;
        updateRangeValidator();
    }

    /**
     * Returns the precise rangeStart used.
     *
     * @param startDate
     *
     */
    public Date getRangeStart() {
        return getState(false).rangeStart;
    }

    /**
     * Returns the precise rangeEnd used.
     *
     * @param startDate
     */
    public Date getRangeEnd() {
        return getState(false).rangeEnd;
    }

    /*
     * only fires the event if preventValueChangeEvent flag is false
     */
    @Override
    protected void fireValueChange(boolean repaintIsNotNeeded) {
        if (!preventValueChangeEvent) {
            super.fireValueChange(repaintIsNotNeeded);
        }
    }

    /**
     * This method is called to handle a non-empty date string from the client
     * if the client could not parse it as a Date.
     *
     * By default, a Converter.ConversionException is thrown, and the current
     * value is not modified.
     *
     * This can be overridden to handle conversions, to return null (equivalent
     * to empty input), to throw an exception or to fire an event.
     *
     * @param dateString
     * @return parsed Date
     * @throws Converter.ConversionException
     *             to keep the old value and indicate an error
     */
    protected Date handleUnparsableDateString(String dateString)
            throws Converter.ConversionException {
        currentParseErrorMessage = null;
        throw new Converter.ConversionException(getParseErrorMessage());
    }

    /* Property features */

    /*
     * Gets the edited property's type. Don't add a JavaDoc comment here, we use
     * the default documentation from implemented interface.
     */
    @Override
    public Class<Date> getType() {
        return Date.class;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.AbstractField#setValue(java.lang.Object, boolean)
     */
    @Override
    protected void setValue(Date newValue, boolean repaintIsNotNeeded)
            throws Property.ReadOnlyException {

        /*
         * First handle special case when the client side component have a date
         * string but value is null (e.g. unparsable date string typed in by the
         * user). No value changes should happen, but we need to do some
         * internal housekeeping.
         */
        if (newValue == null && !uiHasValidDateString) {
            /*
             * Side-effects of setInternalValue clears possible previous strings
             * and flags about invalid input.
             */
            setInternalValue(null);

            /*
             * Due to DateField's special implementation of isValid(),
             * datefields validity may change although the logical value does
             * not change. This is an issue for Form which expects that validity
             * of Fields cannot change unless actual value changes.
             *
             * So we check if this field is inside a form and the form has
             * registered this as a field. In this case we repaint the form.
             * Without this hacky solution the form might not be able to clean
             * validation errors etc. We could avoid this by firing an extra
             * value change event, but feels like at least as bad solution as
             * this.
             */
            markAsDirty();
            return;
        }

        super.setValue(newValue, repaintIsNotNeeded);
    }

    @Override
    protected void setInternalValue(Date newValue) {
        // Also set the internal dateString
        if (newValue != null) {
            dateString = newValue.toString();
        } else {
            dateString = null;
        }

        if (!uiHasValidDateString) {
            // clear component error and parsing flag
            setComponentError(null);
            uiHasValidDateString = true;
            currentParseErrorMessage = null;
        }

        super.setInternalValue(newValue);
        if (newValue == null) {
            getElement().setAttribute("value", null);
        } else {
            getElement().setAttribute("value",
                    elementDateFormat.format(newValue));
        }
    }

    /**
     * Gets the resolution.
     *
     * @return int
     */
    public Resolution getResolution() {
        return resolution;
    }

    /**
     * Sets the resolution of the DateField.
     *
     * The default resolution is {@link Resolution#DAY} since Vaadin 7.0.
     *
     * @param resolution
     *            the resolution to set.
     */
    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
        updateRangeValidator();
        markAsDirty();
    }

    /**
     * Returns new instance calendar used in Date conversions.
     *
     * Returns new clone of the calendar object initialized using the the
     * current date (if available)
     *
     * If this is no calendar is assigned the <code>Calendar.getInstance</code>
     * is used.
     *
     * @return the Calendar.
     * @see #setCalendar(Calendar)
     */
    private Calendar getCalendar() {

        // Makes sure we have an calendar instance
        if (calendar == null) {
            calendar = Calendar.getInstance();
            // Start by a zeroed calendar to avoid having values for lower
            // resolution variables e.g. time when resolution is day
            int min, field;
            for (Resolution r : Resolution
                    .getResolutionsLowerThan(resolution)) {
                field = r.getCalendarField();
                min = calendar.getActualMinimum(field);
                calendar.set(field, min);
            }
            calendar.set(Calendar.MILLISECOND, 0);
        }

        // Clone the instance
        final Calendar newCal = (Calendar) calendar.clone();

        final TimeZone currentTimeZone = getTimeZone();
        if (currentTimeZone != null) {
            newCal.setTimeZone(currentTimeZone);
        }

        final Date currentDate = getValue();
        if (currentDate != null) {
            newCal.setTime(currentDate);
        }
        return newCal;
    }

    /**
     * Sets formatting used by some component implementations. See
     * {@link SimpleDateFormat} for format details.
     *
     * By default it is encouraged to used default formatting defined by Locale,
     * but due some JVM bugs it is sometimes necessary to use this method to
     * override formatting. See Vaadin issue #2200.
     *
     * @param dateFormat
     *            the dateFormat to set
     *
     * @see com.vaadin.ui.AbstractComponent#setLocale(Locale))
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        markAsDirty();
    }

    /**
     * Returns a format string used to format date value on client side or null
     * if default formatting from {@link Component#getLocale()} is used.
     *
     * @return the dateFormat
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * Specifies whether or not date/time interpretation in component is to be
     * lenient.
     *
     * @see Calendar#setLenient(boolean)
     * @see #isLenient()
     *
     * @param lenient
     *            true if the lenient mode is to be turned on; false if it is to
     *            be turned off.
     */
    public void setLenient(boolean lenient) {
        this.lenient = lenient;
        markAsDirty();
    }

    /**
     * Returns whether date/time interpretation is to be lenient.
     *
     * @see #setLenient(boolean)
     *
     * @return true if the interpretation mode of this calendar is lenient;
     *         false otherwise.
     */
    public boolean isLenient() {
        return lenient;
    }

    /**
     * Checks whether ISO 8601 week numbers are shown in the date selector.
     *
     * @return true if week numbers are shown, false otherwise.
     */
    public boolean isShowISOWeekNumbers() {
        return showISOWeekNumbers;
    }

    /**
     * Sets the visibility of ISO 8601 week numbers in the date selector. ISO
     * 8601 defines that a week always starts with a Monday so the week numbers
     * are only shown if this is the case.
     *
     * @param showWeekNumbers
     *            true if week numbers should be shown, false otherwise.
     */
    public void setShowISOWeekNumbers(boolean showWeekNumbers) {
        showISOWeekNumbers = showWeekNumbers;
        markAsDirty();
    }

    /**
     * Validates the current value against registered validators if the field is
     * not empty. Note that DateField is considered empty (value == null) and
     * invalid if it contains text typed in by the user that couldn't be parsed
     * into a Date value.
     *
     * @see com.vaadin.ui.AbstractField#validate()
     */
    @Override
    public void validate() throws InvalidValueException {
        /*
         * To work properly in form we must throw exception if there is
         * currently a parsing error in the datefield. Parsing error is kind of
         * an internal validator.
         */
        if (!uiHasValidDateString) {
            throw new UnparsableDateString(currentParseErrorMessage);
        }
        super.validate();
    }

    /**
     * Return the error message that is shown if the user inputted value can't
     * be parsed into a Date object. If
     * {@link #handleUnparsableDateString(String)} is overridden and it throws a
     * custom exception, the message returned by
     * {@link Exception#getLocalizedMessage()} will be used instead of the value
     * returned by this method.
     *
     * @see #setParseErrorMessage(String)
     *
     * @return the error message that the DateField uses when it can't parse the
     *         textual input from user to a Date object
     */
    public String getParseErrorMessage() {
        return defaultParseErrorMessage;
    }

    /**
     * Sets the default error message used if the DateField cannot parse the
     * text input by user to a Date field. Note that if the
     * {@link #handleUnparsableDateString(String)} method is overridden, the
     * localized message from its exception is used.
     *
     * @see #getParseErrorMessage()
     * @see #handleUnparsableDateString(String)
     * @param parsingErrorMessage
     */
    public void setParseErrorMessage(String parsingErrorMessage) {
        defaultParseErrorMessage = parsingErrorMessage;
    }

    /**
     * Sets the time zone used by this date field. The time zone is used to
     * convert the absolute time in a Date object to a logical time displayed in
     * the selector and to convert the select time back to a Date object.
     *
     * If no time zone has been set, the current default time zone returned by
     * {@code TimeZone.getDefault()} is used.
     *
     * @see #getTimeZone()
     * @param timeZone
     *            the time zone to use for time calculations.
     */
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        markAsDirty();
    }

    /**
     * Gets the time zone used by this field. The time zone is used to convert
     * the absolute time in a Date object to a logical time displayed in the
     * selector and to convert the select time back to a Date object.
     *
     * If {@code null} is returned, the current default time zone returned by
     * {@code TimeZone.getDefault()} is used.
     *
     * @return the current time zone
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    public static class UnparsableDateString
            extends Validator.InvalidValueException {

        public UnparsableDateString(String message) {
            super(message);
        }

    }

    /**
     * Returns current date-out-of-range error message.
     *
     * @see #setDateOutOfRangeMessage(String)
     * @since 7.4
     * @return Current error message for dates out of range.
     */
    public String getDateOutOfRangeMessage() {
        return dateOutOfRangeMessage;
    }

}

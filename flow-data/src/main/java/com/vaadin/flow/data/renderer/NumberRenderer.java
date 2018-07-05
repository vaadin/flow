/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.data.renderer;

import java.text.NumberFormat;
import java.util.Locale;

import com.vaadin.flow.function.ValueProvider;

/**
 * 
 * A template renderer for presenting number values.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 * 
 * @param <SOURCE>
 *            the type of the input item, from which the number is extracted
 *
 */
public class NumberRenderer<SOURCE>
        extends BasicRenderer<SOURCE, Number> {

    private Locale locale;
    private NumberFormat numberFormat;
    private String formatString;
    private String nullRepresentation;

    /**
     * Creates a new number renderer.
     * <p>
     * The renderer is configured to render with the number's natural string
     * representation in the default locale.
     * 
     * @param valueProvider
     *            the callback to provide a {@link Number} to the renderer, not
     *            <code>null</code>
     */
    protected NumberRenderer(ValueProvider<SOURCE, Number> valueProvider) {
        this(valueProvider, Locale.getDefault());
    }

    /**
     * Creates a new number renderer.
     * <p>
     * The renderer is configured to render the number as defined with the given
     * number format.
     *
     * @param valueProvider
     *            the callback to provide a {@link Number} to the renderer, not
     *            <code>null</code>
     * @param numberFormat
     *            the number format with which to display numbers, not
     *            <code>null</code>
     */
    public NumberRenderer(ValueProvider<SOURCE, Number> valueProvider,
            NumberFormat numberFormat) {
        this(valueProvider, numberFormat, "");
    }

    /**
     * Creates a new number renderer.
     * <p>
     * The renderer is configured to render the number as defined with the given
     * number format.
     * 
     * @param valueProvider
     *            the callback to provide a {@link Number} to the renderer, not
     *            <code>null</code>
     * @param numberFormat
     *            the number format with which to display numbers, not
     *            <code>null</code>
     * @param nullRepresentation
     *            the textual representation of <code>null</code> value
     */
    public NumberRenderer(ValueProvider<SOURCE, Number> valueProvider,
            NumberFormat numberFormat, String nullRepresentation) {
        super(valueProvider);

        if (numberFormat == null) {
            throw new IllegalArgumentException("Number format may not be null");
        }

        locale = null;
        this.numberFormat = numberFormat;
        formatString = null;
        this.nullRepresentation = nullRepresentation;
    }

    /**
     * Creates a new number renderer.
     * <p>
     * The renderer is configured to render with the number's natural string
     * representation in the given locale.
     *
     * @param valueProvider
     *            the callback to provide a {@link Number} to the renderer, not
     *            <code>null</code>
     * @param locale
     *            the locale in which to display numbers
     * 
     */
    public NumberRenderer(ValueProvider<SOURCE, Number> valueProvider,
            Locale locale) {
        this(valueProvider, "%s", locale);
    }

    /**
     * Creates a new number renderer.
     * <p>
     * The renderer is configured to render with the number's natural string
     * representation in the given locale.
     *
     * @param valueProvider
     *            the callback to provide a {@link Number} to the renderer, not
     *            <code>null</code>
     * @param formatString
     *            the format string with which to format the number, not
     *            <code>null</code>
     * @param locale
     *            the locale in which to display numbers, not <code>null</code>
     */
    public NumberRenderer(ValueProvider<SOURCE, Number> valueProvider,
            String formatString, Locale locale) {
        // This will call #toString() during formatting
        this(valueProvider, formatString, locale, "");
    }

    /**
     * Creates a new number renderer.
     * <p>
     * The renderer is configured to render with the given format string in the
     * default locale.
     *
     * @param valueProvider
     *            the callback to provide a {@link Number} to the renderer, not
     *            <code>null</code>
     * @param formatString
     *            the format string with which to format the number, not
     *            <code>null</code>
     * @see <a href=
     *      "http://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#syntax">
     *      Format String Syntax</a>
     */
    public NumberRenderer(ValueProvider<SOURCE, Number> valueProvider,
            String formatString) {
        this(valueProvider, formatString, Locale.getDefault(), "");
    }

    /**
     * Creates a new number renderer.
     * <p>
     * The renderer is configured to render with the given format string in the
     * given locale.
     *
     * @param valueProvider
     *            the callback to provide a {@link Number} to the renderer, not
     *            <code>null</code>
     * @param formatString
     *            the format string with which to format the number, not
     *            <code>null</code>
     * @param locale
     *            the locale in which to present numbers, not <code>null</code>
     * @param nullRepresentation
     *            the textual representation of <code>null</code> value
     * @see <a href=
     *      "http://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#syntax">
     *      Format String Syntax</a>
     */
    public NumberRenderer(ValueProvider<SOURCE, Number> valueProvider,
            String formatString, Locale locale, String nullRepresentation) {
        super(valueProvider);

        if (formatString == null) {
            throw new IllegalArgumentException("Format string may not be null");
        }

        if (locale == null) {
            throw new IllegalArgumentException("Locale may not be null");
        }

        this.locale = locale;
        numberFormat = null;
        this.formatString = formatString;
        this.nullRepresentation = nullRepresentation;
    }

    @Override
    protected String getFormattedValue(Number value) {
        String stringValue;
        if (value == null) {
            stringValue = nullRepresentation;
        } else if (formatString != null && locale != null) {
            stringValue = String.format(locale, formatString, value);
        } else if (numberFormat != null) {
            stringValue = numberFormat.format(value);
        } else {
            throw new IllegalStateException(String.format(
                    "Unable to format the given value: "
                            + "[locale: %s, numberFormat: %s, formatString: %s]",
                    locale, numberFormat, formatString));
        }
        return stringValue;
    }
}

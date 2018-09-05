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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import com.vaadin.flow.function.ValueProvider;

/**
 * A template renderer for presenting date values.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <SOURCE>
 *            the type of the input item, from which the {@link LocalDate} is
 *            extracted
 */
public class LocalDateRenderer<SOURCE>
        extends BasicRenderer<SOURCE, LocalDate> {

    private DateTimeFormatter formatter;
    private String nullRepresentation;

    /**
     * Creates a new LocalDateRenderer.
     * <p>
     * The renderer is configured with the format style {@code FormatStyle.LONG}
     * and an empty string as its null representation.
     *
     * @param valueProvider
     *            the callback to provide a {@link LocalDate} to the renderer,
     *            not <code>null</code>
     *
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/api/java/time/format/FormatStyle.html#LONG">
     *      FormatStyle.LONG</a>
     */
    public LocalDateRenderer(ValueProvider<SOURCE, LocalDate> valueProvider) {
        this(valueProvider, DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG),
                "");
    }

    /**
     * Creates a new LocalDateRenderer.
     * <p>
     * The renderer is configured to render with the given string format, with
     * an empty string as its null representation.
     *
     * @param valueProvider
     *            the callback to provide a {@link LocalDate} to the renderer,
     *            not <code>null</code>
     *
     * @param formatPattern
     *            the format pattern to format the date with, not
     *            <code>null</code>
     *
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#patterns">
     *      Format Pattern Syntax</a>
     */
    public LocalDateRenderer(ValueProvider<SOURCE, LocalDate> valueProvider,
            String formatPattern) {
        this(valueProvider, formatPattern, Locale.getDefault());
    }

    /**
     * Creates a new LocalDateRenderer.
     * <p>
     * The renderer is configured to render with the given string format, as
     * displayed in the given locale, with an empty string as its null
     * representation.
     *
     * @param valueProvider
     *            the callback to provide a {@link LocalDate} to the renderer,
     *            not <code>null</code>
     * @param formatPattern
     *            the format pattern to format the date with, not
     *            <code>null</code>
     * @param locale
     *            the locale to use, not <code>null</code>
     *
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#patterns">
     *      Format Pattern Syntax</a>
     */
    public LocalDateRenderer(ValueProvider<SOURCE, LocalDate> valueProvider,
            String formatPattern, Locale locale) {
        this(valueProvider, formatPattern, locale, "");
    }

    /**
     * Creates a new LocalDateRenderer.
     * <p>
     * The renderer is configured to render with the given string format, as
     * displayed in the given locale.
     *
     * @param valueProvider
     *            the callback to provide a {@link LocalDate} to the renderer,
     *            not <code>null</code>
     * @param formatPattern
     *            the format pattern to format the date with, not
     *            <code>null</code>
     * @param locale
     *            the locale to use, not <code>null</code>
     * @param nullRepresentation
     *            the textual representation of the <code>null</code> value
     *
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#patterns">
     *      Format Pattern Syntax</a>
     */
    public LocalDateRenderer(ValueProvider<SOURCE, LocalDate> valueProvider,
            String formatPattern, Locale locale, String nullRepresentation) {
        super(valueProvider);

        if (formatPattern == null) {
            throw new IllegalArgumentException(
                    "format pattern may not be null");
        }

        if (locale == null) {
            throw new IllegalArgumentException("locale may not be null");
        }

        formatter = DateTimeFormatter.ofPattern(formatPattern, locale);
        this.nullRepresentation = nullRepresentation;
    }

    /**
     * Creates a new LocalDateRenderer.
     * <p>
     * The renderer is configured to render with the given formatter, with an
     * empty string as its null representation.
     *
     * @param valueProvider
     *            the callback to provide a {@link LocalDate} to the renderer,
     *            not <code>null</code>
     * @param formatter
     *            the formatter to use, not <code>null</code>
     */
    public LocalDateRenderer(ValueProvider<SOURCE, LocalDate> valueProvider,
            DateTimeFormatter formatter) {
        this(valueProvider, formatter, "");
    }

    /**
     * Creates a new LocalDateRenderer.
     * <p>
     * The renderer is configured to render with the given formatter.
     *
     * @param valueProvider
     *            the callback to provide a {@link LocalDate} to the renderer,
     *            not <code>null</code>
     * @param formatter
     *            the formatter to use, not <code>null</code>
     * @param nullRepresentation
     *            the textual representation of the <code>null</code> value
     * 
     */
    public LocalDateRenderer(ValueProvider<SOURCE, LocalDate> valueProvider,
            DateTimeFormatter formatter, String nullRepresentation) {
        super(valueProvider);

        if (formatter == null) {
            throw new IllegalArgumentException("formatter may not be null");
        }

        this.formatter = formatter;
    }

    @Override
    protected String getFormattedValue(LocalDate date) {
        try {
            return date == null ? nullRepresentation : formatter.format(date);
        } catch (Exception e) {
            throw new IllegalStateException("Could not format input date '"
                    + date + "' using formatter '" + formatter + "'", e);
        }
    }
}

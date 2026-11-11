/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.data.converter;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;

import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Defines conversion between a model and a presentation type.
 * <p>
 * Converters must not have any side effects (never update UI from inside a
 * converter).
 *
 * @param <PRESENTATION>
 *            The presentation type.
 * @param <MODEL>
 *            The model type.
 * @author Vaadin Ltd
 * @since 1.0.
 */
public interface Converter<PRESENTATION, MODEL> extends Serializable {

    /**
     * Converts the given value from presentation type to model type.
     * <p>
     * A converter can optionally use locale to do the conversion.
     *
     * @param value
     *            The value to convert. Can be null
     * @param context
     *            The value context for the conversion.
     * @return The converted value compatible with the source type
     */
    Result<MODEL> convertToModel(PRESENTATION value, ValueContext context);

    /**
     * Converts the given value from model type to presentation type.
     * <p>
     * A converter can optionally use locale to do the conversion.
     *
     * @param value
     *            The value to convert. Can be null
     * @param context
     *            The value context for the conversion.
     * @return The converted value compatible with the source type
     */
    PRESENTATION convertToPresentation(MODEL value, ValueContext context);

    /**
     * Returns a converter that returns its input as-is in both directions.
     *
     * @param <T>
     *            the input and output type
     * @return an identity converter
     */
    static <T> Converter<T, T> identity() {
        return from(t -> Result.ok(t), t -> t);
    }

    /**
     * Constructs a converter from two functions. Any {@code Exception}
     * instances thrown from the {@code toModel} function are converted into
     * error-bearing {@code Result} objects using the given {@code onError}
     * function.
     *
     * @param <P>
     *            the presentation type
     * @param <M>
     *            the model type
     * @param toModel
     *            the function to convert to model
     * @param toPresentation
     *            the function to convert to presentation
     * @param onError
     *            the function to provide error messages
     * @return the new converter
     *
     * @see Result
     * @see Function
     */
    static <P, M> Converter<P, M> from(SerializableFunction<P, M> toModel,
            SerializableFunction<M, P> toPresentation,
            SerializableFunction<Exception, String> onError) {

        return from(val -> Result.of(() -> toModel.apply(val), onError),
                toPresentation);
    }

    /**
     * Constructs a converter from a filter and a function.
     *
     * @param <P>
     *            the presentation type
     * @param <M>
     *            the model type
     * @param toModel
     *            the function to convert to model
     * @param toPresentation
     *            the function to convert to presentation
     * @return the new converter
     *
     * @see Function
     */
    static <P, M> Converter<P, M> from(
            SerializableFunction<P, Result<M>> toModel,
            SerializableFunction<M, P> toPresentation) {
        return new Converter<P, M>() {

            @Override
            public Result<M> convertToModel(P value, ValueContext context) {
                return toModel.apply(value);
            }

            @Override
            public P convertToPresentation(M value, ValueContext context) {
                return toPresentation.apply(value);
            }
        };
    }

    /**
     * Returns a converter that chains together this converter with the given
     * type-compatible converter.
     * <p>
     * The chained converters will form a new converter capable of converting
     * from the presentation type of this converter to the model type of the
     * other converter.
     * <p>
     * In most typical cases you should not need this method but instead only
     * need to define one converter for a binding using
     * {@link BindingBuilder#withConverter(Converter)}.
     *
     * @param <T>
     *            the model type of the resulting converter
     * @param other
     *            the converter to chain, not null
     * @return a chained converter
     */
    default <T> Converter<PRESENTATION, T> chain(Converter<MODEL, T> other) {
        return new Converter<PRESENTATION, T>() {
            @Override
            public Result<T> convertToModel(PRESENTATION value,
                    ValueContext context) {
                Result<MODEL> model = Converter.this.convertToModel(value,
                        context);
                return model.flatMap(v -> other.convertToModel(v, context));
            }

            @Override
            public PRESENTATION convertToPresentation(T value,
                    ValueContext context) {
                MODEL model = other.convertToPresentation(value, context);
                return Converter.this.convertToPresentation(model, context);
            }
        };
    }

    /**
     * Wraps the given {@code converter} so that null values never reach it: a
     * null presentation value converts to a null model value, and a null model
     * value converts to a null presentation value. Other values are passed to
     * the wrapped converter.
     * <p>
     * Use this for fields whose empty value is null, such as a date picker or a
     * combo box. Do not use it for a field that does not accept null, such as a
     * text field: {@code withConverter} turns off the binder's default null
     * representation, so a null model value would reach the field as null and
     * the field would throw. Use {@link #nullSafeConverter(Converter, Object)}
     * with the field's empty value, such as {@code ""}, instead.
     * <p>
     * Example use:
     *
     * <pre>
     * {@code
     * binder.forField(datePicker)
     *         .withConverter(Converter.nullSafeConverter(Converter.from(
     *                 date -> date.atStartOfDay(ZoneOffset.UTC).toInstant(),
     *                 instant -> LocalDate.ofInstant(instant, ZoneOffset.UTC),
     *                 Exception::getMessage)))
     *         .bind(Event::getStart, Event::setStart);
     * }
     * </pre>
     *
     * @param <P>
     *            the presentation type
     * @param <M>
     *            the model type
     * @param converter
     *            the converter to delegate to for non-null values, not null
     * @return a converter that maps null to null in both directions
     * @see #nullSafeConverter(Converter, Object)
     */
    static <P, M> Converter<P, M> nullSafeConverter(Converter<P, M> converter) {
        return nullSafeConverter(converter, null);
    }

    /**
     * Wraps the given {@code converter} so that neither null nor the given
     * {@code nullRepresentation} ever reach it:
     * <ul>
     * <li>A presentation value that is null or equal to
     * {@code nullRepresentation} converts to a null model value.</li>
     * <li>A null model value, or a null returned by the wrapped converter,
     * converts to {@code nullRepresentation}.</li>
     * </ul>
     * Other values are passed to the wrapped converter. A model value that the
     * wrapped converter shows as {@code nullRepresentation}, such as a value
     * object holding an empty string, therefore reads back as null.
     * <p>
     * This is meant for fields whose empty value is not null, such as an empty
     * string in a text field: an empty field becomes a null model value, and a
     * null model value empties the field, without the wrapped converter having
     * to handle either case.
     * <p>
     * When binding with mapping functions rather than an existing converter,
     * {@link BindingBuilder#withNullSafeConverter(SerializableFunction, SerializableFunction, SerializableFunction, Object)}
     * does the same in one call.
     * <p>
     * Example use:
     *
     * <pre>
     * {@code
     * binder.forField(textField)
     *         .withConverter(Converter
     *                 .nullSafeConverter(Converter.from(LocalDate::parse,
     *                         LocalDate::toString, Exception::getMessage), ""))
     *         .bind(Person::getBirthDate, Person::setBirthDate);
     * }
     * </pre>
     *
     * @param <P>
     *            the presentation type
     * @param <M>
     *            the model type
     * @param converter
     *            the converter to delegate to for other values, not null
     * @param nullRepresentation
     *            the presentation value that stands for a null model value, for
     *            example {@code ""} for a text field; may be null
     * @return a converter that maps {@code nullRepresentation} to null and back
     * @see #nullSafeConverter(Converter)
     * @see BindingBuilder#withNullSafeConverter(SerializableFunction,
     *      SerializableFunction, SerializableFunction, Object)
     * @see BindingBuilder#withNullRepresentation(Object)
     */
    static <P, M> Converter<P, M> nullSafeConverter(Converter<P, M> converter,
            P nullRepresentation) {
        Objects.requireNonNull(converter, "converter cannot be null");
        return new Converter<P, M>() {
            @Override
            public Result<M> convertToModel(P value, ValueContext context) {
                if (value == null
                        || Objects.equals(value, nullRepresentation)) {
                    return Result.ok(null);
                }
                return converter.convertToModel(value, context);
            }

            @Override
            public P convertToPresentation(M value, ValueContext context) {
                P presentation = value == null ? null
                        : converter.convertToPresentation(value, context);
                return presentation == null ? nullRepresentation : presentation;
            }
        };
    }
}

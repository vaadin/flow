package com.vaadin.elements.core.grid;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.data.util.converter.Converter;
import com.vaadin.ui.AbstractClientConnector;
import com.vaadin.ui.renderers.Renderer;

import elemental.json.Json;
import elemental.json.JsonValue;

/**
 * An abstract base class for server-side
 * {@link com.vaadin.ui.renderers.Renderer Grid renderers}. This class currently
 * extends the AbstractExtension superclass, but this fact should be regarded as
 * an implementation detail and subject to change in a future major or minor
 * Vaadin revision.
 *
 * @param <T>
 *            the type this renderer knows how to present
 */
public abstract class AbstractRenderer<T> extends AbstractGridExtension
        implements Renderer<T> {

    private final Class<T> presentationType;

    private final String nullRepresentation;

    protected AbstractRenderer(Class<T> presentationType,
            String nullRepresentation) {
        this.presentationType = presentationType;
        this.nullRepresentation = nullRepresentation;
    }

    protected AbstractRenderer(Class<T> presentationType) {
        this(presentationType, null);
    }

    /**
     * This method is inherited from AbstractExtension but should never be
     * called directly with an AbstractRenderer.
     */
    @Deprecated
    @Override
    protected void extend(AbstractClientConnector target) {
        super.extend(target);
    }

    @Override
    public Class<T> getPresentationType() {
        return presentationType;
    }

    @Override
    public JsonValue encode(T value) {
        if (value == null) {
            return encode(getNullRepresentation(), String.class);
        } else {
            return encode(value, getPresentationType());
        }
    }

    /**
     * Null representation for the renderer
     *
     * @return a textual representation of {@code null}
     */
    protected String getNullRepresentation() {
        return nullRepresentation;
    }

    /**
     * Encodes the given value to JSON.
     * <p>
     * This is a helper method that can be invoked by an {@link #encode(Object)
     * encode(T)} override if serializing a value of type other than
     * {@link #getPresentationType() the presentation type} is desired. For
     * instance, a {@code Renderer<Date>} could first turn a date value into a
     * formatted string and return {@code encode(dateString, String.class)}.
     *
     * @param value
     *            the value to be encoded
     * @param type
     *            the type of the value
     * @return a JSON representation of the given value
     */
    protected <U> JsonValue encode(U value, Class<U> type) {
        return Json.createNull();
        // return JsonCodec
        // .encode(value, null, type, getUI().getConnectorTracker())
        // .getEncodedValue();
    }

    /**
     * Converts and encodes the given data model property value using the given
     * converter and renderer. This method is public only for testing purposes.
     *
     * @param renderer
     *            the renderer to use
     * @param converter
     *            the converter to use
     * @param modelValue
     *            the value to convert and encode
     * @param locale
     *            the locale to use in conversion
     * @return an encoded value ready to be sent to the client
     */
    public static <T> JsonValue encodeValue(Object modelValue,
            Renderer<T> renderer, Converter<?, ?> converter, Locale locale) {
        Class<T> presentationType = renderer.getPresentationType();
        T presentationValue;

        if (converter == null) {
            try {
                presentationValue = presentationType.cast(modelValue);
            } catch (ClassCastException e) {
                if (presentationType == String.class) {
                    // If there is no converter, just fallback to using
                    // toString(). modelValue can't be null as
                    // Class.cast(null) will always succeed
                    presentationValue = (T) modelValue.toString();
                } else {
                    throw new Converter.ConversionException(
                            "Unable to convert value of type "
                                    + modelValue.getClass().getName()
                                    + " to presentation type "
                                    + presentationType.getName()
                                    + ". No converter is set and the types are not compatible.");
                }
            }
        } else {
            assert presentationType
                    .isAssignableFrom(converter.getPresentationType());
            @SuppressWarnings("unchecked")
            Converter<T, Object> safeConverter = (Converter<T, Object>) converter;
            presentationValue = safeConverter.convertToPresentation(modelValue,
                    safeConverter.getPresentationType(), locale);
        }

        JsonValue encodedValue;
        try {
            encodedValue = renderer.encode(presentationValue);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Unable to encode data", e);
            encodedValue = renderer.encode(null);
        }

        return encodedValue;
    }

    private static Logger getLogger() {
        return Logger.getLogger(AbstractRenderer.class.getName());
    }

}
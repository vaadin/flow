/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.lang.reflect.Type;

import com.googlecode.gentyref.GenericTypeReflector;

/**
 * Template models encoder. Used for enabling the use of types in template model
 * methods that are not natively supported by the framework.
 *
 * @see Encode
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <D>
 *            the decoded type
 * @param <E>
 *            the encoded type
 */
public interface ModelEncoder<D, E extends Serializable> extends Serializable {

    /**
     * Get the decoded type of this encoder.
     *
     * @return the application type
     */
    @SuppressWarnings("unchecked")
    default Class<D> getDecodedType() {
        Type type = GenericTypeReflector.getTypeParameter(this.getClass(),
                ModelEncoder.class.getTypeParameters()[0]);
        if (type instanceof Class<?>) {
            return (Class<D>) GenericTypeReflector.erase(type);
        }
        throw new InvalidTemplateModelException(String.format(
                "Could not detect the model type of %s '%s'. "
                        + "The method 'getDecodedType' needs to be overridden manually.",
                ModelEncoder.class.getSimpleName(), this.getClass().getName()));
    }

    /**
     * Get the encoded type of this encoder.
     *
     * @return the model type
     */
    @SuppressWarnings("unchecked")
    default Class<E> getEncodedType() {
        Type type = GenericTypeReflector.getTypeParameter(this.getClass(),
                ModelEncoder.class.getTypeParameters()[1]);
        if (type instanceof Class<?>) {
            return (Class<E>) GenericTypeReflector.erase(type);
        }
        throw new InvalidTemplateModelException(String.format(
                "Could not detect the presentation type of %s '%s'. "
                        + "The method 'getEncodedType' needs to be overridden manually.",
                ModelEncoder.class.getSimpleName(), this.getClass().getName()));
    }

    /**
     * Encodes the given value.
     *
     * @param value
     *            the value to encode
     * @return the encoded model value
     */
    E encode(D value);

    /**
     * Decodes the given value.
     *
     * @param value
     *            the value to decode
     * @return the decoded value
     */
    D decode(E value);
}

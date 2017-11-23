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
package com.vaadin.flow.model;

import java.io.Serializable;
import java.lang.reflect.Type;

import com.googlecode.gentyref.GenericTypeReflector;

/**
 * Interface for implementing type conversions in template models together with
 * the {@link Convert} annotation. Used for enabling the use of types in
 * template model methods that are not natively supported by the framework.
 *
 * @see Convert
 *
 * @author Vaadin Ltd
 *
 * @param <M>
 *            the model type
 * @param <P>
 *            the presentation type
 */
public interface ModelConverter<M, P extends Serializable>
        extends Serializable {

    /**
     * Get the model type of this converter.
     *
     * @return the application type
     */
    @SuppressWarnings("unchecked")
    default Class<M> getModelType() {
        Type type = GenericTypeReflector.getTypeParameter(this.getClass(),
                ModelConverter.class.getTypeParameters()[0]);
        if (type instanceof Class<?>) {
            return (Class<M>) GenericTypeReflector.erase(type);
        }
        throw new InvalidTemplateModelException(String.format(
                "Could not detect the model type of ModelConverter '%s'. "
                        + "The method getModelType needs to be overridden manually.",
                this.getClass().getName()));
    }

    /**
     * Get the presentation type of this converter.
     *
     * @return the model type
     */
    @SuppressWarnings("unchecked")
    default Class<P> getPresentationType() {
        Type type = GenericTypeReflector.getTypeParameter(this.getClass(),
                ModelConverter.class.getTypeParameters()[1]);
        if (type instanceof Class<?>) {
            return (Class<P>) GenericTypeReflector.erase(type);
        }
        throw new InvalidTemplateModelException(String.format(
                "Could not detect the presentation type of ModelConverter '%s'. "
                        + "The method getPresentationType needs to be overridden manually.",
                this.getClass().getName()));
    }

    /**
     * Converts the given value from model type to presentation type.
     *
     * @param modelValue
     *            the value to convert
     * @return the converted value
     */
    P toPresentation(M modelValue);

    /**
     * Converts the given value from presentation type to model type.
     *
     * @param presentationValue
     *            the value to convert
     * @return the converted value
     */
    M toModel(P presentationValue);
}

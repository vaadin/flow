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
package com.vaadin.flow.template.model;

import java.io.Serializable;
import java.lang.reflect.Type;

import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.annotations.Convert;

/**
 * Interface for implementing type conversions in template models together with
 * the {@link Convert} annotation. Used for enabling the use of types in
 * template model methods that are not natively supported by the framework.
 * 
 * @see Convert
 * 
 * @author Vaadin Ltd
 *
 * @param <A>
 *            the application code type
 * @param <M>
 *            the type after conversion
 */
public interface ModelConverter<A, M extends Serializable>
        extends Serializable {

    /**
     * Get the application type of this converter.
     * 
     * @return the application type
     */
    @SuppressWarnings("unchecked")
    default Class<A> getApplicationType() {
        Type type = GenericTypeReflector.getTypeParameter(this.getClass(),
                ModelConverter.class.getTypeParameters()[0]);
        if (type instanceof Class<?>) {
            return (Class<A>) GenericTypeReflector.erase(type);
        }
        throw new InvalidTemplateModelException(String.format(
                "Could not detect the application type of ModelConverter '%s'. "
                        + "The method getApplicationType needs to be overridden manually.",
                this.getClass().getName()));
    }

    /**
     * Get the model type of this converter.
     * 
     * @return the model type
     */
    @SuppressWarnings("unchecked")
    default Class<M> getModelType() {
        Type type = GenericTypeReflector.getTypeParameter(this.getClass(),
                ModelConverter.class.getTypeParameters()[1]);
        if (type instanceof Class<?>) {
            return (Class<M>) GenericTypeReflector.erase(type);
        }
        throw new InvalidTemplateModelException(String.format(
                "Could not detect the model type of ModelConverter '%s'. "
                        + "The method getModelType needs to be overridden manually.",
                this.getClass().getName()));
    }

    /**
     * Converts the given value from application type to the model type.
     * 
     * @param applicationValue
     *            the value to convert
     * @return the converted value
     */
    M toModel(A applicationValue);

    /**
     * Converts the given value from model type to application type.
     * 
     * @param modelValue
     *            the value to convert
     * @return the converted value
     */
    A toApplication(M modelValue);
}

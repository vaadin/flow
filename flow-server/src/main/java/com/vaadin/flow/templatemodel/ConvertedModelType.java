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
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.lang.reflect.Type;

import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;

import elemental.json.JsonValue;

/**
 * A {@link ModelType} implementation that wraps a model type for performing
 * type conversions on together with a {@link ModelEncoder}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <A>
 *            application type of the converter used by this class
 * @param <M>
 *            model type of the converter used by this class
 */
public class ConvertedModelType<A, M extends Serializable>
        implements ModelType {

    private final ModelType wrappedModelType;
    private final ModelEncoder<A, M> converter;

    /**
     * Creates a new ConvertedModelType from the given model type and converter.
     *
     * @param modelType
     *            the model type to wrap
     * @param converter
     *            the converter to use
     */
    ConvertedModelType(ModelType modelType, ModelEncoder<A, M> converter) {
        wrappedModelType = modelType;
        this.converter = converter;
    }

    @Override
    public Object modelToApplication(Serializable modelValue) {
        @SuppressWarnings("unchecked")
        M wrappedApplicationValue = (M) wrappedModelType
                .modelToApplication(modelValue);
        return converter.decode(wrappedApplicationValue);
    }

    @Override
    public Serializable applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        @SuppressWarnings("unchecked")
        M convertedValue = converter.encode((A) applicationValue);
        return wrappedModelType.applicationToModel(convertedValue, filter);
    }

    @Override
    public boolean accepts(Type applicationType) {
        return ReflectTools.convertPrimitiveType(converter.getDecodedType())
                .isAssignableFrom(ReflectTools
                        .convertPrimitiveType((Class<?>) applicationType));
    }

    @Override
    public Type getJavaType() {
        return converter.getDecodedType();
    }

    @Override
    public JsonValue toJson() {
        return wrappedModelType.toJson();
    }

    @Override
    public void createInitialValue(StateNode node, String property) {
        wrappedModelType.createInitialValue(node, property);
    }

    /**
     * Gets the model type describing the data actually stored in the model.
     *
     * @return the wrapped model type
     */
    public ModelType getWrappedModelType() {
        return wrappedModelType;
    }
}

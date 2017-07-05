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

import com.vaadin.util.ReflectTools;

/**
 * A model type representing an immutable primitive value (e.g. strings, numbers
 * or booleans), which has a {@link ModelConverter} for applying an additional
 * type conversion between the model and application.
 * 
 * @see ModelConverter
 * 
 * @author Vaadin Ltd
 *
 * @param <A>
 *            the Java type used by this model
 * @param <M>
 *            the type after conversion has been applied
 */
public class ConvertedBasicModelType<A, M extends Serializable>
        extends AbstractBasicModelType<A> {

    private final ModelConverter<A, M> converter;

    protected ConvertedBasicModelType(ModelConverter<A, M> converter) {
        super(converter.getApplicationType());
        this.converter = converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object modelToApplication(Serializable modelValue) {
        if (modelValue == null && getJavaType().isPrimitive()) {
            return ReflectTools.getPrimitiveDefaultValue(getJavaType());
        } else {
            return converter.toApplication((M) modelValue);
        }
    }

    @Override
    public Object modelToNashorn(Serializable modelValue) {
        throw new UnsupportedOperationException("Obsolete functionality");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Serializable applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        return converter.toModel((A) applicationValue);
    }
}

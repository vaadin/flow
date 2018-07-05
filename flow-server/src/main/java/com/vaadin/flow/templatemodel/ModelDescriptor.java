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

import com.vaadin.flow.internal.ReflectionCache;

/**
 * Describes the model type of a template class.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the template model type used by this descriptor
 */
public class ModelDescriptor<T extends TemplateModel> extends BeanModelType<T> {
    private static ReflectionCache<TemplateModel, ModelDescriptor<?>> classToDescriptor = new ReflectionCache<>(
            ModelDescriptor::new);

    private ModelDescriptor(Class<T> beanType) {
        super(beanType, PropertyFilter.ACCEPT_ALL, true);
    }

    /**
     * Gets the model descriptor for a model type.
     *
     * @param <T>
     *            the model type
     * @param modelType
     *            the model type to find a descriptor, not <code>null</code>
     * @return the model descriptor derived from the provided model type, not
     *         <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public static <T extends TemplateModel> ModelDescriptor<T> get(
            Class<T> modelType) {
        assert modelType != null;

        return (ModelDescriptor<T>) classToDescriptor.get(modelType);
    }
}

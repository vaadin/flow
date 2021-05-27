/*
 * Copyright 2000-2021 Vaadin Ltd.
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
import java.util.Map;
import java.util.Optional;

/**
 * A model type representing an immutable leaf value, e.g. strings, numbers or
 * booleans.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * 
 * @deprecated Template model and model types are not supported for lit
 *             template, but you can use {@code @Id} mapping and the component
 *             API or the element API with property synchronization instead.
 *             Polymer template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public class BasicModelType extends AbstractBasicModelType {

    static final Map<Class<?>, BasicModelType> TYPES = loadBasicTypes(
            BasicModelType::new);

    private BasicModelType(Class<?> type) {
        super(type);
    }

    /**
     * Gets the basic model type definition for the given Java class.
     *
     * @param type
     *            the Java class to find a basic model type for
     * @return the basic model type, or an empty optional if the provided type
     *         is not a basic type
     */
    public static Optional<ModelType> get(Class<?> type) {
        return Optional.ofNullable(TYPES.get(type));
    }

    @Override
    public Object modelToApplication(Serializable modelValue) {
        return convertToApplication(modelValue);
    }

    @Override
    public Serializable applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        return (Serializable) applicationValue;
    }

}

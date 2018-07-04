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
package com.vaadin.flow.data.binder;

import java.lang.annotation.Annotation;
import java.util.Objects;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.function.SerializableBiPredicate;

/**
 * This interface represents a predicate which returns {@code true} if bound
 * field should be configured to have required indicator via
 * {@link HasValue#setRequiredIndicatorVisible(boolean)}.
 *
 * @see BeanValidationBinder
 * @see BeanValidationBinder#setRequiredConfigurator(RequiredFieldConfigurator)
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface RequiredFieldConfigurator
        extends SerializableBiPredicate<Annotation, BindingBuilder<?, ?>> {

    /**
     * Configurator which is aware of {@literal @NotNull} annotation presence
     * for a property where the default value is <code>null</code>.
     */
    RequiredFieldConfigurator NOT_NULL = (annotation,
            binding) -> annotation.annotationType().equals(NotNull.class)
                    && RequiredFieldConfiguratorUtil.testConvertedDefaultValue(
                            binding, Objects::isNull);

    /**
     * Configurator which is aware of {@literal @NotEmpty} annotation presence
     * for a property where the default value is empty.
     */
    RequiredFieldConfigurator NOT_EMPTY = (annotation,
            binding) -> (annotation.annotationType().equals(NotEmpty.class)
                    || annotation.annotationType().getName()
                            .equals("org.hibernate.validator.constraints.NotEmpty"))
                    && RequiredFieldConfiguratorUtil.testConvertedDefaultValue(
                            binding,
                            RequiredFieldConfiguratorUtil::hasZeroSize);

    /**
     * Configurator which is aware of {@literal Size} annotation with
     * {@code min()> 0} presence for a property where the size of the default
     * value is 0.
     */
    RequiredFieldConfigurator SIZE = (annotation,
            binding) -> annotation.annotationType().equals(Size.class)
                    && ((Size) annotation).min() > 0
                    && RequiredFieldConfiguratorUtil.testConvertedDefaultValue(
                            binding,
                            RequiredFieldConfiguratorUtil::hasZeroSize);

    /**
     * Default configurator which is combination of {@link #NOT_NULL},
     * {@link #NOT_EMPTY} and {@link #SIZE} configurators.
     */
    RequiredFieldConfigurator DEFAULT = NOT_NULL.chain(NOT_EMPTY).chain(SIZE);

    /**
     * Returns a configurator that chains together this configurator with the
     * given configurator.
     *
     * @param configurator
     *            the configurator to chain, , not null
     * @return a chained configurator
     */
    default RequiredFieldConfigurator chain(
            RequiredFieldConfigurator configurator) {
        return (annotation, binding) -> test(annotation, binding)
                || configurator.test(annotation, binding);
    }
}

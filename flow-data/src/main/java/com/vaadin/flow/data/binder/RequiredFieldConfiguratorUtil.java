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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

import javax.validation.constraints.Size;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.Binder.BindingBuilderImpl;
import com.vaadin.flow.data.binder.Binder.BindingImpl;
import com.vaadin.flow.data.converter.Converter;

/**
 * Helper methods used by {@link RequiredFieldConfigurator}. The methods are
 * extracted to a separate class to prevent populating the public API of the
 * {@link RequiredFieldConfigurator} interface.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class RequiredFieldConfiguratorUtil {
    private RequiredFieldConfiguratorUtil() {
        // Only static helpers
    }

    /**
     * Checks whether the given object would be considered empty according to
     * the {@link Size} constraint.
     *
     * @param value
     *            the value to check
     * @return true if the value is supported by the size constraint, otherwise
     *         <code>false</code>
     */
    public static boolean hasZeroSize(Object value) {
        if (value instanceof CharSequence) {
            return ((CharSequence) value).length() == 0;
        }
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).isEmpty();
        }
        if (value != null && value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }

        return false;
    }

    /**
     * Tests the converted default value of the provided binding builder if
     * possible.
     *
     * @param binding
     *            the binding builder to test
     * @param predicate
     *            predicate for testing the converted default value
     * @return <code>true</code> if a converted default value is available and
     *         it passes the test; <code>false</code> if no converted default
     *         value is available or if it doesn't pass the test
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static boolean testConvertedDefaultValue(
            BindingBuilder<?, ?> binding, Predicate<Object> predicate) {
        if (binding instanceof BindingBuilderImpl<?, ?, ?>) {
            HasValue<?, ?> field = binding.getField();
            Converter converter = ((BindingBuilderImpl<?, ?, ?>) binding)
                    .getConverterValidatorChain();

            Result<?> result = converter.convertToModel(field.getEmptyValue(),
                    BindingImpl.createValueContext(field));

            if (!result.isError()) {
                Object convertedEmptyValue = result
                        .getOrThrow(IllegalStateException::new);
                return predicate.test(convertedEmptyValue);
            }
        }
        return false;
    }

}

/*
 * Copyright 2000-2022 Vaadin Ltd.
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

package com.vaadin.flow.data.validator;

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.internal.BeanUtil;

/**
 * TODO: Abstract class supporting constraint based validation
 *
 * @param <C>
 *            type of the constraint violation
 */
public abstract class AbstractBeanValidator<C> implements Validator<Object> {

    private final String propertyName;
    private final Class<?> beanType;

    /**
     * Creates a new JSR-303 {@code BeanValidator} that validates values of the
     * specified property. Localizes validation messages using the
     * {@linkplain Locale#getDefault() default locale}.
     *
     * @param beanType
     *            the bean type declaring the property, not null
     * @param propertyName
     *            the property to validate, not null
     * @throws IllegalStateException
     *             if {@link BeanUtil#checkBeanValidationAvailable()} returns
     *             false
     */
    protected AbstractBeanValidator(Class<?> beanType, String propertyName) {
        Objects.requireNonNull(beanType, "bean class cannot be null");
        Objects.requireNonNull(propertyName, "property name cannot be null");

        this.beanType = beanType;
        this.propertyName = propertyName;
    }

    /**
     * Validates the given value as if it were the value of the bean property
     * configured for this validator. Returns {@code Result.ok} if there are no
     * JSR-303 constraint violations, a {@code Result.error} of chained
     * constraint violation messages otherwise.
     * <p>
     * Null values are accepted unless the property has an {@code @NotNull}
     * annotation or equivalent.
     *
     * @param value
     *            the input value to validate
     * @param context
     *            the value context for validation
     * @return the validation result
     */
    @Override
    public ValidationResult apply(final Object value, ValueContext context) {
        Locale locale = context.getLocale().orElse(Locale.getDefault());

        Optional<ValidationResult> result = applyValidations(beanType,
                propertyName, value)
                        .stream()
                        .map(violation -> ValidationResult
                                .error(getMessage(violation, locale)))
                        .findFirst();
        return result.orElse(ValidationResult.ok());
    }

    public abstract Stream<Annotation> extractConstraints(Class<?> beanType,
            String propertyName);

    @Override
    public String toString() {
        return String.format("%s[%s.%s]", getClass().getSimpleName(),
                beanType.getSimpleName(), propertyName);
    }

    /**
     * Validates the given value as if it were the value of the bean property
     * configured for this validator. Returns all constraint violations, or an
     * empty set if there are no violations.
     *
     * <p>
     * Null values are accepted unless the property has an {@code @NotNull}
     * annotation or equivalent.
     *
     * @param beanType
     * @param propertyName
     * @param value
     *            the input value to validate
     * @return constraint violations or an empty set, never {@literal null}.
     */
    protected abstract Set<? extends C> applyValidations(Class<?> beanType,
            String propertyName, Object value);

    /**
     * Returns the interpolated error message for the given constraint violation
     * using the locale specified for this validator.
     *
     * @param violation
     *            the constraint violation
     * @param locale
     *            the used locale
     * @return the localized error message
     */
    protected abstract String getMessage(C violation, Locale locale);

}

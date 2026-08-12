/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.MessageInterpolator.Context;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;
import jakarta.validation.metadata.ConstraintDescriptor;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.BeanUtil;

/**
 * A {@code Validator} using the JSR-303 (jakarta.validation) annotation-based
 * bean validation mechanism. Values passed to this validator are compared
 * against the constraints, if any, specified by annotations on the
 * corresponding bean property.
 * <p>
 * Note that a JSR-303 implementation (for instance
 * <a href="http://hibernate.org/validator/">Hibernate Validator</a> or
 * <a href="http://bval.apache.org/">Apache BVal</a>) must be present on the
 * project classpath when using bean validation. Specification versions 1.0 and
 * 1.1 are supported.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 */
public class BeanValidator implements Validator<Object> {

    private static final class ContextImpl implements Context, Serializable {

        private final ConstraintViolation<?> violation;

        private ContextImpl(ConstraintViolation<?> violation) {
            this.violation = violation;
        }

        @Override
        public ConstraintDescriptor<?> getConstraintDescriptor() {
            return violation.getConstraintDescriptor();
        }

        @Override
        public Object getValidatedValue() {
            return violation.getInvalidValue();
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            return violation.unwrap(type);
        }

    }

    private static final Class<?>[] NO_GROUPS = new Class<?>[0];

    private String propertyName;
    private Class<?> beanType;
    private SerializableSupplier<Class<?>[]> validationGroupsSupplier;

    /**
     * Creates a new JSR-303 {@code BeanValidator} that validates values of the
     * specified property against the constraints of the {@linkplain Default
     * default validation group}. Localizes validation messages using the
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
    public BeanValidator(Class<?> beanType, String propertyName) {
        this(beanType, propertyName, NO_GROUPS);
    }

    /**
     * Creates a new JSR-303 {@code BeanValidator} that validates values of the
     * specified property against the constraints of the given validation
     * groups. Localizes validation messages using the
     * {@linkplain Locale#getDefault() default locale}.
     * <p>
     * Note that the validation groups replace, rather than extend, the
     * {@linkplain Default default group}: pass {@code Default.class} explicitly
     * to validate the constraints that do not declare a group as well.
     *
     * @param beanType
     *            the bean type declaring the property, not null
     * @param propertyName
     *            the property to validate, not null
     * @param validationGroups
     *            the validation groups to validate against, or none to use the
     *            {@linkplain Default default group}
     * @throws IllegalStateException
     *             if {@link BeanUtil#checkBeanValidationAvailable()} returns
     *             false
     * @since 25.3
     */
    public BeanValidator(Class<?> beanType, String propertyName,
            Class<?>... validationGroups) {
        this(beanType, propertyName, groupsSupplier(validationGroups));
    }

    /**
     * Creates a new JSR-303 {@code BeanValidator} that validates values of the
     * specified property against the constraints of the validation groups
     * provided by the given supplier. The supplier is queried on every
     * validation, which allows the validation groups to be changed after the
     * validator has been created.
     *
     * @param beanType
     *            the bean type declaring the property, not null
     * @param propertyName
     *            the property to validate, not null
     * @param validationGroupsSupplier
     *            supplier of the validation groups to validate against, not
     *            null
     * @throws IllegalStateException
     *             if {@link BeanUtil#checkBeanValidationAvailable()} returns
     *             false
     * @since 25.3
     */
    public BeanValidator(Class<?> beanType, String propertyName,
            SerializableSupplier<Class<?>[]> validationGroupsSupplier) {
        if (!BeanUtil.checkBeanValidationAvailable()) {
            throw new IllegalStateException("Cannot create a "
                    + BeanValidator.class.getSimpleName()
                    + ": a JSR-303 Bean Validation implementation not found on the classpath");
        }
        Objects.requireNonNull(beanType, "bean class cannot be null");
        Objects.requireNonNull(propertyName, "property name cannot be null");
        Objects.requireNonNull(validationGroupsSupplier,
                "validation groups supplier cannot be null");

        this.beanType = beanType;
        this.propertyName = propertyName;
        this.validationGroupsSupplier = validationGroupsSupplier;
    }

    private static SerializableSupplier<Class<?>[]> groupsSupplier(
            Class<?>[] validationGroups) {
        Class<?>[] groups = validationGroups == null ? NO_GROUPS
                : validationGroups.clone();
        return () -> groups;
    }

    /**
     * Gets the validation groups this validator validates against. An empty
     * array means that the {@linkplain Default default group} is used.
     *
     * @return the validation groups, not null
     * @since 25.3
     */
    public Class<?>[] getValidationGroups() {
        return validationGroups().clone();
    }

    private Class<?>[] validationGroups() {
        Class<?>[] groups = validationGroupsSupplier.get();
        return groups == null ? NO_GROUPS : groups;
    }

    /**
     * Validates the given value as if it were the value of the bean property
     * configured for this validator. Returns {@code Result.ok} if there are no
     * JSR-303 constraint violations, a {@code Result.error} of chained
     * constraint violation messages otherwise.
     * <p>
     * Null values are accepted unless the property has an {@code @NotNull}
     * annotation or equivalent.
     * <p>
     * Only the constraints belonging to the {@linkplain #getValidationGroups()
     * validation groups} of this validator are taken into account.
     *
     * @param value
     *            the input value to validate
     * @param context
     *            the value context for validation
     * @return the validation result
     */
    @Override
    public ValidationResult apply(final Object value, ValueContext context) {
        Set<? extends ConstraintViolation<?>> violations = getJavaxBeanValidator()
                .validateValue(beanType, propertyName, value,
                        validationGroups());

        Locale locale = context.getLocale().orElse(Locale.getDefault());

        Optional<ValidationResult> result = violations.stream()
                .map(violation -> ValidationResult
                        .error(getMessage(violation, locale)))
                .findFirst();
        return result.orElse(ValidationResult.ok());
    }

    @Override
    public String toString() {
        return String.format("%s[%s.%s]", getClass().getSimpleName(),
                beanType.getSimpleName(), propertyName);
    }

    /**
     * Returns the underlying JSR-303 bean validator factory used. A factory is
     * created using {@link Validation} if necessary.
     *
     * @return the validator factory to use
     */
    protected static ValidatorFactory getJavaxBeanValidatorFactory() {
        return LazyFactoryInitializer.FACTORY;
    }

    /**
     * Returns a shared JSR-303 validator instance to use.
     *
     * @return the validator to use
     */
    public jakarta.validation.Validator getJavaxBeanValidator() {
        return getJavaxBeanValidatorFactory().getValidator();
    }

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
    protected String getMessage(ConstraintViolation<?> violation,
            Locale locale) {
        return getJavaxBeanValidatorFactory().getMessageInterpolator()
                .interpolate(violation.getMessageTemplate(),
                        createContext(violation), locale);
    }

    /**
     * Creates a simple message interpolation context based on the given
     * constraint violation.
     *
     * @param violation
     *            the constraint violation
     * @return the message interpolation context
     */
    protected Context createContext(ConstraintViolation<?> violation) {
        return new ContextImpl(violation);
    }

    private static class LazyFactoryInitializer implements Serializable {
        private static final ValidatorFactory FACTORY = getFactory();

        private LazyFactoryInitializer() {
        }

        private static ValidatorFactory getFactory() {
            return Validation.buildDefaultValidatorFactory();
        }
    }
}

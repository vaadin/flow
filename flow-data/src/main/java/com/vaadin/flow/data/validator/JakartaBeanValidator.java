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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;

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
 * project classpath when using bean validation. Specification versions 3.0 are
 * supported.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public class JakartaBeanValidator
        extends AbstractBeanValidator<ConstraintViolation<?>> {

    private static final class ContextImpl
            implements MessageInterpolator.Context, Serializable {

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
    public JakartaBeanValidator(Class<?> beanType, String propertyName) {
        super(beanType, propertyName);
        if (!JEEValidationHelper.JAKARTA_BEAN_VALIDATION_AVAILABLE) {
            throw new IllegalStateException("Cannot create a "
                    + JakartaBeanValidator.class.getSimpleName()
                    + ": a JSR-303 Bean Validation implementation not found on the classpath");
        }
    }

    @Override
    public Stream<Annotation> extractConstraints(Class<?> propertyHolderType,
            String propertyName) {
        BeanDescriptor descriptor = getJavaxBeanValidator()
                .getConstraintsForClass(propertyHolderType);
        PropertyDescriptor propertyDescriptor = descriptor
                .getConstraintsForProperty(propertyName);
        if (propertyDescriptor == null) {
            return Stream.empty();
        }
        return propertyDescriptor.getConstraintDescriptors().stream()
                .map(ConstraintDescriptor::getAnnotation);
    }

    @Override
    protected Set<? extends ConstraintViolation<?>> applyValidations(
            Class<?> beanType, String propertyName, Object value) {
        return getJavaxBeanValidator().validateValue(beanType, propertyName,
                value);
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
    protected MessageInterpolator.Context createContext(
            ConstraintViolation<?> violation) {
        return new ContextImpl(violation);
    }

    private static final class LazyFactoryInitializer implements Serializable {
        private static final ValidatorFactory FACTORY = getFactory();

        private LazyFactoryInitializer() {
        }

        private static ValidatorFactory getFactory() {
            return Validation.buildDefaultValidatorFactory();
        }
    }

}

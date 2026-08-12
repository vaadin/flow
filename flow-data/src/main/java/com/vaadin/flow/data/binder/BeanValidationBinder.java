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
package com.vaadin.flow.data.binder;

import jakarta.validation.groups.Default;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;

import java.util.stream.Stream;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.BeanPropertySet.NestedBeanPropertyDefinition;
import com.vaadin.flow.data.validator.BeanValidator;
import com.vaadin.flow.internal.BeanUtil;

/**
 * Binder that uses reflection based on the provided bean type to resolve bean
 * properties. The Binder automatically adds BeanValidator which validates beans
 * using JSR-303 specification. It assumes that JSR-303 bean validation
 * implementation is present on the classpath.
 * <p>
 * By default only the constraints of the {@linkplain Default default validation
 * group} are validated. Use {@link #setValidationGroups(Class...)} to configure
 * the validation groups to use instead, and {@link #validate(Class...)
 * validate(Class&lt;?&gt;...)} to run a single validation against other
 * validation groups without changing the configured ones.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @see Binder
 * @see BeanValidator
 * @see HasValue
 *
 * @param <BEAN>
 *            the bean type
 */
public class BeanValidationBinder<BEAN> extends Binder<BEAN> {

    private static final Class<?>[] NO_GROUPS = new Class<?>[0];

    private final Class<BEAN> beanType;

    private RequiredFieldConfigurator requiredConfigurator = RequiredFieldConfigurator.DEFAULT;

    private Class<?>[] validationGroups = NO_GROUPS;

    private Class<?>[] oneShotValidationGroups;

    /**
     * Creates a new binder that uses reflection based on the provided bean type
     * to resolve bean properties. It assumes that JSR-303 bean validation
     * implementation is present on the classpath. If there is no such
     * implementation available then {@link Binder} class should be used instead
     * (this constructor will throw an exception). Otherwise
     * {@link BeanValidator} is added to each binding that is defined using a
     * property name.
     *
     * @param beanType
     *            the bean type to use, not <code>null</code>
     */
    public BeanValidationBinder(Class<BEAN> beanType) {
        this(beanType, false);
    }

    /**
     * Creates a new binder that uses reflection based on the provided bean type
     * to resolve bean properties. It assumes that JSR-303 bean validation
     * implementation is present on the classpath. If there is no such
     * implementation available then {@link Binder} class should be used instead
     * (this constructor will throw an exception). Otherwise
     * {@link BeanValidator} is added to each binding that is defined using a
     * property name.
     *
     * @param beanType
     *            the bean type to use, not {@code null}
     * @param scanNestedDefinitions
     *            if {@code true}, scan for nested property definitions as well
     * @since 2.2
     */
    public BeanValidationBinder(Class<BEAN> beanType,
            boolean scanNestedDefinitions) {
        super(beanType, scanNestedDefinitions);
        if (!BeanUtil.checkBeanValidationAvailable()) {
            throw new IllegalStateException(BeanValidationBinder.class
                    .getSimpleName()
                    + " cannot be used because a JSR-303 Bean Validation "
                    + "implementation not found on the classpath or could not be initialized. Use "
                    + Binder.class.getSimpleName() + " instead");
        }
        this.beanType = beanType;
    }

    /**
     * Creates a new binder that uses reflection based on the provided bean type
     * to resolve bean properties, and validates the constraints of the given
     * validation groups instead of the ones of the {@linkplain Default default
     * group}.
     * <p>
     * See {@link #setValidationGroups(Class...)} for details on how the
     * validation groups are used.
     *
     * @param beanType
     *            the bean type to use, not {@code null}
     * @param validationGroups
     *            the validation groups to validate against, or none to use the
     *            {@linkplain Default default group}
     * @since 25.3
     */
    public BeanValidationBinder(Class<BEAN> beanType,
            Class<?>... validationGroups) {
        this(beanType, false);
        setValidationGroups(validationGroups);
    }

    /**
     * Sets the validation groups whose constraints are validated by this
     * binder. Configuring the validation groups affects all validation
     * triggered by this binder, including the validation of a single field when
     * its value changes.
     * <p>
     * Note that the validation groups replace, rather than extend, the
     * {@linkplain Default default group}: pass {@code Default.class} explicitly
     * to validate the constraints that do not declare a group as well. Passing
     * no groups at all restores the default behavior of validating only the
     * default group.
     * <p>
     * The validation groups are also taken into account when configuring the
     * required indicator of a field, in which case only the groups that are
     * configured at the time the field is bound are used.
     *
     * @see #validate(Class...)
     *
     * @param validationGroups
     *            the validation groups to validate against, or none to use the
     *            {@linkplain Default default group}
     * @since 25.3
     */
    public void setValidationGroups(Class<?>... validationGroups) {
        this.validationGroups = validationGroups == null ? NO_GROUPS
                : validationGroups.clone();
    }

    /**
     * Gets the validation groups whose constraints are validated by this
     * binder. An empty array means that the {@linkplain Default default group}
     * is used.
     * <p>
     * While a validation triggered by {@link #validate(Class...)} is running,
     * the groups given to that method are returned instead of the configured
     * ones. Bean level validators added with {@link #withValidator(Validator)}
     * can use this to validate against the same groups as the field level
     * validation.
     *
     * @see #setValidationGroups(Class...)
     *
     * @return the validation groups in effect, not {@code null}
     * @since 25.3
     */
    public Class<?>[] getValidationGroups() {
        return oneShotValidationGroups == null ? validationGroups.clone()
                : oneShotValidationGroups.clone();
    }

    /**
     * Validates the values of all bound fields against the constraints of the
     * given validation groups and returns the validation status. The
     * {@linkplain #setValidationGroups(Class...) configured validation groups}
     * are left unchanged, i.e. any validation triggered later on, for instance
     * by a field value change, uses the configured groups again.
     * <p>
     * This can be used to run constraints that should not be validated while
     * the user is editing, such as constraints that are only relevant when the
     * data is saved:
     *
     * <pre>
     * saveButton.addClickListener(event -&gt; {
     *     if (binder.validate(Save.class).isOk()) {
     *         binder.writeBean(bean);
     *     }
     * });
     * </pre>
     * <p>
     * Calling this method without any validation groups is equivalent to
     * calling {@link #validate()}.
     *
     * @see #validate()
     * @see #setValidationGroups(Class...)
     *
     * @param validationGroups
     *            the validation groups to validate against
     * @return validation status for the binder
     * @since 25.3
     */
    public BinderValidationStatus<BEAN> validate(Class<?>... validationGroups) {
        if (validationGroups == null || validationGroups.length == 0) {
            return validate();
        }
        oneShotValidationGroups = validationGroups.clone();
        try {
            return validate();
        } finally {
            oneShotValidationGroups = null;
        }
    }

    /**
     * Sets a logic which allows to configure require indicator via
     * {@link HasValue#setRequiredIndicatorVisible(boolean)} based on property
     * descriptor.
     * <p>
     * Required indicator configuration will not be used at all if
     * {@code configurator} is null.
     * <p>
     * By default the {@link RequiredFieldConfigurator#DEFAULT} configurator is
     * used.
     *
     * @param configurator
     *            required indicator configurator, may be {@code null}
     */
    public void setRequiredConfigurator(
            RequiredFieldConfigurator configurator) {
        requiredConfigurator = configurator;
    }

    /**
     * Gets field required indicator configuration logic.
     *
     * @see #setRequiredConfigurator(RequiredFieldConfigurator)
     *
     * @return required indicator configurator, may be {@code null}
     */
    public RequiredFieldConfigurator getRequiredConfigurator() {
        return requiredConfigurator;
    }

    @Override
    protected BindingBuilder<BEAN, ?> configureBinding(
            BindingBuilder<BEAN, ?> binding,
            PropertyDefinition<BEAN, ?> definition) {
        Class<?> actualBeanType = findBeanType(beanType, definition);
        BeanValidator validator = new BeanValidator(actualBeanType,
                definition.getTopLevelName(), this::getValidationGroups);
        if (requiredConfigurator != null) {
            configureRequired(binding, definition, validator);
        }
        return binding.withValidator(validator);
    }

    /**
     * Finds the bean type containing the property the given definition refers
     * to.
     *
     * @param beanType
     *            the root beanType
     * @param definition
     *            the definition for the property
     * @return the bean type containing the given property
     */
    @SuppressWarnings({ "rawtypes" })
    private Class<?> findBeanType(Class<BEAN> beanType,
            PropertyDefinition<BEAN, ?> definition) {
        if (definition instanceof NestedBeanPropertyDefinition) {
            return ((NestedBeanPropertyDefinition) definition).getParent()
                    .getType();
        } else {
            // Non nested properties must be defined in the main type
            return beanType;
        }
    }

    private void configureRequired(BindingBuilder<BEAN, ?> binding,
            PropertyDefinition<BEAN, ?> definition, BeanValidator validator) {
        assert requiredConfigurator != null;
        Class<?> propertyHolderType = definition.getPropertyHolderType();
        BeanDescriptor descriptor = validator.getJavaxBeanValidator()
                .getConstraintsForClass(propertyHolderType);
        PropertyDescriptor propertyDescriptor = descriptor
                .getConstraintsForProperty(definition.getTopLevelName());
        if (propertyDescriptor == null) {
            return;
        }
        if (propertyDescriptor.getConstraintDescriptors().stream()
                .filter(this::appliesToValidationGroups)
                .map(ConstraintDescriptor::getAnnotation)
                .anyMatch(constraint -> requiredConfigurator.test(constraint,
                        binding))) {
            binding.getField().setRequiredIndicatorVisible(true);
        }
    }

    /**
     * Checks whether the given constraint belongs to any of the validation
     * groups configured for this binder, i.e. whether it is validated at all.
     * <p>
     * A constraint applies to a validation group also when the group inherits
     * from one of the groups declared by the constraint.
     *
     * @param descriptor
     *            the descriptor of the constraint to check
     * @return {@code true} if the constraint is validated, {@code false}
     *         otherwise
     */
    private boolean appliesToValidationGroups(
            ConstraintDescriptor<?> descriptor) {
        Class<?>[] groups = validationGroups.length == 0
                ? new Class<?>[] { Default.class }
                : validationGroups;
        return descriptor.getGroups().stream().anyMatch(declaredGroup -> Stream
                .of(groups).anyMatch(declaredGroup::isAssignableFrom));
    }

}

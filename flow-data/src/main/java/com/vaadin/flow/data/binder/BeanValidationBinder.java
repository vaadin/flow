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

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
 * validate(Class&lt;?&gt;...)} or {@link #isValid(Class...)
 * isValid(Class&lt;?&gt;...)} to run a single validation against other
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

    private final Map<HasValue<?, ?>, RequiredConstraints> requiredConstraints = new HashMap<>();

    /**
     * The validation groups of the constraints that make a bound field
     * required, and the required indicator value that has been set based on
     * them.
     */
    private static class RequiredConstraints implements Serializable {

        private final Class<?> propertyHolderType;

        private final List<Set<Class<?>>> constraintGroups;

        private boolean indicatorVisible;

        private RequiredConstraints(Class<?> propertyHolderType,
                List<Set<Class<?>>> constraintGroups) {
            this.propertyHolderType = propertyHolderType;
            this.constraintGroups = constraintGroups;
        }

        private boolean appliesTo(Set<Class<?>> validationGroups) {
            return constraintGroups.stream().anyMatch(groups -> groups.stream()
                    .anyMatch(declaredGroup -> validationGroups.stream()
                            .anyMatch(declaredGroup::isAssignableFrom)));
        }
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
     */
    public BeanValidationBinder(Class<BEAN> beanType,
            Class<?>... validationGroups) {
        this(beanType, false, validationGroups);
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
     * @param scanNestedDefinitions
     *            if {@code true}, scan for nested property definitions as well
     * @param validationGroups
     *            the validation groups to validate against, or none to use the
     *            {@linkplain Default default group}
     */
    public BeanValidationBinder(Class<BEAN> beanType,
            boolean scanNestedDefinitions, Class<?>... validationGroups) {
        this(beanType, scanNestedDefinitions);
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
     * The required indicators of the already bound fields are updated to match
     * the new validation groups, unless the indicator has been changed by the
     * application after the field was bound. Validation results that are
     * already shown are not updated, i.e. the application should call
     * {@link #validate()} after changing the validation groups if the fields
     * have already been validated against the previous groups.
     *
     * @see #validate(Class...)
     * @see #isValid(Class...)
     *
     * @param validationGroups
     *            the validation groups to validate against, or none to use the
     *            {@linkplain Default default group}
     * @throws IllegalArgumentException
     *             if any of the given validation groups is not an interface
     */
    public void setValidationGroups(Class<?>... validationGroups) {
        this.validationGroups = copyValidationGroups(validationGroups);
        updateRequiredIndicators();
    }

    /**
     * Gets the validation groups whose constraints are validated by this
     * binder. An empty array means that the {@linkplain Default default group}
     * is used.
     * <p>
     * While a validation triggered by {@link #validate(Class...)} or
     * {@link #isValid(Class...)} is running, the groups given to that method
     * are returned instead of the configured ones. Bean level validators added
     * with {@link #withValidator(Validator)} can use this to validate against
     * the same groups as the field level validation.
     *
     * @see #setValidationGroups(Class...)
     *
     * @return the validation groups in effect, not {@code null}
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
     * calling {@link #validate()}. Use {@link #isValid(Class...)} to validate
     * without showing the validation results to the user.
     *
     * @see #validate()
     * @see #isValid(Class...)
     * @see #setValidationGroups(Class...)
     *
     * @param validationGroups
     *            the validation groups to validate against
     * @return validation status for the binder
     * @throws IllegalArgumentException
     *             if any of the given validation groups is not an interface
     */
    public BinderValidationStatus<BEAN> validate(Class<?>... validationGroups) {
        return validate(true, validationGroups);
    }

    /**
     * Runs all currently configured field level validators, as well as all bean
     * level validators if a bean is currently set with
     * {@link #setBean(Object)}, against the constraints of the given validation
     * groups, and returns whether any of the validators failed. The
     * {@linkplain #setValidationGroups(Class...) configured validation groups}
     * are left unchanged.
     * <p>
     * Unlike {@link #validate(Class...)}, this method does not trigger status
     * change events and does not modify the UI, which makes it suitable for
     * example for enabling and disabling a save button.
     * <p>
     * Calling this method without any validation groups is equivalent to
     * calling {@link #isValid()}.
     *
     * @see #isValid()
     * @see #validate(Class...)
     *
     * @param validationGroups
     *            the validation groups to validate against
     * @return whether this binder is in a valid state
     * @throws IllegalArgumentException
     *             if any of the given validation groups is not an interface
     */
    public boolean isValid(Class<?>... validationGroups) {
        return validate(false, validationGroups).isOk();
    }

    /**
     * Validates the values of all bound fields against the constraints of the
     * given validation groups and returns the validation status. This method
     * can skip firing the event, based on the given {@code boolean}.
     *
     * @see #validate(Class...)
     * @see #isValid(Class...)
     *
     * @param fireEvent
     *            {@code true} to fire validation status events; {@code false}
     *            to not
     * @param validationGroups
     *            the validation groups to validate against
     * @return validation status for the binder
     * @throws IllegalArgumentException
     *             if any of the given validation groups is not an interface
     */
    protected BinderValidationStatus<BEAN> validate(boolean fireEvent,
            Class<?>... validationGroups) {
        Class<?>[] groups = copyValidationGroups(validationGroups);
        if (groups.length == 0) {
            return validate(fireEvent);
        }
        Class<?>[] previousGroups = oneShotValidationGroups;
        oneShotValidationGroups = groups;
        try {
            return validate(fireEvent);
        } finally {
            // Restored instead of cleared so that a validation that is
            // triggered from a listener of this validation does not affect the
            // groups used by the rest of this validation
            oneShotValidationGroups = previousGroups;
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
        List<Set<Class<?>>> constraintGroups = propertyDescriptor
                .getConstraintDescriptors().stream()
                .filter(constraintDescriptor -> requiredConfigurator
                        .test(constraintDescriptor.getAnnotation(), binding))
                .map(constraintDescriptor -> (Set<Class<?>>) new LinkedHashSet<Class<?>>(
                        constraintDescriptor.getGroups()))
                .collect(Collectors.toList());
        if (constraintGroups.isEmpty()) {
            return;
        }
        RequiredConstraints constraints = new RequiredConstraints(
                propertyHolderType, constraintGroups);
        requiredConstraints.put(binding.getField(), constraints);
        if (constraints
                .appliesTo(getExpandedValidationGroups(propertyHolderType))) {
            constraints.indicatorVisible = true;
            binding.getField().setRequiredIndicatorVisible(true);
        }
    }

    @Override
    protected void removeBindingInternal(Binding<BEAN, ?> binding) {
        // The required indicator bookkeeping of a field is not needed after the
        // field has been unbound, and keeping it would also keep the field
        // itself from being garbage collected
        requiredConstraints.remove(binding.getField());
        super.removeBindingInternal(binding);
    }

    /**
     * Updates the required indicator of the bound fields to match the currently
     * configured validation groups.
     * <p>
     * A field whose required indicator has been changed after it was bound is
     * left alone, so that a required indicator set by the application is not
     * overridden.
     */
    private void updateRequiredIndicators() {
        if (requiredConstraints.isEmpty()) {
            return;
        }
        requiredConstraints.forEach((field, constraints) -> {
            boolean indicatorVisible = constraints
                    .appliesTo(getExpandedValidationGroups(
                            constraints.propertyHolderType));
            if (indicatorVisible != constraints.indicatorVisible && field
                    .isRequiredIndicatorVisible() == constraints.indicatorVisible) {
                field.setRequiredIndicatorVisible(indicatorVisible);
                constraints.indicatorVisible = indicatorVisible;
            }
        });
    }

    /**
     * Gets the validation groups that are taken into account when configuring
     * the required indicator of a bound field, i.e. the configured validation
     * groups together with the groups of the {@link GroupSequence} definitions
     * they refer to.
     * <p>
     * The {@link GroupSequence} of the type declaring the property is expanded
     * as well when the {@linkplain Default default group} is validated, since
     * that is how the default group of a type is redefined.
     * <p>
     * A constraint applies to one of these groups also when the group inherits
     * from a group declared by the constraint, which is why the groups are
     * compared with {@link Class#isAssignableFrom(Class)}.
     * <p>
     * Deliberately based on the configured validation groups instead of
     * {@link #getValidationGroups()}, which also reflects the groups of an
     * ongoing one-shot validation: the required indicator follows the groups
     * that are validated while the user is editing.
     *
     * @param propertyHolderType
     *            the type declaring the property of the bound field
     * @return the validation groups used for the required indicator
     */
    private Set<Class<?>> getExpandedValidationGroups(
            Class<?> propertyHolderType) {
        Set<Class<?>> expandedGroups = new LinkedHashSet<>();
        // Default.class is deliberately only referenced from inside a method:
        // the class must stay loadable when no Bean Validation implementation
        // is on the classpath, so that the constructor can throw a readable
        // exception instead of the class failing to initialize
        Deque<Class<?>> groupsToExpand = new ArrayDeque<>(Arrays.asList(
                validationGroups.length == 0 ? new Class<?>[] { Default.class }
                        : validationGroups));
        while (!groupsToExpand.isEmpty()) {
            Class<?> group = groupsToExpand.remove();
            if (!expandedGroups.add(group)) {
                // Already expanded, also guards against a cyclic sequence
                continue;
            }
            if (Default.class.equals(group)) {
                expandDefaultGroupRedefinition(propertyHolderType,
                        groupsToExpand);
            }
            GroupSequence groupSequence = group
                    .getAnnotation(GroupSequence.class);
            if (groupSequence != null) {
                Collections.addAll(groupsToExpand, groupSequence.value());
            }
        }
        return expandedGroups;
    }

    /**
     * Adds the groups of the {@link GroupSequence} of the given type, if any,
     * to the groups to expand. A group sequence on a type redefines the default
     * group of that type, so its constraints are validated whenever the default
     * group is.
     *
     * @param propertyHolderType
     *            the type declaring the property of the bound field
     * @param groupsToExpand
     *            the groups that are still to be expanded
     */
    private static void expandDefaultGroupRedefinition(
            Class<?> propertyHolderType, Deque<Class<?>> groupsToExpand) {
        GroupSequence defaultGroupRedefinition = propertyHolderType
                .getAnnotation(GroupSequence.class);
        if (defaultGroupRedefinition == null) {
            return;
        }
        for (Class<?> group : defaultGroupRedefinition.value()) {
            // The type itself stands for its own default group constraints,
            // which are covered by the default group already
            if (!group.equals(propertyHolderType)) {
                groupsToExpand.add(group);
            }
        }
    }

    /**
     * Checks that the given validation groups can be used for validation and
     * returns a defensive copy of them.
     *
     * @param validationGroups
     *            the validation groups to check, may be {@code null}
     * @return a copy of the validation groups, not {@code null}
     * @throws IllegalArgumentException
     *             if any of the given validation groups is not an interface
     */
    private static Class<?>[] copyValidationGroups(
            Class<?>[] validationGroups) {
        if (validationGroups == null || validationGroups.length == 0) {
            return NO_GROUPS;
        }
        Class<?>[] groups = validationGroups.clone();
        for (Class<?> group : groups) {
            Objects.requireNonNull(group, "validation group cannot be null");
            if (!group.isInterface()) {
                throw new IllegalArgumentException("The validation group "
                        + group.getName() + " is not an interface. "
                        + "A validation group has to be an interface, "
                        + "see the Jakarta Bean Validation specification.");
            }
        }
        return groups;
    }

}

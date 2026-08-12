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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

import java.io.Serializable;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.binder.testcomponents.TestTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Specification of the JSR-303 validation group support requested in
 * <a href="https://github.com/vaadin/flow/issues/7032">flow#7032</a>: an
 * application developer should be able to configure the validation group(s) a
 * {@link BeanValidationBinder} uses by default, and to run a single validation
 * pass against a different group.
 * <p>
 * The tests are split in two halves:
 * <ul>
 * <li>{@code current_*} tests pin down what the binder does today, i.e. the gap
 * the feature has to close. They pass on an unmodified binder.</li>
 * <li>{@code target_*} tests describe the semantics the new API has to
 * implement. Since that API does not exist yet, they use a hand-written,
 * group-aware validator plugged in through the protected
 * {@link BeanValidationBinder#configureBinding} hook - which is also the
 * workaround an application developer has to write today.</li>
 * </ul>
 */
class BeanValidationGroupsTest {

    public interface Draft {
    }

    public interface Publish {
    }

    public static class Article implements Serializable {

        // Default group
        @NotEmpty
        private String title;

        // Publish group only
        @NotEmpty(groups = Publish.class)
        private String summary;

        // Draft and Publish groups, but not Default
        @Size(min = 10, groups = { Draft.class, Publish.class })
        private String body;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    private final TestTextField titleField = new TestTextField();
    private final TestTextField summaryField = new TestTextField();
    private final TestTextField bodyField = new TestTextField();

    /**
     * Baseline: without any group configuration only constraints belonging to
     * the {@link Default} group are validated. Whatever the new API looks like,
     * this must keep working exactly like this when no groups are configured.
     */
    @Test
    void current_noGroupsConfigured_defaultGroupConstraintsAreValidated() {
        Binder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class));

        titleField.setValue("");

        assertFalse(binder.isValid(),
                "empty @NotEmpty title should be invalid");
    }

    /**
     * The gap: constraints that declare an explicit group are silently never
     * validated by the binder, and there is no supported way to opt in to them.
     */
    @Test
    void current_noGroupsConfigured_groupScopedConstraintsAreIgnored() {
        Binder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class));

        titleField.setValue("Flow 25");
        // violates @NotEmpty(groups = Publish.class)
        summaryField.setValue("");
        // violates @Size(min = 10, groups = { Draft.class, Publish.class })
        bodyField.setValue("short");

        assertTrue(binder.isValid(),
                "group scoped constraints are not validated today");
    }

    /**
     * The required indicator ignores groups as well: a constraint that only
     * applies to the Publish group marks the field as required even though that
     * constraint is never validated. Once groups are configurable the indicator
     * has to follow the groups that are actually in use.
     */
    @Test
    void current_requiredIndicator_ignoresConstraintGroups() {
        bind(new BeanValidationBinder<>(Article.class));

        assertTrue(titleField.isRequiredIndicatorVisible(),
                "@NotEmpty in the Default group should mark the field required");
        assertTrue(summaryField.isRequiredIndicatorVisible(),
                "@NotEmpty(groups = Publish.class) marks the field required "
                        + "even though it is never validated");
    }

    /**
     * Target: the group configured on the binder is used for every validation,
     * including the implicit one triggered by a field value change.
     */
    @Test
    void target_configuredGroup_isUsedForAllValidation() {
        GroupAwareBinder<Article> binder = bind(
                new GroupAwareBinder<>(Article.class));
        binder.setValidationGroups(Publish.class);

        titleField.setValue("Flow 25");
        summaryField.setValue("");
        bodyField.setValue("A long enough body");

        assertFalse(binder.isValid(),
                "@NotEmpty(groups = Publish.class) should be validated");

        summaryField.setValue("A summary");
        assertTrue(binder.isValid());
    }

    /**
     * Target: group selection follows the JSR-303 rules, i.e. configuring a
     * group replaces the Default group instead of adding to it. Both groups
     * have to be listed explicitly to validate both.
     */
    @Test
    void target_configuredGroup_replacesDefaultGroup() {
        GroupAwareBinder<Article> binder = bind(
                new GroupAwareBinder<>(Article.class));
        binder.setValidationGroups(Publish.class);

        // violates @NotEmpty, which is in the Default group
        titleField.setValue("");
        summaryField.setValue("A summary");
        bodyField.setValue("A long enough body");

        assertTrue(binder.isValid(),
                "Default group constraints are not validated when another "
                        + "group is configured");

        binder.setValidationGroups(Default.class, Publish.class);
        assertFalse(binder.isValid(),
                "listing Default explicitly validates both groups");
    }

    /**
     * Target: a one-shot validation against an explicit group does not change
     * the group configured on the binder.
     */
    @Test
    void target_oneShotGroup_appliesOnlyToThatCall() {
        GroupAwareBinder<Article> binder = bind(
                new GroupAwareBinder<>(Article.class));

        titleField.setValue("Flow 25");
        summaryField.setValue("");
        // violates @Size(min = 10) of the Draft group
        bodyField.setValue("short");

        assertTrue(binder.isValid(), "Default group alone is happy");

        assertFalse(binder.validate(Draft.class).isOk(),
                "one-shot validation should use the given group");

        assertTrue(binder.isValid(),
                "the binder should be back to its configured groups");
        assertEquals(0, binder.getValidationGroups().length);
    }

    private <T extends Binder<Article>> T bind(T binder) {
        binder.bind(titleField, "title");
        binder.bind(summaryField, "summary");
        binder.bind(bodyField, "body");
        binder.setBean(new Article());
        return binder;
    }

    /**
     * Sketch of the behaviour the feature has to provide, implemented on top of
     * the extension points that exist today. This is the workaround an
     * application developer currently has to write, and it is what the tests
     * above use as a stand-in for the real API.
     */
    private static class GroupAwareBinder<BEAN>
            extends BeanValidationBinder<BEAN> {

        private Class<?>[] validationGroups = new Class<?>[0];

        private Class<?>[] oneShotGroups;

        GroupAwareBinder(Class<BEAN> beanType) {
            super(beanType);
        }

        void setValidationGroups(Class<?>... groups) {
            validationGroups = groups;
        }

        Class<?>[] getValidationGroups() {
            return validationGroups;
        }

        BinderValidationStatus<BEAN> validate(Class<?>... groups) {
            oneShotGroups = groups;
            try {
                return validate();
            } finally {
                oneShotGroups = null;
            }
        }

        private Class<?>[] getEffectiveGroups() {
            return oneShotGroups != null ? oneShotGroups : validationGroups;
        }

        @Override
        protected BindingBuilder<BEAN, ?> configureBinding(
                BindingBuilder<BEAN, ?> binding,
                PropertyDefinition<BEAN, ?> definition) {
            // deliberately bypasses BeanValidator, which has no notion of
            // validation groups
            Class<?> holderType = definition.getPropertyHolderType();
            String property = definition.getTopLevelName();
            Validator<Object> validator = (value, context) -> {
                Set<? extends ConstraintViolation<?>> violations = Validation
                        .buildDefaultValidatorFactory().getValidator()
                        .validateValue(holderType, property, value,
                                getEffectiveGroups());
                return violations.stream().findFirst()
                        .map(violation -> ValidationResult
                                .error(violation.getMessage()))
                        .orElseGet(ValidationResult::ok);
            };
            return binding.withValidator(validator);
        }
    }
}

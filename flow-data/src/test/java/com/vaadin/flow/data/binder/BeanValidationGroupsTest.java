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

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.data.validator.BeanValidator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the JSR-303 validation group support of
 * {@link BeanValidationBinder}, see
 * <a href="https://github.com/vaadin/flow/issues/7032">flow#7032</a>: the
 * validation group(s) used by the binder can be configured, and a single
 * validation can be run against other groups without changing the configured
 * ones.
 */
class BeanValidationGroupsTest {

    public interface Draft {
    }

    public interface Publish {
    }

    /**
     * Group that inherits from {@link Publish}, i.e. validating against it also
     * validates the constraints of the Publish group.
     */
    public interface FinalPublish extends Publish {
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
     * the {@link Default} group are validated.
     */
    @Test
    void noGroupsConfigured_defaultGroupConstraintsAreValidated() {
        BeanValidationBinder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class));

        titleField.setValue("");

        assertFalse(binder.isValid(),
                "empty @NotEmpty title should be invalid");
        assertEquals(0, binder.getValidationGroups().length);
    }

    /**
     * Baseline: without any group configuration constraints that declare a
     * group are not validated.
     */
    @Test
    void noGroupsConfigured_groupScopedConstraintsAreIgnored() {
        BeanValidationBinder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class));

        titleField.setValue("Flow 25");
        // violates @NotEmpty(groups = Publish.class)
        summaryField.setValue("");
        // violates @Size(min = 10, groups = { Draft.class, Publish.class })
        bodyField.setValue("short");

        assertTrue(binder.isValid(),
                "group scoped constraints should not be validated");
    }

    @Test
    void configuredGroups_areUsedForAllValidation() {
        BeanValidationBinder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class));
        binder.setValidationGroups(Publish.class);

        titleField.setValue("Flow 25");
        summaryField.setValue("A summary");
        bodyField.setValue("A long enough body");

        assertTrue(binder.isValid());
        assertFalse(summaryField.isInvalid());

        summaryField.setValue("");

        assertFalse(binder.isValid(),
                "@NotEmpty(groups = Publish.class) should be validated");
        assertTrue(summaryField.isInvalid(),
                "the field should be marked invalid when its value changes");
    }

    @Test
    void constructorGroups_areUsedForAllValidation() {
        BeanValidationBinder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class, Publish.class));

        assertArrayEquals(new Class<?>[] { Publish.class },
                binder.getValidationGroups());

        titleField.setValue("Flow 25");
        summaryField.setValue("");
        bodyField.setValue("A long enough body");

        assertFalse(binder.isValid(),
                "@NotEmpty(groups = Publish.class) should be validated");
    }

    /**
     * Group selection follows the JSR-303 rules, i.e. configuring a group
     * replaces the Default group instead of adding to it. Both groups have to
     * be listed explicitly to validate both.
     */
    @Test
    void configuredGroups_replaceDefaultGroup() {
        BeanValidationBinder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class));
        binder.setValidationGroups(Publish.class);

        // violates @NotEmpty, which is in the Default group
        titleField.setValue("");
        summaryField.setValue("A summary");
        bodyField.setValue("A long enough body");

        assertTrue(binder.isValid(),
                "Default group constraints should not be validated when "
                        + "another group is configured");

        binder.setValidationGroups(Default.class, Publish.class);

        assertFalse(binder.isValid(),
                "listing Default explicitly should validate both groups");
    }

    @Test
    void configuredGroups_inheritedGroupsAreValidated() {
        BeanValidationBinder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class, FinalPublish.class));

        titleField.setValue("Flow 25");
        summaryField.setValue("");
        bodyField.setValue("A long enough body");

        assertFalse(binder.isValid(),
                "constraints of the inherited Publish group should be "
                        + "validated");
    }

    @Test
    void noGroupsConfigured_setValidationGroupsWithoutArgumentsRestoresDefault() {
        BeanValidationBinder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class, Publish.class));
        binder.setValidationGroups();

        titleField.setValue("Flow 25");
        summaryField.setValue("");
        bodyField.setValue("short");

        assertEquals(0, binder.getValidationGroups().length);
        assertTrue(binder.isValid());
    }

    /**
     * A one-shot validation uses the given groups without touching the
     * configured ones, which is what makes it possible to only run e.g. slow or
     * save time constraints when the user clicks a button.
     */
    @Test
    void oneShotGroups_appliesOnlyToThatValidation() {
        BeanValidationBinder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class));

        titleField.setValue("Flow 25");
        summaryField.setValue("");
        // violates @Size(min = 10) of the Draft group
        bodyField.setValue("short");

        assertTrue(binder.isValid(),
                "the Default group alone should not be violated");

        assertFalse(binder.validate(Draft.class).isOk(),
                "one-shot validation should use the given group");

        assertEquals(0, binder.getValidationGroups().length,
                "the configured groups should not be changed");
        assertTrue(binder.isValid(),
                "later validation should use the configured groups again");
    }

    @Test
    void oneShotGroups_valueChangeUsesConfiguredGroups() {
        BeanValidationBinder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class));

        assertFalse(binder.validate(Draft.class).isOk());
        assertTrue(bodyField.isInvalid(),
                "one-shot validation should mark the field invalid");

        // still too short for the Draft group, but that group is not configured
        bodyField.setValue("short");

        assertFalse(bodyField.isInvalid(),
                "validation on value change should use the configured groups");
    }

    @Test
    void oneShotGroups_withoutArgumentsUsesConfiguredGroups() {
        BeanValidationBinder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class, Publish.class));

        titleField.setValue("");
        summaryField.setValue("A summary");
        bodyField.setValue("A long enough body");

        assertTrue(binder.validate(new Class<?>[0]).isOk());
    }

    /**
     * Bean level validators, which are the only way to validate class level
     * constraints with a Binder, can pick up the groups in effect from the
     * binder.
     */
    @Test
    void oneShotGroups_areVisibleToBeanLevelValidators() {
        BeanValidationBinder<Article> binder = new BeanValidationBinder<>(
                Article.class, Publish.class);
        List<Class<?>[]> seenGroups = new ArrayList<>();
        binder.withValidator((article, context) -> {
            BeanValidationBinder<?> source = (BeanValidationBinder<?>) context
                    .getBinder().orElseThrow();
            seenGroups.add(source.getValidationGroups());
            return ValidationResult.ok();
        });
        bind(binder);

        titleField.setValue("Flow 25");
        summaryField.setValue("A summary");
        bodyField.setValue("A long enough body");

        // a value change validates the bean as well when a bean is set
        seenGroups.clear();

        binder.validate();
        binder.validate(Draft.class);

        assertEquals(2, seenGroups.size());
        assertArrayEquals(new Class<?>[] { Publish.class }, seenGroups.get(0));
        assertArrayEquals(new Class<?>[] { Draft.class }, seenGroups.get(1));
    }

    @Test
    void requiredIndicator_noGroupsConfigured_onlyDefaultGroupCounts() {
        bind(new BeanValidationBinder<>(Article.class));

        assertTrue(titleField.isRequiredIndicatorVisible(),
                "@NotEmpty in the Default group should mark the field required");
        assertFalse(summaryField.isRequiredIndicatorVisible(),
                "a constraint of a group that is not validated should not "
                        + "mark the field required");
    }

    @Test
    void requiredIndicator_configuredGroupsAreTakenIntoAccount() {
        bind(new BeanValidationBinder<>(Article.class, Publish.class));

        assertFalse(titleField.isRequiredIndicatorVisible(),
                "the Default group is not validated when another group is "
                        + "configured");
        assertTrue(summaryField.isRequiredIndicatorVisible(),
                "@NotEmpty(groups = Publish.class) should mark the field "
                        + "required when the Publish group is configured");
    }

    @Test
    void requiredIndicator_followsInheritedGroups() {
        bind(new BeanValidationBinder<>(Article.class, FinalPublish.class));

        assertTrue(summaryField.isRequiredIndicatorVisible(),
                "a constraint of an inherited group should mark the field "
                        + "required");
    }

    @Test
    void beanValidator_validatesGivenGroups() {
        BeanValidator validator = new BeanValidator(Article.class, "summary",
                Publish.class);

        assertArrayEquals(new Class<?>[] { Publish.class },
                validator.getValidationGroups());
        assertTrue(validator.apply("", new ValueContext()).isError());
        assertFalse(validator.apply("A summary", new ValueContext()).isError());
        assertFalse(
                new BeanValidator(Article.class, "summary")
                        .apply("", new ValueContext()).isError(),
                "without groups the Publish constraint should be ignored");
    }

    @Test
    void binderWithGroupsIsSerializable() {
        BeanValidationBinder<Article> binder = bind(
                new BeanValidationBinder<>(Article.class, Publish.class));

        BinderTestBase.testSerialization(binder);
    }

    private <T extends BeanValidationBinder<Article>> T bind(T binder) {
        binder.bind(titleField, "title");
        binder.bind(summaryField, "summary");
        binder.bind(bodyField, "body");
        binder.setBean(new Article());
        return binder;
    }
}

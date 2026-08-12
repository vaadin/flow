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

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.tests.data.bean.BeanToValidate;
import com.vaadin.flow.tests.data.bean.ConvertibleValues;
import com.vaadin.flow.tests.data.bean.Person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executable specification for resolving bean property names from the method
 * references passed to {@link Binder}, i.e. making
 * {@code binder.bind(field, Person::getFirstName, Person::setFirstName)} know
 * that it is bound to the {@code firstName} property.
 * <p>
 * The tests are split in two parts:
 * <ul>
 * <li>{@code serializedLambda_*} verify the underlying mechanism: because
 * {@link ValueProvider} and {@link Setter} already extend {@link Serializable},
 * a method reference assigned to them carries a {@link SerializedLambda} that
 * names the referenced method. These tests pass today and exist to document
 * which lambda shapes can and cannot be resolved.
 * <li>{@code binder_*} describe the behaviour that resolving the property name
 * would unlock. These <em>fail</em> until the resolution is wired into
 * {@code Binder}, and are the specification for that work.
 * </ul>
 */
class MethodReferencePropertyNameTest {

    private UI ui;

    @BeforeEach
    void setUp() {
        ui = new UI();
        ui.setLocale(Locale.ENGLISH);
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    /*
     * ------------------------------------------------------------------ The
     * mechanism: what a Serializable method reference tells us.
     * ------------------------------------------------------------------
     */

    @Test
    void serializedLambda_getterReference_resolvesProperty() {
        assertEquals("firstName",
                propertyName(
                        (ValueProvider<Person, String>) Person::getFirstName)
                        .orElse(null));
    }

    @Test
    void serializedLambda_setterReference_resolvesSameProperty() {
        assertEquals("firstName",
                propertyName((Setter<Person, String>) Person::setFirstName)
                        .orElse(null),
                "a setter reference must resolve to the same property name as "
                        + "the matching getter, so that one can be inferred "
                        + "from the other");
    }

    @Test
    void serializedLambda_primitiveAccessors_resolveProperty() {
        // int getter widened/boxed to the Integer target type of the
        // functional interface
        assertEquals("age",
                propertyName((ValueProvider<Person, Integer>) Person::getAge)
                        .orElse(null));
        assertEquals("age",
                propertyName((Setter<Person, Integer>) Person::setAge)
                        .orElse(null));
    }

    @Test
    void serializedLambda_isGetterReference_resolvesProperty() {
        assertEquals("stringToPrimitiveBoolean", propertyName(
                (ValueProvider<ConvertibleValues, Boolean>) ConvertibleValues::isStringToPrimitiveBoolean)
                .orElse(null), "the is prefix must be stripped as well");
    }

    @Test
    void serializedLambda_booleanGetterReference_resolvesProperty() {
        assertEquals("deceased",
                propertyName(
                        (ValueProvider<Person, Boolean>) Person::getDeceased)
                        .orElse(null));
    }

    @Test
    void serializedLambda_recordAccessorReference_resolvesProperty() {
        assertEquals("label",
                propertyName((ValueProvider<Point, String>) Point::label)
                        .orElse(null),
                "record accessors have no get prefix, the method name is the "
                        + "property name");
    }

    @Test
    void serializedLambda_explicitLambda_isNotResolvable() {
        assertFalse(propertyName(
                (ValueProvider<Person, String>) person -> person.getFirstName())
                .isPresent(),
                "an explicit lambda compiles to a synthetic method and must be "
                        + "detected as not being a property reference");
    }

    @Test
    void serializedLambda_computedValue_isNotResolvable() {
        assertFalse(propertyName(
                (ValueProvider<Person, String>) person -> person.getFirstName()
                        + " " + person.getLastName())
                .isPresent());
    }

    @Test
    void serializedLambda_nestedPropertyLambda_isNotResolvable() {
        assertFalse(
                propertyName((ValueProvider<Person, String>) person -> person
                        .getAddress().getStreetAddress()).isPresent(),
                "nested properties cannot be expressed as a single method "
                        + "reference; they stay string based");
    }

    /*
     * ------------------------------------------------------------------ The
     * payoff: what Binder should do with the resolved name. These fail until
     * the resolution is wired into Binder.
     * ------------------------------------------------------------------
     */

    @Test
    void binder_boundWithMethodReferences_bindingIsFoundByPropertyName() {
        Binder<Person> binder = new Binder<>(Person.class);
        TestTextField field = new TestTextField();

        Binding<Person, String> binding = binder.bind(field,
                Person::getFirstName, Person::setFirstName);

        assertEquals(Optional.of(binding), binder.getBinding("firstName"),
                "a binding created from method references should be "
                        + "addressable by its property name, like a binding "
                        + "created with bind(field, \"firstName\")");
    }

    @Test
    void binder_boundWithMethodReferences_beanValidationIsApplied() {
        BeanValidationBinder<BeanToValidate> binder = new BeanValidationBinder<>(
                BeanToValidate.class);
        BeanToValidate bean = new BeanToValidate();
        bean.setFirstname("Johannes");
        bean.setAge(32);

        TestTextField field = new TestTextField();
        binder.bind(field, BeanToValidate::getFirstname,
                BeanToValidate::setFirstname);
        binder.setBean(bean);

        // firstname is annotated @Size(min = 3, max = 16)
        field.setValue("ab");

        assertFalse(binder.validate().isOk(),
                "the JSR-303 constraints of the resolved property should be "
                        + "validated, as they are for a name based binding");
    }

    @Test
    void binder_boundWithMethodReferences_requiredIndicatorIsSet() {
        BeanValidationBinder<BeanToValidate> binder = new BeanValidationBinder<>(
                BeanToValidate.class);

        TestTextField field = new TestTextField();
        binder.bind(field, BeanToValidate::getFirstname,
                BeanToValidate::setFirstname);
        binder.setBean(new BeanToValidate());

        assertTrue(field.isRequiredIndicatorVisible(),
                "@Size(min = 3) on the resolved property should mark the field "
                        + "as required, as it does for a name based binding");
    }

    /*
     * ------------------------------------------------------------------
     * Test-local stand-in for the resolution logic that production code would
     * provide.
     * ------------------------------------------------------------------
     */

    /**
     * Resolves the bean property name a serializable method reference points
     * at, or an empty optional if the lambda is not a plain accessor reference.
     */
    private static Optional<String> propertyName(Serializable methodReference) {
        SerializedLambda lambda = serializedLambda(methodReference);
        if (lambda.getCapturedArgCount() > 0) {
            // captures state, so it cannot be a plain accessor reference
            return Optional.empty();
        }
        String methodName = lambda.getImplMethodName();
        if (methodName.startsWith("lambda$") || methodName.contains("$")) {
            // synthetic method generated for an inline lambda, or a synthetic
            // accessor generated for a private target
            return Optional.empty();
        }
        for (String prefix : new String[] { "get", "set", "is" }) {
            if (methodName.startsWith(prefix)
                    && methodName.length() > prefix.length()) {
                return Optional.of(Introspector
                        .decapitalize(methodName.substring(prefix.length())));
            }
        }
        // record accessor or fluent getter
        return Optional.of(methodName);
    }

    private static SerializedLambda serializedLambda(Serializable lambda) {
        try {
            Method writeReplace = lambda.getClass()
                    .getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            return (SerializedLambda) writeReplace.invoke(lambda);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                    "Could not read the SerializedLambda of " + lambda, e);
        }
    }

    private record Point(int x, String label) {
    }
}

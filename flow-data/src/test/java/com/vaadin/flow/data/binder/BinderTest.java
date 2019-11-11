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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.data.validator.NotEmptyValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.tests.data.bean.Person;
import com.vaadin.flow.tests.data.bean.Sex;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BinderTest extends BinderTestBase<Binder<Person>, Person> {

    private Map<HasValue<?, ?>, String> componentErrors = new HashMap<>();

    @Rule
    /*
     * transient to avoid interfering with serialization tests that capture a
     * test instance in a closure
     */
    public transient ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
        binder = new Binder<Person>() {
            @Override
            protected void handleError(HasValue<?, ?> field,
                    ValidationResult result) {
                super.handleError(field, result);
                componentErrors.put(field, result.getErrorMessage());
            }

            @Override
            protected void clearError(HasValue<?, ?> field) {
                super.clearError(field);
                componentErrors.remove(field);
            }
        };
        item = new Person();
        item.setFirstName("Johannes");
        item.setAge(32);
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void bindNullBean_noBeanPresent() {
        binder.setBean(item);
        assertNotNull(binder.getBean());

        binder.setBean(null);
        assertNull(binder.getBean());
    }

    @Test
    public void bindNullBean_FieldsAreCleared() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.setBean(item);
        assertEquals("No name field value", "Johannes", nameField.getValue());
        assertEquals("No age field value", "32", ageField.getValue());

        binder.setBean(null);
        assertEquals("Name field not empty", "", nameField.getValue());
        assertEquals("Age field not empty", "", ageField.getValue());
    }

    @Test
    public void removeInvalidBinding_validateDoesNotThrow() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        Binding<Person, Integer> ageBinding = binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.withValidator(bean -> true, "");
        binder.setBean(item);

        ageField.setValue("foo");

        binder.removeBinding(ageBinding);

        binder.validate();
    }

    @Test
    public void clearForReadBean_boundFieldsAreCleared() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.readBean(item);

        assertEquals("No name field value", "Johannes", nameField.getValue());
        assertEquals("No age field value", "32", ageField.getValue());

        binder.readBean(null);
        assertEquals("Name field not empty", "", nameField.getValue());
        assertEquals("Age field not empty", "", ageField.getValue());
    }

    @Test
    public void clearReadOnlyField_shouldClearField() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);

        // Make name field read only
        nameField.setReadOnly(true);

        binder.setBean(item);
        assertEquals("No name field value", "Johannes", nameField.getValue());

        binder.setBean(null);

        assertEquals("ReadOnly field not empty", "", nameField.getValue());
    }

    @Test
    public void clearBean_setsHasChangesToFalse() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);

        // Make name field read only
        nameField.setReadOnly(true);

        binder.readBean(item);
        assertEquals("No name field value", "Johannes", nameField.getValue());
        nameField.setValue("James");

        assertTrue("Binder did not have value changes", binder.hasChanges());

        binder.readBean(null);

        assertFalse("Binder has changes after clearing all fields",
                binder.hasChanges());

    }

    @Test
    public void clearReadOnlyBinder_shouldClearFields() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        binder.setReadOnly(true);

        binder.setBean(item);

        binder.setBean(null);
        assertEquals("ReadOnly name field not empty", "", nameField.getValue());
        assertEquals("ReadOnly age field not empty", "", ageField.getValue());
    }

    @Test(expected = NullPointerException.class)
    public void bindNullField_throws() {
        binder.forField(null);
    }

    @Test(expected = NullPointerException.class)
    public void bindNullGetter_throws() {
        binder.bind(nameField, null, Person::setFirstName);
    }

    @Test
    public void fieldBound_bindItem_fieldValueUpdated() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.setBean(item);
        assertEquals("Johannes", nameField.getValue());
    }

    @Test
    public void fieldBoundWithShortcut_bindBean_fieldValueUpdated() {
        bindName();
        assertEquals("Johannes", nameField.getValue());
    }

    @Test
    public void beanBound_updateFieldValue_beanValueUpdated() {
        binder.setBean(item);
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);

        assertEquals("Johannes", nameField.getValue());
        nameField.setValue("Artur");
        assertEquals("Artur", item.getFirstName());
    }

    @Test
    public void bound_getBean_returnsBoundBean() {
        assertNull(binder.getBean());
        binder.setBean(item);
        assertSame(item, binder.getBean());
    }

    @Test
    public void unbound_getBean_returnsNothing() {
        binder.setBean(item);
        binder.removeBean();
        assertNull(binder.getBean());
    }

    @Test
    public void bound_changeFieldValue_beanValueUpdated() {
        bindName();
        nameField.setValue("Henri");
        assertEquals("Henri", item.getFirstName());
    }

    @Test
    public void unbound_changeFieldValue_beanValueNotUpdated() {
        bindName();
        nameField.setValue("Henri");
        binder.removeBean();
        nameField.setValue("Aleksi");
        assertEquals("Henri", item.getFirstName());
    }

    @Test
    public void bindNullSetter_valueChangesIgnored() {
        binder.bind(nameField, Person::getFirstName, null);
        binder.setBean(item);
        nameField.setValue("Artur");
        assertEquals(item.getFirstName(), "Johannes");
    }

    @Test
    public void bound_bindToAnotherBean_stopsUpdatingOriginal() {
        bindName();
        nameField.setValue("Leif");

        Person p2 = new Person();
        p2.setFirstName("Marlon");
        binder.setBean(p2);
        assertEquals("Marlon", nameField.getValue());
        assertEquals("Leif", item.getFirstName());
        assertSame(p2, binder.getBean());

        nameField.setValue("Ilia");
        assertEquals("Ilia", p2.getFirstName());
        assertEquals("Leif", item.getFirstName());
    }

    @Test
    public void save_unbound_noChanges() throws ValidationException {
        Binder<Person> binder = new Binder<>();
        Person person = new Person();

        int age = 10;
        person.setAge(age);

        binder.writeBean(person);

        Assert.assertEquals(age, person.getAge());
    }

    @Test
    public void save_bound_beanIsUpdated() throws ValidationException {
        Binder<Person> binder = new Binder<>();
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);

        Person person = new Person();

        String fieldValue = "bar";
        nameField.setValue(fieldValue);

        person.setFirstName("foo");

        binder.writeBean(person);

        Assert.assertEquals(fieldValue, person.getFirstName());
    }

    @Test
    public void load_bound_fieldValueIsUpdated() {
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);

        Person person = new Person();

        String name = "bar";
        person.setFirstName(name);
        binder.readBean(person);

        Assert.assertEquals(name, nameField.getValue());
    }

    @Test
    public void load_unbound_noChanges() {
        nameField.setValue("");

        Person person = new Person();

        String name = "bar";
        person.setFirstName(name);
        binder.readBean(person);

        Assert.assertEquals("", nameField.getValue());
    }

    protected void bindName() {
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);
        binder.setBean(item);
    }

    @Test
    public void binding_with_null_representation() {
        String nullRepresentation = "Some arbitrary text";
        String realName = "John";
        Person namelessPerson = new Person(null, "Doe", "", 25, Sex.UNKNOWN,
                null);

        binder.forField(nameField).withNullRepresentation(nullRepresentation)
                .bind(Person::getFirstName, Person::setFirstName);

        // Bind a person with null value and check that null representation is
        // used
        binder.setBean(namelessPerson);
        Assert.assertEquals(
                "Null value from bean was not converted to explicit null representation",
                nullRepresentation, nameField.getValue());

        // Verify that changes are applied to bean
        nameField.setValue(realName);
        Assert.assertEquals(
                "Bean was not correctly updated from a change in the field",
                realName, namelessPerson.getFirstName());

        // Verify conversion back to null
        nameField.setValue(nullRepresentation);
        Assert.assertEquals(
                "Two-way null representation did not change value back to null",
                null, namelessPerson.getFirstName());
    }

    @Test
    public void binding_with_default_null_representation() {
        TestTextField nullTextField = new TestTextField() {
            @Override
            public String getEmptyValue() {
                return "null";
            }
        };

        Person namelessPerson = new Person(null, "Doe", "", 25, Sex.UNKNOWN,
                null);
        binder.bind(nullTextField, Person::getFirstName, Person::setFirstName);
        binder.setBean(namelessPerson);

        assertTrue(nullTextField.isEmpty());
        Assert.assertEquals("null", namelessPerson.getFirstName());

        // Change value, see that textfield is not empty and bean is updated.
        nullTextField.setValue("");
        assertFalse(nullTextField.isEmpty());
        Assert.assertEquals("First name of person was not properly updated", "",
                namelessPerson.getFirstName());

        // Verify that default null representation does not map back to null
        nullTextField.setValue("null");
        assertTrue(nullTextField.isEmpty());
        Assert.assertEquals("Default one-way null representation failed.",
                "null", namelessPerson.getFirstName());
    }

    @Test
    public void binding_with_null_representation_value_not_null() {
        String nullRepresentation = "Some arbitrary text";

        binder.forField(nameField).withNullRepresentation(nullRepresentation)
                .bind(Person::getFirstName, Person::setFirstName);

        assertFalse("First name in item should not be null",
                Objects.isNull(item.getFirstName()));
        binder.setBean(item);

        Assert.assertEquals("Field value was not set correctly",
                item.getFirstName(), nameField.getValue());
    }

    @Test
    public void withConverter_disablesDefaulNullRepresentation() {
        Integer customNullConverter = 0;
        binder.forField(ageField).withNullRepresentation("foo")
                .withConverter(new StringToIntegerConverter(""))
                .withConverter(age -> age,
                        age -> age == null ? customNullConverter : age)
                .bind(Person::getSalary, Person::setSalary);
        binder.setBean(item);

        Assert.assertEquals(customNullConverter.toString(),
                ageField.getValue());

        Integer salary = 11;
        ageField.setValue(salary.toString());
        Assert.assertEquals(11, salary.intValue());
    }

    @Test
    public void beanBinder_nullRepresentationIsNotDisabled() {
        Binder<Person> binder = new Binder<>(Person.class);
        binder.forField(nameField).bind("firstName");

        Person person = new Person();
        binder.setBean(person);

        Assert.assertEquals("", nameField.getValue());
    }

    @Test
    public void beanBinder_withConverter_nullRepresentationIsNotDisabled() {
        String customNullPointerRepresentation = "foo";
        Binder<Person> binder = new Binder<>(Person.class);
        binder.forField(nameField)
                .withConverter(value -> value,
                        value -> value == null ? customNullPointerRepresentation
                                : value)
                .bind("firstName");

        Person person = new Person();
        binder.setBean(person);

        Assert.assertEquals(customNullPointerRepresentation,
                nameField.getValue());
    }

    @Test
    public void withValidator_doesNotDisablesDefaulNullRepresentation() {
        String nullRepresentation = "foo";
        binder.forField(nameField).withNullRepresentation(nullRepresentation)
                .withValidator(new NotEmptyValidator<>(""))
                .bind(Person::getFirstName, Person::setFirstName);
        item.setFirstName(null);
        binder.setBean(item);

        Assert.assertEquals(nullRepresentation, nameField.getValue());

        String newValue = "bar";
        nameField.setValue(newValue);
        Assert.assertEquals(newValue, item.getFirstName());
    }

    @Test
    public void setRequired_withErrorMessage_fieldGetsRequiredIndicatorAndValidator() {
        TestTextField textField = new TestTextField();
        assertFalse(textField.isRequiredIndicatorVisible());

        BindingBuilder<Person, String> binding = binder.forField(textField);
        assertFalse(textField.isRequiredIndicatorVisible());

        binding.asRequired("foobar");
        assertTrue(textField.isRequiredIndicatorVisible());

        binding.bind(Person::getFirstName, Person::setFirstName);
        binder.setBean(item);
        assertThat(textField.getErrorMessage(), isEmptyString());

        textField.setValue(textField.getEmptyValue());
        Assert.assertEquals("foobar", componentErrors.get(textField));

        textField.setValue("value");
        assertFalse(textField.isInvalid());
        assertTrue(textField.isRequiredIndicatorVisible());
    }

    @Test
    public void readNullBeanRemovesError() {
        TestTextField textField = new TestTextField();
        binder.forField(textField).asRequired("foobar")
                .bind(Person::getFirstName, Person::setFirstName);
        Assert.assertTrue(textField.isRequiredIndicatorVisible());
        Assert.assertNull(componentErrors.get(textField));

        binder.readBean(item);
        Assert.assertNull(componentErrors.get(textField));

        textField.setValue(textField.getEmptyValue());
        Assert.assertTrue(textField.isRequiredIndicatorVisible());
        Assert.assertNotNull(componentErrors.get(textField));

        binder.readBean(null);
        assertTrue(textField.isRequiredIndicatorVisible());
        Assert.assertNull(componentErrors.get(textField));
    }

    @Test
    public void setRequired_withErrorMessageProvider_fieldGetsRequiredIndicatorAndValidator() {
        TestTextField textField = new TestTextField();
        assertFalse(textField.isRequiredIndicatorVisible());

        BindingBuilder<Person, String> binding = binder.forField(textField);
        assertFalse(textField.isRequiredIndicatorVisible());
        AtomicInteger invokes = new AtomicInteger();

        binding.asRequired(context -> {
            invokes.incrementAndGet();
            return "foobar";
        });
        assertTrue(textField.isRequiredIndicatorVisible());

        binding.bind(Person::getFirstName, Person::setFirstName);
        binder.setBean(item);
        assertThat(textField.getErrorMessage(), isEmptyString());
        Assert.assertEquals(0, invokes.get());

        textField.setValue(textField.getEmptyValue());
        Assert.assertEquals("foobar", componentErrors.get(textField));
        // validation is done for all changed bindings once.
        Assert.assertEquals(1, invokes.get());

        textField.setValue("value");
        assertFalse(textField.isInvalid());
        assertTrue(textField.isRequiredIndicatorVisible());
    }

    @Test
    public void setRequired_withCustomValidator_fieldGetsRequiredIndicatorAndValidator() {
        TestTextField textField = new TestTextField();
        assertFalse(textField.isRequiredIndicatorVisible());

        BindingBuilder<Person, String> binding = binder.forField(textField);
        assertFalse(textField.isRequiredIndicatorVisible());
        AtomicInteger invokes = new AtomicInteger();

        Validator<String> customRequiredValidator = (value, context) -> {
            invokes.incrementAndGet();
            if (StringUtils.isBlank(value)) {
                return ValidationResult.error("Input is required.");
            }
            return ValidationResult.ok();
        };
        binding.asRequired(customRequiredValidator);
        assertTrue(textField.isRequiredIndicatorVisible());
        binding.bind(Person::getFirstName, Person::setFirstName);
        binder.setBean(item);

        assertThat(textField.getErrorMessage(), isEmptyString());
        assertEquals(1, invokes.get());

        textField.setValue("        ");
        String errorMessage = textField.getErrorMessage();
        assertNotNull(errorMessage);
        assertEquals("Input is required.", componentErrors.get(textField));
        // validation is done for all changed bindings once.
        assertEquals(2, invokes.get());

        textField.setValue("value");
        assertFalse(textField.isInvalid());
        assertTrue(textField.isRequiredIndicatorVisible());
    }

    @Test
    public void setRequired_withCustomValidator_modelConverterBeforeValidator() {
        TestTextField textField = new TestTextField();
        assertFalse(textField.isRequiredIndicatorVisible());

        Converter<String, String> stringBasicPreProcessingConverter = new Converter<String, String>() {
            @Override
            public Result<String> convertToModel(String value,
                    ValueContext context) {
                if (StringUtils.isBlank(value)) {
                    return Result.ok(null);
                }
                return Result.ok(StringUtils.trim(value));
            }

            @Override
            public String convertToPresentation(String value,
                    ValueContext context) {
                if (value == null) {
                    return "";
                }
                return value;
            }
        };

        AtomicInteger invokes = new AtomicInteger();
        Validator<String> customRequiredValidator = (value, context) -> {
            invokes.incrementAndGet();
            if (value == null) {
                return ValidationResult.error("Input required.");
            }
            return ValidationResult.ok();
        };

        binder.forField(textField)
                .withConverter(stringBasicPreProcessingConverter)
                .asRequired(customRequiredValidator)
                .bind(Person::getFirstName, Person::setFirstName);
        binder.setBean(item);
        assertThat(textField.getErrorMessage(), isEmptyString());
        assertEquals(1, invokes.get());
        textField.setValue(" ");
        assertNotNull(textField.getErrorMessage());
        assertEquals("Input required.", componentErrors.get(textField));
        // validation is done for all changed bindings once.
        assertEquals(2, invokes.get());

        textField.setValue("value");
        assertFalse(textField.isInvalid());
        assertTrue(textField.isRequiredIndicatorVisible());
    }

    @Test
    public void validationStatusHandler_onlyRunForChangedField() {
        TestTextField firstNameField = new TestTextField();
        TestTextField lastNameField = new TestTextField();

        AtomicInteger invokes = new AtomicInteger();

        binder.forField(firstNameField)
                .withValidator(new NotEmptyValidator<>(""))
                .withValidationStatusHandler(
                        validationStatus -> invokes.addAndGet(1))
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(lastNameField)
                .withValidator(new NotEmptyValidator<>(""))
                .bind(Person::getLastName, Person::setLastName);

        binder.setBean(item);
        // setting the bean causes 2:
        Assert.assertEquals(2, invokes.get());

        lastNameField.setValue("");
        Assert.assertEquals(2, invokes.get());

        firstNameField.setValue("");
        Assert.assertEquals(3, invokes.get());

        binder.removeBean();
        Person person = new Person();
        person.setFirstName("a");
        person.setLastName("a");
        binder.readBean(person);
        // reading from a bean causes 2:
        Assert.assertEquals(5, invokes.get());

        lastNameField.setValue("");
        Assert.assertEquals(5, invokes.get());

        firstNameField.setValue("");
        Assert.assertEquals(6, invokes.get());
    }

    @Test(expected = IllegalStateException.class)
    public void noArgsConstructor_stringBind_throws() {
        binder.bind(new TestTextField(), "firstName");
    }

    @Test
    public void setReadOnly_unboundBinder() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);

        binder.forField(ageField);

        binder.setReadOnly(true);

        assertTrue(nameField.isReadOnly());
        assertFalse(ageField.isReadOnly());

        binder.setReadOnly(false);

        assertFalse(nameField.isReadOnly());
        assertFalse(ageField.isReadOnly());
    }

    @Test
    public void setReadOnly_boundBinder() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);

        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        binder.setBean(new Person());

        binder.setReadOnly(true);

        assertTrue(nameField.isReadOnly());
        assertTrue(ageField.isReadOnly());

        binder.setReadOnly(false);

        assertFalse(nameField.isReadOnly());
        assertFalse(ageField.isReadOnly());
    }

    @Test
    public void setReadOnly_binderLoadedByReadBean() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);

        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        binder.readBean(new Person());

        binder.setReadOnly(true);

        assertTrue(nameField.isReadOnly());
        assertTrue(ageField.isReadOnly());

        binder.setReadOnly(false);

        assertFalse(nameField.isReadOnly());
        assertFalse(ageField.isReadOnly());
    }

    @Test
    public void setReadonlyShouldIgnoreBindingsWithNullSetter() {
        binder.bind(nameField, Person::getFirstName, null);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        binder.setReadOnly(true);
        assertTrue("Name field should be ignored but should be readonly",
                nameField.isReadOnly());
        assertTrue("Age field should be readonly", ageField.isReadOnly());

        binder.setReadOnly(false);
        assertTrue("Name field should be ignored and should remain readonly",
                nameField.isReadOnly());
        assertFalse("Age field should not be readonly", ageField.isReadOnly());

        nameField.setReadOnly(false);
        binder.setReadOnly(false);
        assertFalse("Name field should be ignored and remain not readonly",
                nameField.isReadOnly());
        assertFalse("Age field should not be readonly", ageField.isReadOnly());

        binder.setReadOnly(true);
        assertFalse("Name field should be ignored and remain not readonly",
                nameField.isReadOnly());
        assertTrue("Age field should be readonly", ageField.isReadOnly());
    }

    @Test
    public void isValidTest_bound_binder() {
        binder.forField(nameField)
                .withValidator(Validator.from(
                        name -> !name.equals("fail field validation"), ""))
                .bind(Person::getFirstName, Person::setFirstName);

        binder.withValidator(Validator.from(
                person -> !person.getFirstName().equals("fail bean validation"),
                ""));

        binder.setBean(item);

        Assert.assertTrue(binder.isValid());

        nameField.setValue("fail field validation");
        Assert.assertFalse(binder.isValid());

        nameField.setValue("");
        Assert.assertTrue(binder.isValid());

        nameField.setValue("fail bean validation");
        Assert.assertFalse(binder.isValid());
    }

    @Test
    public void isValidTest_unbound_binder() {
        binder.forField(nameField)
                .withValidator(Validator.from(
                        name -> !name.equals("fail field validation"), ""))
                .bind(Person::getFirstName, Person::setFirstName);

        Assert.assertTrue(binder.isValid());

        nameField.setValue("fail field validation");
        Assert.assertFalse(binder.isValid());

        nameField.setValue("");
        Assert.assertTrue(binder.isValid());
    }

    @Test(expected = IllegalStateException.class)
    public void isValidTest_unbound_binder_throws_with_bean_level_validation() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.withValidator(Validator.from(
                person -> !person.getFirstName().equals("fail bean validation"),
                ""));
        binder.isValid();
    }

    @Test
    public void getFields_returnsFields() {
        Assert.assertEquals(0, binder.getFields().count());
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        assertStreamEquals(Stream.of(nameField), binder.getFields());
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        assertStreamEquals(Stream.of(nameField, ageField), binder.getFields());
    }

    private void assertStreamEquals(Stream<?> s1, Stream<?> s2) {
        Assert.assertArrayEquals(s1.toArray(), s2.toArray());
    }

    @Test
    public void multiple_calls_to_same_binding_builder() {
        String stringLength = "String length failure";
        String conversion = "Conversion failed";
        String ageLimit = "Age not in valid range";
        BindingValidationStatus validation;

        binder = new Binder<>(Person.class);
        BindingBuilder builder = binder.forField(ageField);
        builder.withValidator(new StringLengthValidator(stringLength, 0, 3));
        builder.withConverter(new StringToIntegerConverter(conversion));
        builder.withValidator(new IntegerRangeValidator(ageLimit, 3, 150));
        Binding<Person, ?> bind = builder.bind("age");

        binder.setBean(item);

        ageField.setValue("123123");
        validation = bind.validate();
        Assert.assertTrue(validation.isError());
        Assert.assertEquals(stringLength, validation.getMessage().get());

        ageField.setValue("age");
        validation = bind.validate();
        Assert.assertTrue(validation.isError());
        Assert.assertEquals(conversion, validation.getMessage().get());

        ageField.setValue("256");
        validation = bind.validate();
        Assert.assertTrue(validation.isError());
        Assert.assertEquals(ageLimit, validation.getMessage().get());

        ageField.setValue("30");
        validation = bind.validate();
        Assert.assertFalse(validation.isError());
        Assert.assertEquals(30, item.getAge());
    }

    @Test
    public void remove_field_binding() {
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter("Can't convert"))
                .bind(Person::getAge, Person::setAge);

        // Test that the binding does work
        assertTrue("Field not initially empty", ageField.isEmpty());
        binder.setBean(item);
        assertEquals("Binding did not work", String.valueOf(item.getAge()),
                ageField.getValue());
        binder.setBean(null);
        assertTrue("Field not cleared", ageField.isEmpty());

        // Remove the binding
        binder.removeBinding(ageField);

        // Test that it does not work anymore
        binder.setBean(item);
        assertNotEquals("Binding was not removed",
                String.valueOf(item.getAge()), ageField.getValue());
    }

    @Test
    public void remove_propertyname_binding() {
        // Use a bean aware binder
        Binder<Person> binder = new Binder<>(Person.class);

        binder.bind(nameField, "firstName");

        // Test that the binding does work
        assertTrue("Field not initially empty", nameField.isEmpty());
        binder.setBean(item);
        assertEquals("Binding did not work", item.getFirstName(),
                nameField.getValue());
        binder.setBean(null);
        assertTrue("Field not cleared", nameField.isEmpty());

        // Remove the binding
        binder.removeBinding("firstName");

        // Test that it does not work anymore
        binder.setBean(item);
        assertNotEquals("Binding was not removed", item.getFirstName(),
                nameField.getValue());
    }

    @Test
    public void remove_binding() {
        Binding<Person, Integer> binding = binder.forField(ageField)
                .withConverter(new StringToIntegerConverter("Can't convert"))
                .bind(Person::getAge, Person::setAge);

        // Test that the binding does work
        assertTrue("Field not initially empty", ageField.isEmpty());
        binder.setBean(item);
        assertEquals("Binding did not work", String.valueOf(item.getAge()),
                ageField.getValue());
        binder.setBean(null);
        assertTrue("Field not cleared", ageField.isEmpty());

        // Remove the binding
        binder.removeBinding(binding);

        // Test that it does not work anymore
        binder.setBean(item);
        assertNotEquals("Binding was not removed",
                String.valueOf(item.getAge()), ageField.getValue());
    }

    @Test
    public void remove_binding_fromFieldValueChangeListener() {
        // Add listener before bind to make sure it will be executed first.
        nameField.addValueChangeListener(e -> {
            if (e.getValue() == "REMOVE") {
                binder.removeBinding(nameField);
            }
        });

        binder.bind(nameField, Person::getFirstName, Person::setFirstName);

        binder.setBean(item);

        nameField.setValue("REMOVE");

        // Removed binding should not update bean.
        assertNotEquals("REMOVE", item.getFirstName());
    }

    @Test
    public void removed_binding_not_updates_value() {
        Binding<Person, Integer> binding = binder.forField(ageField)
                .withConverter(new StringToIntegerConverter("Can't convert"))
                .bind(Person::getAge, Person::setAge);

        binder.setBean(item);

        String modifiedAge = String.valueOf(item.getAge() + 10);
        String ageBeforeUnbind = String.valueOf(item.getAge());

        binder.removeBinding(binding);

        ageField.setValue(modifiedAge);

        assertEquals("Binding still affects bean even after unbind",
                ageBeforeUnbind, String.valueOf(item.getAge()));

    }

    static class MyBindingHandler implements BindingValidationStatusHandler {

        boolean expectingError = false;
        int callCount = 0;

        @Override
        public void statusChange(BindingValidationStatus<?> statusChange) {
            ++callCount;
            if (expectingError) {
                assertTrue("Expecting error", statusChange.isError());
            } else {
                assertFalse("Unexpected error", statusChange.isError());
            }
        }
    }

    @Test
    public void execute_binding_status_handler_from_binder_status_handler() {
        MyBindingHandler bindingHandler = new MyBindingHandler();
        binder.forField(nameField)
                .withValidator(t -> !t.isEmpty(), "No empty values.")
                .withValidationStatusHandler(bindingHandler)
                .bind(Person::getFirstName, Person::setFirstName);

        String ageError = "CONVERSIONERROR";
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(ageError))
                .bind(Person::getAge, Person::setAge);

        binder.setValidationStatusHandler(
                BinderValidationStatus::notifyBindingValidationStatusHandlers);

        String initialName = item.getFirstName();
        int initialAge = item.getAge();

        binder.setBean(item);

        // Test specific error handling.
        bindingHandler.expectingError = true;
        nameField.setValue("");

        // Test default error handling.
        ageField.setValue("foo");
        assertThat("Error message is not what was expected",
                ageField.getErrorMessage(), containsString(ageError));

        // Restore values and test no errors.
        ageField.setValue(String.valueOf(initialAge));
        assertFalse("The field should be valid", ageField.isInvalid());

        bindingHandler.expectingError = false;
        nameField.setValue(initialName);

        // Assert that the handler was called.
        assertEquals(
                "Unexpected callCount to binding validation status handler", 6,
                bindingHandler.callCount);
    }

    @Test
    public void beanvalidation_two_fields_not_equal() {
        TestTextField lastNameField = new TestTextField();
        setBeanValidationFirstNameNotEqualsLastName(nameField, lastNameField);

        item.setLastName("Valid");
        binder.setBean(item);

        Assert.assertFalse("Should not have changes initially",
                binder.hasChanges());
        Assert.assertTrue("Should be ok initially", binder.validate().isOk());
        Assert.assertNotEquals(
                "First name and last name are not same initially",
                item.getFirstName(), item.getLastName());

        nameField.setValue("Invalid");

        Assert.assertFalse("First name change not handled",
                binder.hasChanges());
        Assert.assertTrue(
                "Changing first name to something else than last name should be ok",
                binder.validate().isOk());

        lastNameField.setValue("Invalid");

        Assert.assertTrue("Last name should not be saved yet",
                binder.hasChanges());
        Assert.assertFalse(
                "Binder validation should fail with pending illegal value",
                binder.validate().isOk());
        Assert.assertNotEquals("Illegal last name should not be stored to bean",
                item.getFirstName(), item.getLastName());

        nameField.setValue("Valid");

        Assert.assertFalse("With new first name both changes should be saved",
                binder.hasChanges());
        Assert.assertTrue("Everything should be ok for 'Valid Invalid'",
                binder.validate().isOk());
        Assert.assertNotEquals("First name and last name should never match.",
                item.getFirstName(), item.getLastName());
    }

    @Test
    public void beanvalidation_initially_broken_bean() {
        TestTextField lastNameField = new TestTextField();
        setBeanValidationFirstNameNotEqualsLastName(nameField, lastNameField);

        item.setLastName(item.getFirstName());
        binder.setBean(item);

        Assert.assertFalse(binder.isValid());
        Assert.assertFalse(binder.validate().isOk());
    }

    @Test(expected = IllegalStateException.class)
    public void beanvalidation_isValid_throws_with_readBean() {
        TestTextField lastNameField = new TestTextField();
        setBeanValidationFirstNameNotEqualsLastName(nameField, lastNameField);

        binder.readBean(item);

        Assert.assertTrue(binder.isValid());
    }

    @Test(expected = IllegalStateException.class)
    public void beanvalidation_validate_throws_with_readBean() {
        TestTextField lastNameField = new TestTextField();
        setBeanValidationFirstNameNotEqualsLastName(nameField, lastNameField);

        binder.readBean(item);

        Assert.assertTrue(binder.validate().isOk());
    }

    protected void setBeanValidationFirstNameNotEqualsLastName(
            TestTextField firstNameField, TestTextField lastNameField) {
        binder.bind(firstNameField, Person::getFirstName, Person::setFirstName);
        binder.forField(lastNameField)
                .withValidator(t -> !"foo".equals(t),
                        "Last name cannot be 'foo'")
                .bind(Person::getLastName, Person::setLastName);

        binder.withValidator(p -> !p.getFirstName().equals(p.getLastName()),
                "First name and last name can't be the same");
    }

    @Test
    public void info_validator_not_considered_error() {
        String infoMessage = "Young";
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter("Can't convert"))
                .withValidator(i -> i > 5, infoMessage, ErrorLevel.INFO)
                .bind(Person::getAge, Person::setAge);

        binder.setBean(item);
        ageField.setValue("3");
        Assert.assertEquals(infoMessage, ageField.getErrorMessage());

        Assert.assertEquals(3, item.getAge());
    }

    @Test
    public void two_asRequired_fields_without_initial_values() {
        binder.forField(nameField).asRequired("Empty name").bind(p -> "",
                (p, s) -> {
                });
        binder.forField(ageField).asRequired("Empty age").bind(p -> "",
                (p, s) -> {
                });

        binder.setBean(item);
        assertThat("Initially there should be no errors",
                nameField.getErrorMessage(), isEmptyString());
        assertThat("Initially there should be no errors",
                ageField.getErrorMessage(), isEmptyString());

        nameField.setValue("Foo");
        assertThat("Name with a value should not be an error",
                nameField.getErrorMessage(), isEmptyString());

        assertThat(
                "Age field should not be in error, since it has not been modified.",
                ageField.getErrorMessage(), isEmptyString());

        nameField.setValue("");
        assertNotNull("Empty name should now be in error.",
                nameField.getErrorMessage());

        assertThat("Age field should still be ok.", ageField.getErrorMessage(),
                isEmptyString());
    }

    @Test
    public void refreshValueFromBean() {
        Binding<Person, String> binding = binder.bind(nameField,
                Person::getFirstName, Person::setFirstName);

        binder.readBean(item);

        assertEquals("Name should be read from the item", item.getFirstName(),
                nameField.getValue());

        nameField.setValue("foo");

        assertNotEquals("Name should be different from the item",
                item.getFirstName(), nameField.getValue());

        binding.read(item);

        assertEquals("Name should be read again from the item",
                item.getFirstName(), nameField.getValue());
    }

    @Test(expected = IllegalStateException.class)
    public void bindWithNullSetterSetReadWrite() {
        Binding<Person, String> binding = binder.bind(nameField,
                Person::getFirstName, null);
        binding.setReadOnly(false);
    }

    @Test
    public void bindWithNullSetterShouldMarkFieldAsReadonly() {
        Binding<Person, String> nameBinding = binder.bind(nameField,
                Person::getFirstName, null);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        assertTrue("Name field should be readonly", nameField.isReadOnly());
        assertFalse("Age field should not be readonly", ageField.isReadOnly());
        assertTrue("Binding should be marked readonly",
                nameBinding.isReadOnly());
    }

    @Test
    public void setReadOnly_binding() {
        Binding<Person, String> binding = binder.bind(nameField,
                Person::getFirstName, Person::setFirstName);

        assertFalse("Binding should not be readonly", binding.isReadOnly());
        assertFalse("Name field should not be readonly",
                nameField.isReadOnly());

        binding.setReadOnly(true);
        assertTrue("Binding should be readonly", binding.isReadOnly());
        assertTrue("Name field should be readonly", nameField.isReadOnly());
    }

    @Test
    public void nonSymetricValue_setBean_writtenToBean() {
        binder.bind(nameField, Person::getLastName, Person::setLastName);

        Assert.assertNull(item.getLastName());

        binder.setBean(item);

        Assert.assertEquals("", item.getLastName());
    }

    @Test
    public void nonSymmetricValue_readBean_beanNotTouched() {
        binder.bind(nameField, Person::getLastName, Person::setLastName);
        binder.addValueChangeListener(
                event -> Assert.fail("No value change event should be fired"));

        Assert.assertNull(item.getLastName());

        binder.readBean(item);

        Assert.assertNull(item.getLastName());
    }

    @Test
    public void symetricValue_setBean_beanNotUpdated() {
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);

        binder.setBean(new Person() {
            @Override
            public String getFirstName() {
                return "First";
            }

            @Override
            public void setFirstName(String firstName) {
                Assert.fail("Setter should not be called");
            }
        });
    }

    @Test
    public void conversionWithLocaleBasedErrorMessage() {
        TestTextField ageField = new TestTextField();

        String fiError = "VIRHE";
        String otherError = "ERROR";

        StringToIntegerConverter converter = new StringToIntegerConverter(
                context -> context.getLocale().map(Locale::getLanguage)
                        .orElse("en").equals("fi") ? fiError : otherError);

        binder.forField(ageField).withConverter(converter).bind(Person::getAge,
                Person::setAge);
        binder.setBean(item);

        UI testUI = new UI();
        UI.setCurrent(testUI);

        testUI.add(ageField);

        ageField.setValue("not a number");
        assertEquals(otherError, ageField.getErrorMessage());

        testUI.setLocale(new Locale("fi", "FI"));

        // Re-validate to get the error message with correct locale
        binder.validate();
        assertEquals(fiError, ageField.getErrorMessage());
    }

    @Test
    public void valueChangeListenerOrder() {
        AtomicBoolean beanSet = new AtomicBoolean();
        nameField.addValueChangeListener(e -> {
            if (!beanSet.get()) {
                assertEquals("Value in bean updated earlier than expected",
                        e.getOldValue(), item.getFirstName());
            }
        });
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);
        nameField.addValueChangeListener(e -> {
            if (!beanSet.get()) {
                assertEquals("Value in bean not updated when expected",
                        e.getValue(), item.getFirstName());
            }
        });

        beanSet.set(true);
        binder.setBean(item);
        beanSet.set(false);

        nameField.setValue("Foo");
    }

    @Test
    public void nullRejetingField_nullValue_wrappedExceptionMentionsNullRepresentation() {
        TestTextField field = createNullRejectingFieldWithEmptyValue("");

        Binder<AtomicReference<Integer>> binder = createIntegerConverterBinder(
                field);

        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("null representation");
        exceptionRule.expectCause(CoreMatchers.isA(NullPointerException.class));

        binder.readBean(new AtomicReference<>());
    }

    @Test
    public void nullRejetingField_otherRejectedValue_originalExceptionIsThrown() {
        TestTextField field = createNullRejectingFieldWithEmptyValue("");

        Binder<AtomicReference<Integer>> binder = createIntegerConverterBinder(
                field);

        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("42");

        binder.readBean(new AtomicReference<>(Integer.valueOf(42)));
    }

    @Test
    public void nullAcceptingField_nullValue_originalExceptionIsThrown() {
        /*
         * Edge case with a field that throws for null but has null as the empty
         * value. This is most likely the case if the field doesn't explicitly
         * reject null values but is instead somehow broken so that any value is
         * rejected.
         */
        TestTextField field = createNullRejectingFieldWithEmptyValue(null);

        Binder<AtomicReference<Integer>> binder = createIntegerConverterBinder(
                field);

        exceptionRule.expect(NullPointerException.class);

        binder.readBean(new AtomicReference<>(null));
    }

    private TestTextField createNullRejectingFieldWithEmptyValue(
            String emptyValue) {
        return new TestTextField() {
            @Override
            public void setValue(String value) {
                if (value == null) {
                    throw new NullPointerException("Null value");
                } else if ("42".equals(value)) {
                    throw new IllegalArgumentException("42 is not allowed");
                }
                super.setValue(value);
            }

            @Override
            public String getEmptyValue() {
                return emptyValue;
            }
        };
    }

    private Binder<AtomicReference<Integer>> createIntegerConverterBinder(
            TestTextField field) {
        Binder<AtomicReference<Integer>> binder = new Binder<>();
        binder.forField(field)
                .withConverter(new StringToIntegerConverter("Must have number"))
                .bind(AtomicReference::get, AtomicReference::set);
        return binder;
    }
}

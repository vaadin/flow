/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.StringToBigDecimalConverter;
import com.vaadin.flow.data.converter.StringToDoubleConverter;
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
import static org.junit.Assert.assertThrows;
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
    public void bindingHasChanges_trueWhenFieldValueChanges() {
        var binding = binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);

        binder.readBean(item);
        assertEquals("No name field value", "Johannes", nameField.getValue());
        assertFalse("Field marked as changed after reading bean",
                binder.hasChanges(binding));

        ageField.setValue("99");
        assertFalse("Age field caused name field change",
                binder.hasChanges(binding));

        nameField.setValue("James");
        assertTrue("Binder did not have value changes",
                binder.hasChanges(binding));

        binder.readBean(null);

        assertFalse("Binder has changes after clearing all fields",
                binder.hasChanges(binding));

    }

    @Test
    public void bindingInstanceHasChanges_trueWhenFieldValueChanges() {
        var binding = binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);

        binder.readBean(item);

        assertEquals("No name field value", "Johannes", nameField.getValue());
        assertFalse("Field marked as changed after reading bean",
                binding.hasChanges());

        ageField.setValue("99");
        assertFalse("Age field caused name field change", binding.hasChanges());

        nameField.setValue("James");
        assertTrue("Binder did not have value changes", binding.hasChanges());

        binder.readBean(null);

        assertFalse("Binder has changes after clearing all fields",
                binding.hasChanges());
    }

    @Test
    public void bindingInstanceHasChanges_throwsWhenBinderNotAttached() {
        var binding = binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);

        binder.readBean(item);

        assertFalse("Field marked as changed after reading bean",
                binding.hasChanges());

        nameField.setValue("James");
        assertTrue("Binder did not have value changes", binding.hasChanges());

        binding.unbind();

        assertThrows("Expect unbound binding to throw exception",
                IllegalStateException.class, () -> {
                    binding.hasChanges();
                });
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
    public void bindReadOnly_valueChangesIgnored_fieldIsReadOnly() {
        binder.bindReadOnly(nameField, Person::getFirstName);
        binder.setBean(item);
        nameField.setValue("Artur");
        assertEquals(item.getFirstName(), "Johannes");
        Assert.assertTrue(nameField.isReadOnly());
    }

    @Test
    public void bindReadOnly_proeprtyBinding_valueChangesIgnored_fieldIsReadOnly() {
        binder = new Binder<>(Person.class);
        binder.bindReadOnly(nameField, "firstName");
        binder.setBean(item);
        nameField.setValue("Artur");
        assertEquals(item.getFirstName(), "Johannes");
        Assert.assertTrue(nameField.isReadOnly());
    }

    @Test
    public void bindBindingReadOnly_valueChangesIgnored_fieldIsReadOnly() {
        binder.forField(nameField).bindReadOnly(Person::getFirstName);
        binder.setBean(item);
        nameField.setValue("Artur");
        assertEquals(item.getFirstName(), "Johannes");
        Assert.assertTrue(nameField.isReadOnly());
    }

    @Test
    public void bindBindingReadOnly_proeprtyBinding_valueChangesIgnored_fieldIsReadOnly() {
        binder = new Binder<>(Person.class);
        binder.forField(nameField).bindReadOnly("firstName");
        binder.setBean(item);
        nameField.setValue("Artur");
        assertEquals(item.getFirstName(), "Johannes");
        Assert.assertTrue(nameField.isReadOnly());
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
    public void write_binding_bound_propertyIsUpdated()
            throws ValidationException {
        Binder<Person> binder = new Binder<>();
        Binding<Person, String> binding = binder.bind(nameField,
                Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        Person person = new Person();

        String nameValue = "bar";
        nameField.setValue(nameValue);
        String ageValue = "10";
        ageField.setValue(ageValue);

        person.setFirstName("foo");
        person.setAge(20);

        binder.writeBean(person, Set.of(binding));

        Assert.assertEquals(1, person.getAgeSetterCallcount());
        Assert.assertEquals(nameValue, person.getFirstName());
        Assert.assertNotEquals((int) Integer.valueOf(ageValue),
                person.getAge());
    }

    @Test
    public void write_changedBindings_bound_propertyIsUpdated()
            throws ValidationException {
        Binder<Person> binder = new Binder<>();
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        Person person = new Person();
        Person updatedPerson = new Person();

        String nameValue = "bar";
        nameField.setValue(nameValue);

        person.setFirstName("foo");
        person.setAge(20);

        Assert.assertEquals(1, binder.getChangedBindings().size());
        binder.writeBean(updatedPerson, binder.getChangedBindings());

        Assert.assertEquals(0, updatedPerson.getAgeSetterCallcount());
        Assert.assertEquals(nameValue, updatedPerson.getFirstName());
        Assert.assertEquals(0, updatedPerson.getAge());
    }

    @Test
    public void update_bound_propertyIsUpdated() throws ValidationException {
        Binder<Person> binder = new Binder<>();
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        Person person = new Person();
        Person updatedPerson = new Person();

        String nameValue = "bar";
        nameField.setValue(nameValue);

        person.setFirstName("foo");
        person.setAge(20);

        Assert.assertEquals(1, binder.getChangedBindings().size());

        binder.writeChangedBindingsToBean(updatedPerson);

        Assert.assertEquals(0, updatedPerson.getAgeSetterCallcount());
        Assert.assertEquals(nameValue, updatedPerson.getFirstName());
        Assert.assertEquals(0, updatedPerson.getAge());
    }

    @Test
    public void update_to_initial_value_removes_binding_from_changedBindings_with_set_predicates()
            throws ValidationException {
        Person person = new Person();
        String initialName = "Foo";
        person.setFirstName(initialName);
        person.setAge(20);

        Binder<Person> binder = new Binder<>();
        Binding<Person, String> nameBinding = binder.forField(nameField)
                .withEqualityPredicate(
                        (oldVal, newVal) -> Objects.equals(oldVal, newVal))
                .bind(Person::getFirstName, Person::setFirstName);
        Binding<Person, Integer> ageBinding = binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .withEqualityPredicate(
                        (oldVal, newVal) -> Objects.equals(oldVal, newVal))
                .bind(Person::getAge, Person::setAge);

        binder.readBean(person);
        nameField.setValue("Bar");

        assertEquals(1, binder.getChangedBindings().size());
        assertTrue(binder.getChangedBindings().contains(nameBinding));

        ageField.setValue("21");
        assertEquals(2, binder.getChangedBindings().size());

        nameField.setValue(initialName);

        assertEquals(1, binder.getChangedBindings().size());
        assertTrue(binder.getChangedBindings().contains(ageBinding));

        ageField.setValue("20");
        assertTrue(binder.getChangedBindings().isEmpty());
    }

    @Test
    public void update_to_initial_value_removes_binding_from_changedBindings_with_default_predicates()
            throws ValidationException {
        Person person = new Person();
        String initialName = "Foo";
        person.setFirstName(initialName);
        person.setAge(20);

        Binder<Person> binder = new Binder<>();
        binder.setChangeDetectionEnabled(true);
        Binding<Person, String> nameBinding = binder.forField(nameField)
                .bind(Person::getFirstName, Person::setFirstName);
        Binding<Person, Integer> ageBinding = binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        binder.readBean(person);
        nameField.setValue("Bar");

        assertEquals(1, binder.getChangedBindings().size());
        assertTrue(binder.getChangedBindings().contains(nameBinding));

        ageField.setValue("21");
        assertEquals(2, binder.getChangedBindings().size());

        nameField.setValue(initialName);

        assertEquals(1, binder.getChangedBindings().size());
        assertTrue(binder.getChangedBindings().contains(ageBinding));

        ageField.setValue("20");
        assertTrue(binder.getChangedBindings().isEmpty());
    }

    @Test
    public void update_to_initial_value_does_not_remove_binding_from_changedBindings_by_default()
            throws ValidationException {
        Person person = new Person();
        String initialName = "Foo";
        person.setFirstName(initialName);
        person.setAge(20);

        Binder<Person> binder = new Binder<>();
        Binding<Person, String> nameBinding = binder.forField(nameField)
                .bind(Person::getFirstName, Person::setFirstName);
        Binding<Person, Integer> ageBinding = binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        binder.readBean(person);
        nameField.setValue("Bar");

        assertEquals(1, binder.getChangedBindings().size());
        assertTrue(binder.getChangedBindings().contains(nameBinding));

        ageField.setValue("21");
        assertEquals(2, binder.getChangedBindings().size());
        assertTrue(binder.getChangedBindings().contains(ageBinding));

        nameField.setValue(initialName);

        assertEquals(2, binder.getChangedBindings().size());

        ageField.setValue("20");
        assertEquals(2, binder.getChangedBindings().size());
    }

    @Test
    public void save_bound_beanAsDraft() {
        do_test_save_bound_beanAsDraft(false);
    }

    @Test
    public void save_bound_beanAsDraft_setBean() {
        do_test_save_bound_beanAsDraft(true);
    }

    private void do_test_save_bound_beanAsDraft(boolean setBean) {
        Binder<Person> binder = new Binder<>();
        binder.forField(nameField).withValidator((value, context) -> {
            if (value.equals("Mike")) {
                return ValidationResult.ok();
            } else {
                return ValidationResult.error("value must be Mike");
            }
        }).bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        Person person = new Person();
        if (setBean) {
            binder.setBean(person);
        }

        String fieldValue = "John";
        nameField.setValue(fieldValue);

        int age = 10;
        ageField.setValue("10");

        person.setFirstName("Mark");

        binder.writeBeanAsDraft(person);

        // name is not written to draft as validation / conversion
        // does not pass
        assertNotEquals(fieldValue, person.getFirstName());
        // age is written to draft even if firstname validation
        // fails
        assertEquals(age, person.getAge());

        binder.writeBeanAsDraft(person, true);
        // name is now written despite validation as write was forced
        assertEquals(fieldValue, person.getFirstName());
    }

    @Test
    public void save_bound_bean_disable_validation_binding()
            throws ValidationException {
        Binder<Person> binder = new Binder<>();
        Binding<Person, String> nameBinding = binder.forField(nameField)
                .withValidator((value, context) -> {
                    if (value.equals("Mike"))
                        return ValidationResult.ok();
                    else
                        return ValidationResult.error("value must be Mike");
                }).bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        Person person = new Person();

        String fieldValue = "John";
        nameField.setValue(fieldValue);

        int age = 10;
        ageField.setValue("10");

        person.setFirstName("Mark");

        nameBinding.setValidatorsDisabled(true);
        binder.writeBean(person);

        // name is now written as validation was disabled
        assertEquals(fieldValue, person.getFirstName());
        assertEquals(age, person.getAge());
    }

    @Test
    public void save_bound_bean_disable_validation_binder()
            throws ValidationException {
        Binder<Person> binder = new Binder<>();
        binder.forField(nameField).withValidator((value, context) -> {
            if (value.equals("Mike"))
                return ValidationResult.ok();
            else
                return ValidationResult.error("value must be Mike");
        }).bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        Person person = new Person();

        String fieldValue = "John";
        nameField.setValue(fieldValue);

        int age = 10;
        ageField.setValue("10");

        person.setFirstName("Mark");

        binder.setValidatorsDisabled(true);
        binder.writeBean(person);

        // name is now written as validation was disabled
        assertEquals(fieldValue, person.getFirstName());
        assertEquals(age, person.getAge());
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
    public void withConverter_writeBackValue() {
        TestTextField rentField = new TestTextField();
        rentField.setValue("");
        binder.forField(rentField).withConverter(new EuroConverter(""))
                .withNullRepresentation(BigDecimal.valueOf(0d))
                .bind(Person::getRent, Person::setRent);
        binder.setBean(item);
        rentField.setValue("10");

        assertEquals("€ 10.00", rentField.getValue());
    }

    @Test
    public void withConverter_writeBackValueDisabled() {
        TestTextField rentField = new TestTextField();
        rentField.setValue("");
        Binding<Person, BigDecimal> binding = binder.forField(rentField)
                .withConverter(new EuroConverter(""))
                .withNullRepresentation(BigDecimal.valueOf(0d))
                .bind(Person::getRent, Person::setRent);
        binder.setBean(item);
        binding.setConvertBackToPresentation(false);
        rentField.setValue("10");

        assertNotEquals("€ 10.00", rentField.getValue());
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
    public void withValidator_isAppliedIsEvaluated() {
        // Binding has validator which always fails
        Binding<Person, String> nameBinding = binder.forField(nameField)
                .withValidator(name -> false, "")
                .bind(Person::getFirstName, Person::setFirstName);
        binder.setBean(item);

        // Base state -> not valid
        Assert.assertFalse(binder.isValid());

        // Default -> non-visible field -> valid
        ((Component) nameBinding.getField()).setVisible(false);
        Assert.assertTrue(binder.isValid());

        // isApplied = false -> valid
        ((Component) nameBinding.getField()).setVisible(true);
        nameBinding.setIsAppliedPredicate(p -> false);
        Assert.assertTrue(binder.isValid());

        // isApplied = true -> not valid
        nameBinding.setIsAppliedPredicate(p -> true);
        Assert.assertFalse(binder.isValid());

        // Check removing predicate and restoring default behavior
        nameBinding.setIsAppliedPredicate(null);
        Assert.assertFalse(binder.isValid());
        ((Component) nameBinding.getField()).setVisible(false);
        Assert.assertTrue(binder.isValid());
    }

    @Test
    public void writeBean_isAppliedIsEvaluated() {
        AtomicInteger invokes = new AtomicInteger();

        binder.forField(nameField).withValidator(name -> false, "")
                .bind(Person::getFirstName, Person::setFirstName)
                .setIsAppliedPredicate(p -> {
                    invokes.incrementAndGet();
                    return false;
                });

        binder.readBean(item);
        nameField.setValue("");

        binder.writeBeanIfValid(item);
        Assert.assertEquals("writeBeanIfValid should have invoked isApplied", 1,
                invokes.get());

        binder.writeBeanAsDraft(item);
        Assert.assertEquals("writeBeanAsDraft should have invoked isApplied", 2,
                invokes.get());

    }

    @Test
    public void setRequired_withErrorMessage_fieldGetsRequiredIndicatorAndValidator() {
        TestTextField textField = new TestTextField();
        assertFalse(textField.isRequiredIndicatorVisible());

        BindingBuilder<Person, String> bindingBuilder = binder
                .forField(textField);
        assertFalse(textField.isRequiredIndicatorVisible());

        bindingBuilder.asRequired("foobar");
        assertTrue(textField.isRequiredIndicatorVisible());

        Binding<Person, String> binding = bindingBuilder
                .bind(Person::getFirstName, Person::setFirstName);
        binder.setBean(item);
        assertThat(textField.getErrorMessage(), isEmptyString());
        Assert.assertFalse(textField.isInvalid());

        textField.setValue(textField.getEmptyValue());
        Assert.assertEquals("foobar", componentErrors.get(textField));
        Assert.assertTrue(textField.isInvalid());

        textField.setValue("value");
        assertFalse(textField.isInvalid());
        assertTrue(textField.isRequiredIndicatorVisible());

        binding.setAsRequiredEnabled(false);
        assertFalse(textField.isRequiredIndicatorVisible());
        textField.setValue("");
        assertFalse(textField.isInvalid());
    }

    @Test(expected = IllegalStateException.class)
    public void settingAsRequiredEnabledFalseWhenNoAsRequired() {
        TestTextField textField = new TestTextField();

        BindingBuilder<Person, String> bindingBuilder = binder
                .forField(textField);
        Binding<Person, String> binding = bindingBuilder
                .bind(Person::getFirstName, Person::setFirstName);

        binder.readBean(item);

        // TextField input is not set required, this should trigger
        // IllegalStateExceptipon
        binding.setAsRequiredEnabled(false);
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
        Assert.assertFalse(textField.isInvalid());
        Assert.assertEquals(0, invokes.get());

        textField.setValue(textField.getEmptyValue());
        Assert.assertEquals("foobar", componentErrors.get(textField));
        Assert.assertTrue(textField.isInvalid());
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
        Assert.assertFalse(textField.isInvalid());
        assertEquals(1, invokes.get());

        textField.setValue("        ");
        String errorMessage = textField.getErrorMessage();
        assertNotNull(errorMessage);
        assertEquals("Input is required.", componentErrors.get(textField));
        Assert.assertTrue(textField.isInvalid());
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
        Assert.assertFalse(textField.isInvalid());
        assertEquals(1, invokes.get());
        textField.setValue(" ");
        assertNotNull(textField.getErrorMessage());
        assertEquals("Input required.", componentErrors.get(textField));
        Assert.assertTrue(textField.isInvalid());
        // validation is done for all changed bindings once.
        assertEquals(2, invokes.get());

        textField.setValue("value");
        assertFalse(textField.isInvalid());
        assertTrue(textField.isRequiredIndicatorVisible());
    }

    @Test
    public void setRequiredAsEnabled_shouldNotTriggerValidation() {
        AtomicBoolean hasErrors = new AtomicBoolean();
        // Binding is required but has setAsRequiredEnabled set to false
        Binding<Person, String> nameBinding = binder.forField(nameField)
                .asRequired("Name is required")
                .bind(Person::getFirstName, Person::setFirstName);

        binder.addStatusChangeListener(
                status -> hasErrors.getAndSet(status.hasValidationErrors()));
        binder.setBean(new Person());

        // Base state -> valid
        Assert.assertFalse("binder should not have errors", hasErrors.get());
        Assert.assertEquals("Name field should not be in error.", "",
                nameField.getErrorMessage());
        Assert.assertFalse(nameField.isInvalid());

        // Set setAsRequiredEnabled false -> should still be valid
        nameBinding.setAsRequiredEnabled(false);
        Assert.assertFalse("binder should not have errors", hasErrors.get());
        Assert.assertEquals("Name field should not be in error.", "",
                nameField.getErrorMessage());
        Assert.assertFalse(nameField.isInvalid());

        // Set setAsRequiredEnabled true -> should still be valid
        nameBinding.setAsRequiredEnabled(true);
        Assert.assertFalse("binder should not have errors", hasErrors.get());
        Assert.assertEquals("Name field should not be in error.", "",
                nameField.getErrorMessage());
        Assert.assertFalse(nameField.isInvalid());
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
    public void setReadonly_record_allFieldsAreReadonly() {
        Binder<TestRecord> binder = new Binder<>(TestRecord.class);
        binder.forField(nameField).bind("name");
        binder.forField(ageField).bind("age");

        binder.getBinding("name").ifPresent(b -> b.setReadOnly(true));
        binder.setReadOnly(true);
        assertTrue("Name field should be readonly", nameField.isReadOnly());
        assertTrue("Age field should be readonly", ageField.isReadOnly());

        binder.setReadOnly(false);
        assertFalse("Name field should not be readonly",
                nameField.isReadOnly());
        assertFalse("Age field should not be readonly", ageField.isReadOnly());
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

    @Test
    public void isValidTest_unbound_binder_passes_with_bean_level_validation() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.withValidator(Validator.from(
                person -> !person.getFirstName().equals("fail bean validation"),
                ""));
        Assert.assertTrue(binder.isValid());
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

    @Test
    public void replace_binding_previousBindingUnbound() {
        List<String> bindingCalls = new ArrayList<>();
        Binding<Person, Integer> binding1 = binder.forField(ageField)
                .withConverter(new StringToIntegerConverter("Can't convert"))
                .bind(p -> {
                    bindingCalls.add("READ FIRST");
                    return p.getAge();
                }, (p, v) -> {
                    bindingCalls.add("WRITE FIRST");
                    p.setAge(v);
                });

        binder.setBean(item);
        Assert.assertEquals(List.of("READ FIRST"), bindingCalls);

        bindingCalls.clear();
        ageField.setValue("99");
        Assert.assertEquals(List.of("READ FIRST", "WRITE FIRST"), bindingCalls);

        Binding<Person, Integer> binding2 = binder.forField(ageField)
                .withConverter(new StringToIntegerConverter("Can't convert"))
                .bind(p -> {
                    bindingCalls.add("READ SECOND");
                    return p.getAge();
                }, (p, v) -> {
                    bindingCalls.add("WRITE SECOND");
                    p.setAge(v);
                });

        bindingCalls.clear();
        ageField.setValue("33");
        Assert.assertEquals(List.of("READ SECOND", "WRITE SECOND"),
                bindingCalls);

        assertNull("Expecting first binding to be unbound",
                binding1.getField());
        assertSame("Expecting second binding to be bound", ageField,
                binding2.getField());

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
        Assert.assertTrue(ageField.isInvalid());

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

    @Test
    public void beanvalidation_isValid_passes_with_readBean() {
        TestTextField lastNameField = new TestTextField();
        setBeanValidationFirstNameNotEqualsLastName(nameField, lastNameField);
        binder.withValidator(Validator.alwaysFail("fail"));

        binder.readBean(item);

        Assert.assertTrue(binder.isValid());
    }

    @Test
    public void beanvalidation_validate_passes_with_readBean() {
        TestTextField lastNameField = new TestTextField();
        setBeanValidationFirstNameNotEqualsLastName(nameField, lastNameField);
        binder.withValidator(Validator.alwaysFail("fail"));

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
    public void two_asRequired_fields_without_initial_values_setBean() {
        binder.forField(nameField).asRequired("Empty name").bind(p -> "",
                (p, s) -> {
                });
        binder.forField(ageField).asRequired("Empty age").bind(p -> "",
                (p, s) -> {
                });

        binder.setBean(item);
        assertThat("Initially there should be no errors",
                nameField.getErrorMessage(), isEmptyString());
        Assert.assertFalse(nameField.isInvalid());
        assertThat("Initially there should be no errors",
                ageField.getErrorMessage(), isEmptyString());
        Assert.assertFalse(ageField.isInvalid());

        nameField.setValue("Foo");
        assertThat("Name with a value should not be an error",
                nameField.getErrorMessage(), isEmptyString());
        Assert.assertFalse(nameField.isInvalid());

        assertTrue(
                "Age field should not be in error, since it was not modified.",
                StringUtils.isEmpty(ageField.getErrorMessage()));
        Assert.assertFalse(ageField.isInvalid());

        nameField.setValue("");

        assertFalse("Empty name should now be in error.",
                StringUtils.isEmpty(nameField.getErrorMessage()));
        Assert.assertTrue(nameField.isInvalid());
        assertTrue(
                "Age field should still not be in error, since it was not modified.",
                StringUtils.isEmpty(ageField.getErrorMessage()));
        Assert.assertFalse(ageField.isInvalid());
    }

    @Test
    public void two_asRequired_fields_without_initial_values_readBean() {
        binder.forField(nameField).asRequired("Empty name").bind(p -> "",
                (p, s) -> {
                });
        binder.forField(ageField).asRequired("Empty age").bind(p -> "",
                (p, s) -> {
                });

        binder.readBean(item);
        assertThat("Initially there should be no errors",
                nameField.getErrorMessage(), isEmptyString());
        Assert.assertFalse(nameField.isInvalid());
        assertThat("Initially there should be no errors",
                ageField.getErrorMessage(), isEmptyString());
        Assert.assertFalse(ageField.isInvalid());

        nameField.setValue("Foo");
        assertThat("Name with a value should not be an error",
                nameField.getErrorMessage(), isEmptyString());
        Assert.assertFalse(nameField.isInvalid());

        assertTrue(
                "Age field should not be in error, since it was not modified.",
                StringUtils.isEmpty(ageField.getErrorMessage()));
        Assert.assertFalse(ageField.isInvalid());

        nameField.setValue("");
        assertFalse("Empty name should now be in error.",
                StringUtils.isEmpty(nameField.getErrorMessage()));
        Assert.assertTrue(nameField.isInvalid());

        assertTrue(
                "Age field should still not be in error, since it was not modified.",
                StringUtils.isEmpty(ageField.getErrorMessage()));
        Assert.assertFalse(ageField.isInvalid());
    }

    @Test
    public void validated_and_asRequired_fields_without_initial_values_setBean() {
        binder.forField(nameField).asRequired("Empty name")
                .bind(Person::getFirstName, Person::setFirstName);
        TestTextField lastNameField = new TestTextField();
        binder.forField(lastNameField)
                .withValidator((v, c) -> StringUtils.isEmpty(v)
                        ? ValidationResult.error("Empty last name")
                        : ValidationResult.ok())
                .bind(Person::getLastName, Person::setLastName);

        binder.setBean(item);
        assertFalse("Initially there should be no errors",
                nameField.isInvalid());
        assertFalse("Initially there should be no errors",
                lastNameField.isInvalid());

        nameField.setValue("Foo");
        assertFalse("Name with a value should not be an error",
                nameField.isInvalid());
        assertFalse(
                "Last name field should not be in error, since it was not modified.",
                lastNameField.isInvalid());

        nameField.setValue("");

        assertTrue("Empty name should now be in error.", nameField.isInvalid());
        assertFalse(
                "Last name field should not be in error, since it was not modified.",
                lastNameField.isInvalid());

        nameField.setValue("Bar");
        lastNameField.setValue("Bar");
        lastNameField.setValue("");

        assertFalse("Name with a value should not be an error",
                nameField.isInvalid());
        assertTrue("Empty last name field should now be in error.",
                lastNameField.isInvalid());
    }

    @Test
    public void validated_and_asRequired_fields_without_initial_values_readBean() {
        binder.forField(nameField).asRequired("Empty name")
                .bind(Person::getFirstName, Person::setFirstName);
        TestTextField lastNameField = new TestTextField();
        binder.forField(lastNameField)
                .withValidator((v, c) -> StringUtils.isEmpty(v)
                        ? ValidationResult.error("Empty last name")
                        : ValidationResult.ok())
                .bind(Person::getLastName, Person::setLastName);

        binder.readBean(item);
        assertFalse("Initially there should be no errors",
                nameField.isInvalid());
        assertFalse("Initially there should be no errors",
                lastNameField.isInvalid());

        nameField.setValue("Foo");
        assertFalse("Name with a value should not be an error",
                nameField.isInvalid());
        assertFalse(
                "Last name field should not be in error, since it was not modified.",
                lastNameField.isInvalid());

        nameField.setValue("");

        assertTrue("Empty name should now be in error.", nameField.isInvalid());
        assertFalse(
                "Last name field should not be in error, since it was not modified.",
                lastNameField.isInvalid());

        nameField.setValue("Bar");
        lastNameField.setValue("Bar");
        lastNameField.setValue("");

        assertFalse("Name with a value should not be an error",
                nameField.isInvalid());
        assertTrue("Empty last name field should now be in error.",
                lastNameField.isInvalid());
    }

    @Test
    public void disableDefaultValidators_binderLevel_enabledAndDisabled() {
        TestTextFieldDefaultValidator field1 = new TestTextFieldDefaultValidator(
                (val, ctx) -> ValidationResult.error("fail_1"));
        TestTextFieldDefaultValidator field2 = new TestTextFieldDefaultValidator(
                (val, ctx) -> ValidationResult.error("fail_2"));

        binder.forField(field1).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(field2).bind(Person::getLastName, Person::setLastName);

        // Default behavior -> validators enabled
        BinderValidationStatus<Person> status = binder.validate();
        assertEquals(
                "Validation should have two errors. "
                        + "Default validators should be run.",
                2, status.getValidationErrors().size());

        // Toggle default validators to disabled
        binder.setDefaultValidatorsEnabled(false);
        status = binder.validate();
        Assert.assertTrue(
                "Validation should not have errors. "
                        + "Default validator should be skipped.",
                status.getValidationErrors().isEmpty());
    }

    @Test
    public void disableDefaultValidator_testSingleBindingLevelDisabled_whenBinderLevelEnabled() {
        TestTextFieldDefaultValidator field1 = new TestTextFieldDefaultValidator(
                (val, ctx) -> ValidationResult.error("fail_1"));
        TestTextFieldDefaultValidator field2 = new TestTextFieldDefaultValidator(
                (val, ctx) -> ValidationResult.error("fail_2"));

        binder.forField(field1).bind(Person::getFirstName,
                Person::setFirstName);
        Binding<Person, String> binding = binder.forField(field2)
                .withDefaultValidator(false)
                .bind(Person::getLastName, Person::setLastName);

        // One binding has default validators disabled
        BinderValidationStatus<Person> status = binder.validate();
        assertEquals(
                "Validation should have one error. "
                        + "Only one default validators should be skipped.",
                1, status.getValidationErrors().size());

        // Re-enable default validators of binding
        binding.setDefaultValidatorEnabled(true);
        status = binder.validate();
        assertEquals(
                "Validation should have two errors. "
                        + "Default validators should be run.",
                2, status.getValidationErrors().size());
    }

    @Test
    public void skipDefaultValidator_crossToggleSingleBindingAndBinderState() {
        AtomicBoolean nonSkippedDidRun = new AtomicBoolean(false);

        TestTextFieldDefaultValidator field1 = new TestTextFieldDefaultValidator(
                (val, ctx) -> ValidationResult.error("fail_1"));
        TestTextFieldDefaultValidator field2 = new TestTextFieldDefaultValidator(
                (val, ctx) -> {
                    nonSkippedDidRun.getAndSet(true);
                    return ValidationResult.error("fail_2");
                });

        binder.forField(field1).bind(Person::getFirstName,
                Person::setFirstName);
        Binding<Person, String> binding = binder.forField(field2)
                .withDefaultValidator(true)
                .bind(Person::getLastName, Person::setLastName);

        // Initial case: binder disabled & binding overrides to enable
        binder.setDefaultValidatorsEnabled(false);
        BinderValidationStatus<Person> status = binder.validate();
        assertEquals(
                "Validation should have one error. "
                        + "Only one default validators should be skipped.",
                1, status.getValidationErrors().size());
        assertTrue("Non-skipped validator should have been run.",
                nonSkippedDidRun.get());

        nonSkippedDidRun.getAndSet(false);

        // Cross-toggle false <-> true
        binder.setDefaultValidatorsEnabled(true);
        binding.setDefaultValidatorEnabled(false);
        status = binder.validate();
        assertEquals(
                "Validation should have one error. "
                        + "Only one default validators should be skipped.",
                1, status.getValidationErrors().size());
        assertFalse("Now skipped validator should not have been run.",
                nonSkippedDidRun.get());
    }

    public class TestTextFieldDefaultValidator extends TestTextField
            implements HasValidator<String> {

        private final Validator<String> defaultValidator;

        public TestTextFieldDefaultValidator(
                Validator<String> defaultValidator) {
            this.defaultValidator = defaultValidator;
        }

        @Override
        public Validator<String> getDefaultValidator() {
            return defaultValidator;
        }
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

    @Test
    public void refreshFields_beforeSettingBean_clearsTheFields() {
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);

        assertEquals("Name field should be empty", "", nameField.getValue());

        binder.readBean(item);

        assertEquals("Name should be read from the item", item.getFirstName(),
                nameField.getValue());

        item.setFirstName("bar");
        binder.refreshFields();

        assertEquals("Name field should be cleared since bean is not set", "",
                nameField.getValue());
    }

    @Test
    public void refreshFields_afterSettingBean_readValuesfromBeanAgain() {
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);

        assertEquals("Name field should be empty", "", nameField.getValue());

        binder.readBean(item);

        assertEquals("Name should be read from the item", item.getFirstName(),
                nameField.getValue());

        binder.setBean(item); // refreshFields would read the values again from
                              // bean
        item.setFirstName("bar");
        binder.refreshFields();

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
        Assert.assertTrue(ageField.isInvalid());

        testUI.setLocale(new Locale("fi", "FI"));

        // Re-validate to get the error message with correct locale
        binder.validate();
        assertEquals(fiError, ageField.getErrorMessage());
        Assert.assertTrue(ageField.isInvalid());
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

    @Test
    public void addStatusListenerFromStatusListener_listenerAdded() {
        AtomicBoolean outerListenerInvoked = new AtomicBoolean();
        AtomicBoolean innerListenerInvoked = new AtomicBoolean();

        binder.addStatusChangeListener(event -> {
            if (!outerListenerInvoked.getAndSet(true)) {
                binder.addStatusChangeListener(event2 -> {
                    innerListenerInvoked.set(true);
                });
            }
        });

        // Trigger status change event
        binder.setBean(new Person());

        Assert.assertTrue("Outer listener should be invoked",
                outerListenerInvoked.get());
        Assert.assertFalse("Inner listener should not (yet) be invoked",
                innerListenerInvoked.get());

        // Trigger status change event
        binder.setBean(new Person());

        Assert.assertTrue("Inner listener should be invoked",
                innerListenerInvoked.get());
    }

    @Test
    public void addValueListenerFromStatusListener_listenerAdded() {
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);

        AtomicBoolean outerListenerInvoked = new AtomicBoolean();
        AtomicBoolean innerListenerInvoked = new AtomicBoolean();

        binder.addStatusChangeListener(event -> {
            if (!outerListenerInvoked.getAndSet(true)) {
                binder.addValueChangeListener(event2 -> {
                    innerListenerInvoked.set(true);
                });
            }
        });

        // Trigger status change event
        binder.setBean(new Person());

        Assert.assertTrue("Outer listener should be invoked",
                outerListenerInvoked.get());
        Assert.assertFalse("Inner listener should not (yet) be invoked",
                innerListenerInvoked.get());

        // Trigger value change event
        nameField.setValue("foo");

        Assert.assertTrue("Inner listener should be invoked",
                innerListenerInvoked.get());
    }

    @Test
    public void setBean_readOnlyBinding_propertyBinding_valueIsNotUpdated() {
        Binder<ExampleBean> binder = new Binder<>(ExampleBean.class);

        binder.forField(nameField).withNullRepresentation("")
                .withConverter(new TestConverter()).bind("vals")
                .setReadOnly(true);

        ExampleBean bean = new ExampleBean();
        SubPropClass val = new SubPropClass();
        bean.setVals(val);
        binder.setBean(bean);

        Assert.assertSame(val, bean.getVals());
    }

    @Test
    public void setBean_readOnlyBindingMethod_propertyBinding_valueIsNotUpdated() {
        Binder<ExampleBean> binder = new Binder<>(ExampleBean.class);

        binder.forField(nameField).withNullRepresentation("")
                .withConverter(new TestConverter()).bindReadOnly("vals");

        ExampleBean bean = new ExampleBean();
        SubPropClass val = new SubPropClass();
        bean.setVals(val);
        binder.setBean(bean);

        Assert.assertSame(val, bean.getVals());
        Assert.assertTrue(nameField.isReadOnly());
    }

    @Test
    public void setBean_readOnlyBinding_accessorsBiding_valueIsNotUpdated() {
        Binder<ExampleBean> binder = new Binder<>(ExampleBean.class);

        binder.forField(nameField).withNullRepresentation("")
                .withConverter(new TestConverter())
                .bind(ExampleBean::getVals, ExampleBean::setVals)
                .setReadOnly(true);

        ExampleBean bean = new ExampleBean();
        SubPropClass val = new SubPropClass();
        bean.setVals(val);
        binder.setBean(bean);

        Assert.assertSame(val, bean.getVals());
    }

    @Test
    public void setBean_oneBindingValidationFails_otherBindingValueShouldBeSet() {
        AtomicBoolean validationStatusErrors = new AtomicBoolean();
        AtomicBoolean statusChangeListenerErrors = new AtomicBoolean();
        AtomicInteger validationStatusCalls = new AtomicInteger();
        AtomicInteger statusChangeListenerCalls = new AtomicInteger();

        binder.forField(nameField).withValidator(
                (value, context) -> ValidationResult.error("Always fails"))
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        binder.setBean(item);

        binder.setValidationStatusHandler(status -> {
            validationStatusCalls.incrementAndGet();
            validationStatusErrors.set(status.hasErrors());
        });
        binder.addStatusChangeListener(e -> {
            statusChangeListenerCalls.incrementAndGet();
            statusChangeListenerErrors.set(e.hasValidationErrors());
        });

        ageField.setValue("15");

        assertEquals(
                "Age should have been set regardless of invalid name field.",
                15, item.getAge());
        assertEquals("Validation status should not have errors.", false,
                validationStatusErrors.get());
        assertEquals("Status change listener should not report errors.", false,
                statusChangeListenerErrors.get());
        assertEquals("Validation status should get one call.", 1,
                validationStatusCalls.get());
        assertEquals("Status change listener should get one call.", 1,
                statusChangeListenerCalls.get());
    }

    @Test
    public void setBean_oneBindingRequiredButEmpty_otherBindingValueShouldBeSet() {
        AtomicBoolean validationStatusErrors = new AtomicBoolean();
        AtomicBoolean statusChangeListenerErrors = new AtomicBoolean();
        AtomicInteger validationStatusCalls = new AtomicInteger();
        AtomicInteger statusChangeListenerCalls = new AtomicInteger();

        item.setFirstName(null);

        binder.forField(nameField).asRequired("Name is required")
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        binder.setBean(item);

        binder.setValidationStatusHandler(status -> {
            validationStatusCalls.incrementAndGet();
            validationStatusErrors.set(status.hasErrors());
        });
        binder.addStatusChangeListener(e -> {
            statusChangeListenerCalls.incrementAndGet();
            statusChangeListenerErrors.set(e.hasValidationErrors());
        });

        ageField.setValue("15");

        assertEquals(
                "Age should have been set regardless of required but empty name field.",
                15, item.getAge());
        assertEquals("Validation status should not have errors.", false,
                validationStatusErrors.get());
        assertEquals("Status change listener should not report errors.", false,
                statusChangeListenerErrors.get());
        assertEquals("Validation status should get one call.", 1,
                validationStatusCalls.get());
        assertEquals("Status change listener should get one call.", 1,
                statusChangeListenerCalls.get());
    }

    @Test
    public void setBean_binderValidationFails_noValueShouldBeSet() {
        AtomicBoolean validationStatusErrors = new AtomicBoolean();
        AtomicBoolean statusChangeListenerErrors = new AtomicBoolean();
        AtomicInteger validationStatusCalls = new AtomicInteger();
        AtomicInteger statusChangeListenerCalls = new AtomicInteger();

        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        binder.withValidator(
                (value, context) -> ValidationResult.error("Always fails"));
        binder.setBean(item);

        binder.setValidationStatusHandler(status -> {
            validationStatusCalls.incrementAndGet();
            validationStatusErrors.set(status.hasErrors());
        });
        binder.addStatusChangeListener(e -> {
            statusChangeListenerCalls.incrementAndGet();
            statusChangeListenerErrors.set(e.hasValidationErrors());
        });

        ageField.setValue("15");

        assertEquals(
                "Age should not have been set since binder validation fails.",
                32, item.getAge());
        assertEquals("Validation status should have errors.", true,
                validationStatusErrors.get());
        assertEquals("Status change listener should report errors.", true,
                statusChangeListenerErrors.get());
        assertEquals("Validation status should get one call.", 1,
                validationStatusCalls.get());
        assertEquals("Status change listener should get one call.", 1,
                statusChangeListenerCalls.get());
    }

    @Test
    public void invalidUsage_modifyFieldsInsideValidator_binderDoesNotThrow() {
        TestTextField field = new TestTextField();

        AtomicBoolean validatorIsExecuted = new AtomicBoolean();
        binder.forField(field).asRequired().withValidator((val, context) -> {
            nameField.setValue("foo");
            ageField.setValue("bar");
            validatorIsExecuted.set(true);
            return ValidationResult.ok();
        }).bind(Person::getEmail, Person::setEmail);

        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(ageField).bind(Person::getLastName,
                Person::setLastName);

        binder.setBean(new Person());

        field.setValue("baz");
        // mostly self control, the main check is: not exception is thrown
        Assert.assertTrue(validatorIsExecuted.get());
    }

    @Test
    public void validationShouldNotRunTwice() {
        TestTextField salaryField = new TestTextField();
        AtomicInteger count = new AtomicInteger(0);
        item.setSalaryDouble(100d);
        binder.forField(salaryField)
                .withConverter(new StringToDoubleConverter(""))
                .bind(Person::getSalaryDouble, Person::setSalaryDouble);
        binder.setBean(item);
        binder.addValueChangeListener(event -> {
            count.incrementAndGet();
        });

        salaryField.setValue("1000");
        assertTrue(binder.isValid());
        assertEquals(1, count.get());

        salaryField.setValue("salary");
        assertFalse(binder.isValid());
        assertEquals(2, count.get());

        salaryField.setValue("2000");

        // Without fix for #12356 count will be 5
        assertEquals(3, count.get());

        assertEquals(Double.valueOf(2000), item.getSalaryDouble());
    }

    @Test
    public void validationShouldNotRunTwiceWhenWriting() {
        TestTextField nameField = new TestTextField();
        AtomicInteger count = new AtomicInteger(0);
        binder.forField(nameField).withValidator((value, context) -> {
            count.incrementAndGet();
            if (value.equals("Mike")) {
                return ValidationResult.ok();
            } else {
                return ValidationResult.error("value must be Mike");
            }
        }).bind(Person::getFirstName, Person::setFirstName);
        binder.readBean(item);
        nameField.setValue("Mike");
        assertEquals("Validation should be run only once for value change", 1,
                count.get());
        try {
            binder.writeBean(item);
        } catch (ValidationException e) {
        }
        assertEquals("Validation should be run only once for writing the bean",
                2, count.get());
    }

    @Test
    public void setValidationErrorHandler_handlerIsSet_handlerMethodsAreCalled() {
        TestTextField testField = new TestTextField();

        class TestErrorHandler implements BinderValidationErrorHandler {

            private ValidationResult result;
            private boolean clearIsCalled;

            @Override
            public void handleError(HasValue<?, ?> field,
                    ValidationResult result) {
                Assert.assertSame(testField, field);
                this.result = result;
                clearIsCalled = false;
            }

            @Override
            public void clearError(HasValue<?, ?> field) {
                Assert.assertSame(testField, field);
                result = null;
                clearIsCalled = true;
            }
        }
        ;

        TestErrorHandler handler = new TestErrorHandler();
        binder.setValidationErrorHandler(handler);

        binder.forField(testField).asRequired()
                .withValidator((val, context) -> {
                    if ("bar".equals(val)) {
                        return ValidationResult.error("foo");
                    }
                    return ValidationResult.ok();
                }).bind(Person::getFirstName, Person::setFirstName);
        binder.setBean(new Person());

        testField.setValue("bar");

        Assert.assertTrue(handler.result.isError());
        Assert.assertFalse(handler.clearIsCalled);

        testField.setValue("foo");

        Assert.assertNull(handler.result);
        Assert.assertTrue(handler.clearIsCalled);

        Assert.assertSame(handler, binder.getValidationErrorHandler());
    }

    @Test(expected = BindingException.class)
    public void readBean_converterThrows_readBean_exceptionHandlerSet_bindingExceptionIsThrown() {
        TestTextField testField = new TestTextField();
        setExceptionHandler();

        binder.forField(testField).withConverter(Converter
                .<String, String> from(name -> Result.ok(name), name -> {
                    throw new NullPointerException();
                })).bind(Person::getFirstName, Person::setFirstName);

        binder.readBean(new Person());
    }

    @Test(expected = BindingException.class)
    public void readBean_getterThrows_exceptionHandlerSet_bindingExceptionIsThrown() {
        TestTextField testField = new TestTextField();

        setExceptionHandler();

        binder.forField(testField).bind(person -> {
            throw new NullPointerException();
        }, Person::setFirstName);

        binder.readBean(new Person());
    }

    @Test(expected = BindingException.class)
    public void setBean_converterThrows_setBean_exceptionHandlerSet_bindingExceptionIsThrown() {
        TestTextField testField = new TestTextField();

        setExceptionHandler();

        binder.forField(testField)
                .withConverter(Converter.<String, String> from(name -> {
                    throw new NullPointerException();
                }, name -> name))
                .bind(Person::getFirstName, Person::setFirstName);

        binder.setBean(new Person());
    }

    @Test(expected = BindingException.class)
    public void setBean_setterThrows_exceptionHandlerSet_bindingExceptionIsThrown() {
        TestTextField testField = new TestTextField();
        setExceptionHandler();

        binder.forField(testField).bind(Person::getFirstName,
                (person, field) -> {
                    throw new NullPointerException();
                });

        binder.setBean(new Person());
    }

    @Test(expected = BindingException.class)
    public void setBean_setValueThrows_exceptionHandlerSet_bindingExceptionIsThrown() {
        TestTextField testField = new ThrowingSetter();
        setExceptionHandler();

        binder.forField(testField).bind(Person::getFirstName,
                Person::setFirstName);

        binder.setBean(new Person());
    }

    @Test(expected = BindingException.class)
    public void writeBean_converterThrows_exceptionHandlerSet_bindingExceptionIsThrown()
            throws ValidationException {
        TestTextField testField = new TestTextField();

        setExceptionHandler();

        binder.forField(testField)
                .withConverter(Converter.<String, String> from(name -> {
                    throw new NullPointerException();
                }, name -> name))
                .bind(Person::getFirstName, Person::setFirstName);

        binder.writeBean(new Person());
    }

    @Test(expected = BindingException.class)
    public void writeBean_setterThrows_exceptionHandlerSet_bindingExceptionIsThrown()
            throws ValidationException {
        TestTextField testField = new TestTextField();
        setExceptionHandler();

        binder.forField(testField).bind(Person::getFirstName,
                (person, field) -> {
                    throw new NullPointerException();
                });

        Person person = new Person();
        person.setFirstName("foo");
        binder.writeBean(person);
    }

    @Test(expected = BindingException.class)
    public void writeBean_setValueThrows_exceptionHandlerSet_bindingExceptionIsThrown()
            throws ValidationException {
        TestTextField testField = new ThrowingSetter();
        setExceptionHandler();

        binder.forField(testField)
                .withConverter(Converter.<String, String> from(
                        name -> Result.ok(name), name -> "foo"))
                .bind(Person::getFirstName, Person::setFirstName);

        binder.writeBean(new Person());
    }

    @Test(expected = BindingException.class)
    public void writeBean_getValueThrows_exceptionHandlerSet_bindingExceptionIsThrown()
            throws ValidationException {
        TestTextField testField = new ThrowingGetter();
        setExceptionHandler();

        binder.forField(testField).bind(Person::getFirstName,
                Person::setFirstName);

        binder.writeBean(new Person());
    }

    @Test(expected = BindingException.class)
    public void readBean_converterThrows_exceptionHandlerSet_bindingExceptionIsThrown() {
        TestTextField testField = new TestTextField();
        setExceptionHandler();

        binder.forField(testField)
                .withConverter(Converter.<String, String> from(name -> {
                    throw new NullPointerException();
                }, name -> name))
                .bind(Person::getFirstName, Person::setFirstName)
                .read(new Person());

    }

    @Test(expected = BindingException.class)
    public void bindingReadBean_setValueThrows_exceptionHandlerSet_bindingExceptionIsThrown() {
        TestTextField testField = new ThrowingSetter();
        setExceptionHandler();

        binder.forField(testField)
                .bind(Person::getFirstName, Person::setFirstName)
                .read(new Person());

    }

    @Test(expected = BindingException.class)
    public void bindingReadBean_converterThrows_exceptionHandlerSet_bindingExceptionIsThrown() {
        TestTextField testField = new TestTextField();
        setExceptionHandler();

        binder.forField(testField).withConverter(Converter
                .<String, String> from(name -> Result.ok(name), name -> {
                    throw new NullPointerException();
                })).bind(Person::getFirstName, Person::setFirstName)
                .read(new Person());

    }

    @Test
    public void getBindingExceptionHandler_defaultHandlerIsReturned() {
        BindingExceptionHandler exceptionHandler = binder
                .getBindingExceptionHandler();
        Assert.assertTrue(
                exceptionHandler instanceof DefaultBindingExceptionHandler);
    }

    private void setExceptionHandler() {
        BindingException bindingException = new BindingException("foo");
        binder.setBindingExceptionHandler(
                (field, exception) -> Optional.of(bindingException));
    }

    // See: https://github.com/vaadin/framework/issues/9581
    @Test
    public void withConverter_hasChangesFalse() {
        TestTextField nameField = new TestTextField();
        nameField.setValue("");
        TestTextField rentField = new TestTextField();
        rentField.setValue("");
        rentField.addValueChangeListener(event -> {
            nameField.setValue("Name");
        });
        item.setRent(BigDecimal.valueOf(10));
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(rentField).withConverter(new EuroConverter(""))
                .withNullRepresentation(BigDecimal.valueOf(0d))
                .bind(Person::getRent, Person::setRent);
        binder.readBean(item);

        assertFalse(binder.hasChanges());
        assertEquals("€ 10.00", rentField.getValue());
        assertEquals("Name", nameField.getValue());
    }

    public record TestRecord(String name, int age) {
    }

    @Test
    public void readRecord_writeRecord() throws ValidationException {
        Binder<TestRecord> binder = new Binder<>(TestRecord.class);

        TestTextField nameField = new TestTextField();
        nameField.setValue("");
        TestTextField ageField = new TestTextField();
        ageField.setValue("");

        binder.forField(ageField)
                .withConverter(
                        new StringToIntegerConverter(0, "Failed to convert"))
                .bind("age");
        binder.forField(nameField).bind("name");
        binder.readBean(new TestRecord("test", 42));

        // Check that fields are enabled for records
        Assert.assertFalse(nameField.isReadOnly());
        Assert.assertFalse(ageField.isReadOnly());

        // Check valid record writing
        nameField.setValue("foo");
        ageField.setValue("50");
        TestRecord testRecord = binder.writeRecord();
        Assert.assertEquals("foo", testRecord.name);
        Assert.assertEquals(50, testRecord.age);

        // Check that invalid record writing fails
        ageField.setValue("invalid value");
        assertThrows(ValidationException.class, () -> {
            TestRecord failedRecord = binder.writeRecord();
        });
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

    public static class ExampleBean implements Serializable {
        private SubPropClass vals;

        public SubPropClass getVals() {
            return vals;
        }

        public void setVals(SubPropClass vals) {
            this.vals = vals;
        }
    }

    public static class SubPropClass implements Serializable {
        private String val1 = "Val1";

        @Override
        public String toString() {
            return val1;
        }
    }

    public static class TestConverter
            implements Converter<String, SubPropClass> {

        @Override
        public Result<SubPropClass> convertToModel(String value,
                ValueContext context) {
            return Result.ok(null);
        }

        @Override
        public String convertToPresentation(SubPropClass value,
                ValueContext context) {
            return value != null ? value.toString() : null;
        }
    }

    /**
     * A converter that adds/removes the euro sign and formats currencies with
     * two decimal places.
     */
    public class EuroConverter extends StringToBigDecimalConverter {

        public EuroConverter() {
            super("defaultErrorMessage");
        }

        public EuroConverter(String errorMessage) {
            super(errorMessage);
        }

        @Override
        public Result<BigDecimal> convertToModel(String value,
                ValueContext context) {
            if (value.isEmpty()) {
                return Result.ok(null);
            }
            value = value.replaceAll("[€\\s]", "").trim();
            if (value.isEmpty()) {
                value = "0";
            }
            return super.convertToModel(value, context);
        }

        @Override
        public String convertToPresentation(BigDecimal value,
                ValueContext context) {
            if (value == null) {
                return convertToPresentation(BigDecimal.ZERO, context);
            }
            return "€ " + super.convertToPresentation(value, context);
        }

        @Override
        protected NumberFormat getFormat(Locale locale) {
            // Always display currency with two decimals
            NumberFormat format = super.getFormat(Locale.ENGLISH);
            if (format instanceof DecimalFormat) {
                ((DecimalFormat) format).setMaximumFractionDigits(2);
                ((DecimalFormat) format).setMinimumFractionDigits(2);
            }
            return format;
        }
    }

    private static class ThrowingSetter extends TestTextField {
        @Override
        public void setValue(String value) {
            throw new NullPointerException();
        }
    }

    private static class ThrowingGetter extends TestTextField {
        @Override
        public String getValue() {
            throw new NullPointerException();
        }
    }
}

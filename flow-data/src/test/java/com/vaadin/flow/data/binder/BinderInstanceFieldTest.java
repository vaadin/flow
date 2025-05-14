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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.data.binder.testcomponents.TestDatePicker;
import com.vaadin.flow.data.binder.testcomponents.TestFormLayout;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.tests.data.bean.Address;
import com.vaadin.flow.tests.data.bean.ConvertibleValues;
import com.vaadin.flow.tests.data.bean.Person;

public class BinderInstanceFieldTest {

    public static class BindAllFields extends TestFormLayout {
        private TestTextField firstName;
        private TestDatePicker birthDate;
    }

    public static class BindFieldsUsingAnnotation extends TestFormLayout {
        @PropertyId("firstName")
        private TestTextField nameField;

        @PropertyId("birthDate")
        private TestDatePicker birthDateField;
    }

    public static class BindNestedFieldsUsingAnnotation extends TestFormLayout {
        @PropertyId("address.streetAddress")
        private TestTextField streetAddressField;
    }

    public static class BindDeepNestedFieldsUsingAnnotation
            extends TestFormLayout {
        @PropertyId("first.address.streetAddress")
        private TestTextField firstStreetField;

        @PropertyId("second.address.streetAddress")
        private TestTextField secondStreetField;
    }

    public static class BindDeepNestingFieldsWithCircularStructure
            extends TestFormLayout {
        @PropertyId("child.name")
        private TestTextField childName;

        @PropertyId("child.child.name")
        private TestTextField grandchildName;

        @PropertyId("child.child.child.child.child.child.child.child.name")
        private TestTextField eighthLevelGrandchildName;

        @PropertyId("child.child.child.child.child.child.child.child.child.child.child.child.child.name")
        private TestTextField distantGreatGrandchildName;
    }

    public static class BindOnlyOneField extends TestFormLayout {
        private TestTextField firstName;
        private TestTextField noFieldInPerson;
    }

    public static class BindWithNoFieldInPerson extends TestFormLayout {
        private TestTextField firstName;
        private TestDatePicker birthDate;
        private TestTextField noFieldInPerson;
    }

    public static class BindFieldHasWrongType extends TestFormLayout {
        private String firstName;
        private TestDatePicker birthDate;
    }

    public static class BindGenericField extends TestFormLayout {
        private CustomField<String> firstName;
    }

    public static class BindGenericWrongTypeParameterField
            extends TestFormLayout {
        private CustomField<Boolean> firstName;
    }

    public static class BindWrongTypeParameterField extends TestFormLayout {
        private IntegerTextField firstName;
    }

    public static class BindOneFieldRequiresConverter extends TestFormLayout {
        private TestTextField firstName;
        private TestTextField age;
    }

    public static class BindAutomaticConverter extends TestFormLayout {
        private TestDatePicker localDateToDate;
        private TestTextField stringToBigDecimal;
        private TestTextField stringToBigInteger;
        private TestTextField stringToBoolean;
        private TestTextField stringToPrimitiveBoolean;
        private TestTextField stringToDouble;
        private TestTextField stringToPrimitiveDouble;
        private TestTextField stringToFloat;
        private TestTextField stringToPrimitiveFloat;
        private TestTextField stringToInteger;
        private TestTextField stringToPrimitiveInteger;
        private TestTextField stringToLong;
        private TestTextField stringToPrimitiveLong;
        private TestTextField stringToUUID;
    }

    public static class BindGeneric<T> extends TestFormLayout {
        private CustomField<T> firstName;
    }

    public static class BindRaw extends TestFormLayout {
        private CustomField firstName;
    }

    public static abstract class AbstractTextField extends Component
            implements HasValue<ValueChangeEvent<String>, String> {

    }

    public static class BindAbstract extends TestFormLayout {
        private AbstractTextField firstName;
    }

    public static class BindNonInstantiatableType extends TestFormLayout {
        private NoDefaultCtor firstName;
    }

    public static class BindComplextHierarchyGenericType
            extends TestFormLayout {
        private ComplexHierarchy firstName;
    }

    public static class NoDefaultCtor extends TestTextField {
        public NoDefaultCtor(int arg) {
        }
    }

    public static class IntegerTextField extends CustomField<Integer> {

    }

    public static class ComplexHierarchy extends Generic<Long> {

    }

    public static class Generic<T> extends ComplexGeneric<Boolean, String, T> {

    }

    public static class ComplexGeneric<U, V, S> extends CustomField<V> {

    }

    @Tag("input")
    public static class CustomField<T>
            extends AbstractField<CustomField<T>, T> {

        public CustomField() {
            super(null);
        }

        @Override
        protected void setPresentationValue(T newPresentationValue) {

        }

    }

    final static class Couple {
        Person first;
        Person second;

        public Person getFirst() {
            return first;
        }

        public Person getSecond() {
            return second;
        }

        public void setFirst(Person first) {
            this.first = first;
        }

        public void setSecond(Person second) {
            this.second = second;
        }
    }

    final class NestingStructure {
        NestingStructure child;
        String name;

        public NestingStructure getChild() {
            return child;
        }

        public void setChild(NestingStructure child) {
            this.child = child;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void bindInstanceFields_bindAllFields() {
        BindAllFields form = new BindAllFields();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);

        Person person = new Person();
        person.setFirstName("foo");
        person.setBirthDate(LocalDate.now());

        binder.setBean(person);

        Assert.assertEquals(person.getFirstName(), form.firstName.getValue());
        Assert.assertEquals(person.getBirthDate(), form.birthDate.getValue());

        form.firstName.setValue("bar");
        form.birthDate.setValue(person.getBirthDate().plusDays(345));

        Assert.assertEquals(form.firstName.getValue(), person.getFirstName());
        Assert.assertEquals(form.birthDate.getValue(), person.getBirthDate());
    }

    @Test(expected = IllegalStateException.class)
    public void bind_instanceFields_noArgsConstructor() {
        BindAllFields form = new BindAllFields();
        Binder<Person> binder = new Binder<>();
        binder.bindInstanceFields(form);
    }

    @Test
    public void bindInstanceFields_bindOnlyOneFields() {
        BindOnlyOneField form = new BindOnlyOneField();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);

        Person person = new Person();
        person.setFirstName("foo");

        binder.setBean(person);

        Assert.assertEquals(person.getFirstName(), form.firstName.getValue());

        Assert.assertNull(form.noFieldInPerson);

        form.firstName.setValue("bar");

        Assert.assertEquals(form.firstName.getValue(), person.getFirstName());
    }

    @Test
    public void bindInstanceFields_bindNotHasValueField_fieldIsNull() {
        BindFieldHasWrongType form = new BindFieldHasWrongType();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);

        Person person = new Person();
        person.setFirstName("foo");

        binder.setBean(person);

        Assert.assertNull(form.firstName);
    }

    @Test
    public void bindInstanceFields_genericField() {
        BindGenericField form = new BindGenericField();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);

        Person person = new Person();
        person.setFirstName("foo");

        binder.setBean(person);

        Assert.assertEquals(person.getFirstName(), form.firstName.getValue());

        form.firstName.setValue("bar");

        Assert.assertEquals(form.firstName.getValue(), person.getFirstName());
    }

    @Test(expected = IllegalStateException.class)
    public void bindInstanceFields_genericFieldWithWrongTypeParameter() {
        BindGenericWrongTypeParameterField form = new BindGenericWrongTypeParameterField();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);
    }

    @Test(expected = IllegalStateException.class)
    public void bindInstanceFields_generic() {
        BindGeneric<String> form = new BindGeneric<>();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);
    }

    @Test(expected = IllegalStateException.class)
    public void bindInstanceFields_rawFieldType() {
        BindRaw form = new BindRaw();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);
    }

    @Test(expected = IllegalStateException.class)
    public void bindInstanceFields_abstractFieldType() {
        BindAbstract form = new BindAbstract();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);
    }

    @Test(expected = IllegalStateException.class)
    public void bindInstanceFields_noInstantiatableFieldType() {
        BindNonInstantiatableType form = new BindNonInstantiatableType();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);
    }

    @Test(expected = IllegalStateException.class)
    public void bindInstanceFields_wrongFieldType() {
        BindWrongTypeParameterField form = new BindWrongTypeParameterField();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);
    }

    @Test
    public void bindInstanceFields_complexGenericHierarchy() {
        BindComplextHierarchyGenericType form = new BindComplextHierarchyGenericType();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);

        Person person = new Person();
        person.setFirstName("foo");

        binder.setBean(person);

        Assert.assertEquals(person.getFirstName(), form.firstName.getValue());

        form.firstName.setValue("bar");

        Assert.assertEquals(form.firstName.getValue(), person.getFirstName());
    }

    @Test
    public void bindInstanceFields_bindNotHasValueField_fieldIsNotReplaced() {
        BindFieldHasWrongType form = new BindFieldHasWrongType();
        Binder<Person> binder = new Binder<>(Person.class);

        String name = "foo";
        form.firstName = name;

        Person person = new Person();
        person.setFirstName("foo");

        binder.setBean(person);

        Assert.assertEquals(name, form.firstName);
    }

    @Test
    public void bindInstanceFields_bindAllFieldsUsingAnnotations() {
        BindFieldsUsingAnnotation form = new BindFieldsUsingAnnotation();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.bindInstanceFields(form);

        Person person = new Person();
        person.setFirstName("foo");
        person.setBirthDate(LocalDate.now());

        binder.setBean(person);

        Assert.assertEquals(person.getFirstName(), form.nameField.getValue());
        Assert.assertEquals(person.getBirthDate(),
                form.birthDateField.getValue());

        form.nameField.setValue("bar");
        form.birthDateField.setValue(person.getBirthDate().plusDays(345));

        Assert.assertEquals(form.nameField.getValue(), person.getFirstName());
        Assert.assertEquals(form.birthDateField.getValue(),
                person.getBirthDate());
    }

    @Test
    public void bindInstanceFields_bindNestedFieldUsingAnnotation() {
        BindNestedFieldsUsingAnnotation form = new BindNestedFieldsUsingAnnotation();
        Binder<Person> binder = new Binder<>(Person.class, true);
        binder.bindInstanceFields(form);

        Person person = new Person();
        Address address = new Address();
        address.setStreetAddress("Foo st.");
        person.setAddress(address);

        binder.setBean(person);

        Assert.assertEquals("Reading nested properties bound using annotation",
                person.getAddress().getStreetAddress(),
                form.streetAddressField.getValue());

        form.streetAddressField.setValue("Bar ave.");
        Assert.assertEquals("Changing nested properties bound using annotation",
                form.streetAddressField.getValue(),
                person.getAddress().getStreetAddress());
    }

    @Test
    public void bindInstanceFields_bindDeepNestedFieldsUsingAnnotation() {
        BindDeepNestedFieldsUsingAnnotation form = new BindDeepNestedFieldsUsingAnnotation();
        Binder<Couple> binder = new Binder<>(Couple.class, true);
        binder.bindInstanceFields(form);
        Person first = new Person();
        Person second = new Person();
        Address firstAddress = new Address();
        firstAddress.setStreetAddress("Foo st.");
        first.setAddress(firstAddress);
        Address secondAddress = new Address();
        second.setAddress(secondAddress);
        secondAddress.setStreetAddress("Bar ave.");
        Couple couple = new Couple();
        couple.setFirst(first);
        couple.setSecond(second);

        binder.setBean(couple);

        Assert.assertEquals("Binding deep nested properties using annotation",
                couple.first.getAddress().getStreetAddress(),
                form.firstStreetField.getValue());
        Assert.assertEquals(
                "Binding parallel deep nested properties using annotation",
                couple.second.getAddress().getStreetAddress(),
                form.secondStreetField.getValue());

        form.firstStreetField.setValue(second.getAddress().getStreetAddress());
        Assert.assertEquals("Updating value in deep nested properties",
                form.firstStreetField.getValue(),
                first.getAddress().getStreetAddress());
    }

    @Test
    public void bindInstanceFields_circular() {
        BindDeepNestingFieldsWithCircularStructure form = new BindDeepNestingFieldsWithCircularStructure();
        Binder<NestingStructure> binder = new Binder<>(NestingStructure.class,
                true);
        binder.bindInstanceFields(form);
        NestingStructure parent = new NestingStructure();
        parent.setName("parent");
        NestingStructure child = new NestingStructure();
        child.setName("child");
        parent.setChild(child);
        NestingStructure grandchild = new NestingStructure();
        grandchild.setName("grandchild");
        child.setChild(grandchild);
        NestingStructure root = grandchild;
        for (int i = 1; i < 15; i++) {
            NestingStructure ns = new NestingStructure();
            ns.setName("great " + root.getName());
            root.setChild(ns);
            root = ns;
        }
        binder.setBean(parent);
        Assert.assertEquals(child.getName(), form.childName.getValue());
        Assert.assertEquals(grandchild.getName(),
                form.grandchildName.getValue());
        Assert.assertNotNull(
                "Reading nested properties within default supported nested depth (max 10 levels)",
                form.eighthLevelGrandchildName);
        // only 10 levels of nesting properties are scanned by default
        Assert.assertNull(
                "By default, only 10 levels of nesting properties are scanned.",
                form.distantGreatGrandchildName);
    }

    @Test
    public void bindInstanceFields_customNestingLevel() {
        BindDeepNestingFieldsWithCircularStructure form = new BindDeepNestingFieldsWithCircularStructure();
        int customScanningDepth = 5;
        PropertyFilterDefinition shallowFilter = new PropertyFilterDefinition(
                customScanningDepth, Arrays.asList("java.lang"));
        Binder<NestingStructure> binder = new Binder<>(BeanPropertySet
                .get(NestingStructure.class, true, shallowFilter));
        binder.bindInstanceFields(form);
        NestingStructure parent = new NestingStructure();
        parent.setName("parent");
        NestingStructure child = new NestingStructure();
        child.setName("child");
        parent.setChild(child);
        NestingStructure grandchild = new NestingStructure();
        grandchild.setName("grandchild");
        child.setChild(grandchild);
        NestingStructure root = grandchild;
        for (int i = 1; i < 15; i++) {
            NestingStructure ns = new NestingStructure();
            ns.setName("great " + root.getName());
            root.setChild(ns);
            root = ns;
        }
        binder.setBean(parent);
        Assert.assertEquals(child.getName(), form.childName.getValue());
        Assert.assertEquals(
                "Reading 3rd level nesting works when custom scanning depth is 5",
                grandchild.getName(), form.grandchildName.getValue());
        Assert.assertNull(
                "Reading eighth level nesting doesn't work when custom scanning depth is 5",
                form.eighthLevelGrandchildName);
    }

    @Test
    public void bindInstanceFields_bindNotBoundFieldsOnly_customBindingIsNotReplaced() {
        BindAllFields form = new BindAllFields();
        Binder<Person> binder = new Binder<>(Person.class);

        TestTextField name = new TestTextField();
        form.firstName = name;
        binder.forField(form.firstName)
                .withValidator(
                        new StringLengthValidator("Name is invalid", 3, 10))
                .bind("firstName");

        binder.bindInstanceFields(form);

        Person person = new Person();
        String personName = "foo";
        person.setFirstName(personName);
        person.setBirthDate(LocalDate.now());

        binder.setBean(person);

        Assert.assertEquals(person.getFirstName(), form.firstName.getValue());
        Assert.assertEquals(person.getBirthDate(), form.birthDate.getValue());
        // the instance is not overridden
        Assert.assertEquals(name, form.firstName);

        // Test automatic binding
        form.birthDate.setValue(person.getBirthDate().plusDays(345));
        Assert.assertEquals(form.birthDate.getValue(), person.getBirthDate());

        // Test custom binding
        form.firstName.setValue("aa");
        Assert.assertEquals(personName, person.getFirstName());

        Assert.assertFalse(binder.validate().isOk());
    }

    @Test
    public void bindInstanceFields_fieldsAreConfigured_customBindingIsNotReplaced() {
        BindWithNoFieldInPerson form = new BindWithNoFieldInPerson();
        Binder<Person> binder = new Binder<>(Person.class);

        TestTextField name = new TestTextField();
        form.firstName = name;
        binder.forField(form.firstName)
                .withValidator(
                        new StringLengthValidator("Name is invalid", 3, 10))
                .bind("firstName");
        TestTextField ageField = new TestTextField();
        form.noFieldInPerson = ageField;
        binder.forField(form.noFieldInPerson)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);

        binder.bindInstanceFields(form);

        Person person = new Person();
        String personName = "foo";
        int age = 11;
        person.setFirstName(personName);
        person.setAge(age);

        binder.setBean(person);

        Assert.assertEquals(person.getFirstName(), form.firstName.getValue());
        Assert.assertEquals(String.valueOf(person.getAge()),
                form.noFieldInPerson.getValue());
        // the instances are not overridden
        Assert.assertEquals(name, form.firstName);
        Assert.assertEquals(ageField, form.noFieldInPerson);

        // Test correct age
        age += 56;
        form.noFieldInPerson.setValue(String.valueOf(age));
        Assert.assertEquals(form.noFieldInPerson.getValue(),
                String.valueOf(person.getAge()));

        // Test incorrect name
        form.firstName.setValue("aa");
        Assert.assertEquals(personName, person.getFirstName());

        Assert.assertFalse(binder.validate().isOk());
    }

    @Test
    public void bindInstanceFields_preconfiguredFieldNotBoundToPropertyPreserved() {
        BindOneFieldRequiresConverter form = new BindOneFieldRequiresConverter();
        form.age = new TestTextField();
        form.firstName = new TestTextField();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.forField(form.age)
                .withConverter(str -> Integer.parseInt(str) / 2,
                        integer -> Integer.toString(integer * 2))
                .bind(Person::getAge, Person::setAge);
        binder.bindInstanceFields(form);
        Person person = new Person();
        person.setFirstName("first");
        person.setAge(45);
        binder.setBean(person);
        Assert.assertEquals("90", form.age.getValue());
    }

    @Test
    public void bindInstanceFields_explicitelyBoundFieldAndNotBoundField() {
        BindOnlyOneField form = new BindOnlyOneField();
        Binder<Person> binder = new Binder<>(Person.class);

        binder.forField(new TestTextField()).bind("firstName");

        binder.bindInstanceFields(form);
    }

    @Test
    public void bindInstanceFields_tentativelyBoundFieldAndNotBoundField() {
        BindOnlyOneField form = new BindOnlyOneField();
        Binder<Person> binder = new Binder<>(Person.class);

        TestTextField field = new TestTextField();
        form.firstName = field;

        // This is an incomplete binding which is supposed to be configured
        // manually
        binder.forMemberField(field);

        // bindInstanceFields will not complain even though it can't bind
        // anything as there is a binding in progress (an exception will be
        // thrown later if the binding is not completed)
        binder.bindInstanceFields(form);
    }

    @Test
    public void bindInstanceFields_fieldsNeedConversion_knownConvertersApplied() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        try {
            BindAutomaticConverter form = new BindAutomaticConverter();
            form.stringToInteger = new TestTextField();
            form.localDateToDate = new TestDatePicker();
            form.stringToBigDecimal = new TestTextField();
            form.stringToBigInteger = new TestTextField();
            form.stringToBoolean = new TestTextField();
            form.stringToPrimitiveBoolean = new TestTextField();
            form.stringToDouble = new TestTextField();
            form.stringToPrimitiveDouble = new TestTextField();
            form.stringToFloat = new TestTextField();
            form.stringToPrimitiveFloat = new TestTextField();
            form.stringToInteger = new TestTextField();
            form.stringToPrimitiveInteger = new TestTextField();
            form.stringToLong = new TestTextField();
            form.stringToPrimitiveLong = new TestTextField();
            form.stringToUUID = new TestTextField();

            Binder<ConvertibleValues> binder = new Binder<>(
                    ConvertibleValues.class);
            binder.bindInstanceFields(form);

            LocalDate now = LocalDate.of(2022, 3, 27);
            UUID uuid = UUID.randomUUID();

            ConvertibleValues data = new ConvertibleValues();
            data.setStringToBigDecimal(new BigDecimal("20.23"));
            data.setStringToBigInteger(new BigInteger("30"));
            data.setStringToDouble(40.56);
            data.setStringToPrimitiveDouble(50.78);
            data.setStringToFloat(60.23f);
            data.setStringToPrimitiveFloat(70.12f);
            data.setStringToInteger(80);
            data.setStringToPrimitiveInteger(90);
            data.setStringToLong(100L);
            data.setStringToPrimitiveLong(110);
            data.setStringToBoolean(true);
            data.setStringToPrimitiveBoolean(false);
            data.setLocalDateToDate(java.sql.Date.valueOf(now));
            data.setStringToUUID(uuid);

            binder.setBean(data);

            Assert.assertEquals("20.23", form.stringToBigDecimal.getValue());
            Assert.assertEquals("30", form.stringToBigInteger.getValue());
            Assert.assertEquals("40.56", form.stringToDouble.getValue());
            Assert.assertEquals("50.78",
                    form.stringToPrimitiveDouble.getValue());
            Assert.assertEquals("60.23", form.stringToFloat.getValue());
            Assert.assertEquals("70.12",
                    form.stringToPrimitiveFloat.getValue());
            Assert.assertEquals("80", form.stringToInteger.getValue());
            Assert.assertEquals("90", form.stringToPrimitiveInteger.getValue());
            Assert.assertEquals("100", form.stringToLong.getValue());
            Assert.assertEquals("110", form.stringToPrimitiveLong.getValue());
            Assert.assertEquals("true", form.stringToBoolean.getValue());
            Assert.assertEquals("false",
                    form.stringToPrimitiveBoolean.getValue());
            Assert.assertEquals(now, form.localDateToDate.getValue());
            Assert.assertEquals(uuid.toString(), form.stringToUUID.getValue());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void bindInstanceFields_fieldsNeedConversion_nullRepresentationIsConfigured() {
        BindAutomaticConverter form = new BindAutomaticConverter();
        form.stringToInteger = new TestTextField() {
            @Override
            public String getEmptyValue() {
                return "EMPTY";
            }
        };

        Binder<ConvertibleValues> binder = new Binder<>(
                ConvertibleValues.class);
        binder.bindInstanceFields(form);

        ConvertibleValues data = new ConvertibleValues();
        binder.setBean(data);

        Assert.assertEquals("EMPTY", form.stringToInteger.getValue());
    }

    @Test
    public void bindInstanceFields_incompleteBinding_converterNotAppliedAutomatically() {
        BindOneFieldRequiresConverter form = new BindOneFieldRequiresConverter();
        form.age = new TestTextField();
        Binder<Person> binder = new Binder<>(Person.class);
        Binder.BindingBuilder<Person, Integer> ageBinding = binder
                .forField(form.age)
                .withConverter(str -> Integer.parseInt(str) / 2,
                        integer -> Integer.toString(integer * 2));
        binder.bindInstanceFields(form);

        Assert.assertFalse(
                "Expecting incomplete binding to be ignored by Binder, but field was bound",
                binder.getBinding("age").isPresent());

        ageBinding.bind(Person::getAge, Person::setAge);

        Person person = new Person();
        person.setAge(45);
        binder.setBean(person);
        Assert.assertEquals("90", form.age.getValue());
    }

    @Test
    public void bindInstanceFields_customBindingAfterInvoke_automaticBindingOverwritten() {
        BindOnlyOneField form = new BindOnlyOneField();
        form.firstName = new TestTextField();
        Binder<Person> binder = new Binder<>(Person.class);

        binder.bindInstanceFields(form);
        Binder.BindingBuilder<Person, String> binding = binder
                .forField(form.firstName)
                .withConverter(str -> str.substring(str.length() / 2),
                        str -> str + str);
        binding.bind(Person::getFirstName, Person::setFirstName);

        Person person = new Person();
        person.setFirstName("Hello!");
        binder.setBean(person);
        Assert.assertEquals("Hello!Hello!", form.firstName.getValue());
    }

    @Test
    public void bindInstanceFields_incompleteBindingBoundAfterInvoke_automaticBindingOverwritten() {
        BindOnlyOneField form = new BindOnlyOneField();
        form.firstName = new TestTextField();
        Binder<Person> binder = new Binder<>(Person.class);

        Binder.BindingBuilder<Person, String> binding = binder
                .forField(form.firstName)
                .withConverter(str -> str.substring(str.length() / 2),
                        str -> str + str);
        binder.bindInstanceFields(form);
        binding.bind(Person::getFirstName, Person::setFirstName);

        Person person = new Person();
        person.setFirstName("Hello!");
        binder.setBean(person);
        Assert.assertEquals("Hello!Hello!", form.firstName.getValue());
    }

    @Test
    public void bindInstanceFields_incompleteBinding_fieldIgnored() {
        BindOnlyOneField form = new BindOnlyOneField();
        form.firstName = new TestTextField();
        Binder<Person> binder = new Binder<>(Person.class);

        binder.forField(form.firstName).withConverter(
                str -> str.substring(str.length() / 2), str -> str + str);
        binder.bindInstanceFields(form);

        Assert.assertFalse(
                "Expecting incomplete binding to be ignored by Binder, but field was bound",
                binder.getBinding("firstName").isPresent());
    }

}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.BeanBinderTest.RequiredConstraints.SubConstraint;
import com.vaadin.flow.data.binder.BeanBinderTest.RequiredConstraints.SubSubConstraint;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.tests.data.bean.BeanToValidate;

public class BeanBinderTest
        extends BinderTestBase<Binder<BeanToValidate>, BeanToValidate> {

    private enum TestEnum {
    }

    private class TestClass {
        private TestTextField number = new TestTextField();
    }

    private class TestClassWithoutFields {
    }

    private static class TestBean implements Serializable {
        private Set<TestEnum> enums;
        private int number;

        public Set<TestEnum> getEnums() {
            return enums;
        }

        public void setEnums(Set<TestEnum> enums) {
            this.enums = enums;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }

    public static class RequiredConstraints implements Serializable {
        @NotNull
        @Max(10)
        private String firstname;

        @Size(min = 3, max = 16)
        @Digits(integer = 3, fraction = 2)
        private String age;

        @NotEmpty
        private String lastname;

        private SubConstraint subfield;

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public String getAge() {
            return age;
        }

        public void setAge(String age) {
            this.age = age;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public SubConstraint getSubfield() {
            return subfield;
        }

        public void setSubfield(SubConstraint subfield) {
            this.subfield = subfield;
        }

        public static class SubConstraint implements Serializable {

            @NotNull
            @NotEmpty
            @Size(min = 5)
            private String name;

            private SubSubConstraint subsub;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public SubSubConstraint getSubsub() {
                return subsub;
            }

            public void setSubsub(SubSubConstraint subsub) {
                this.subsub = subsub;
            }

        }

        public static class SubSubConstraint implements Serializable {

            @Size(min = 10)
            private String value;

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

        }
    }

    public static class Person {
        LocalDate mydate;

        public LocalDate getMydate() {
            return mydate;
        }

        public void setMydate(LocalDate mydate) {
            this.mydate = mydate;
        }
    }

    public static class PersonForm {
        private TestTextField mydate = new TestTextField();
    }

    private UI ui;

    @Before
    public void setUp() {
        ui = new UI();
        ui.setLocale(Locale.ENGLISH);
        UI.setCurrent(ui);

        binder = new BeanValidationBinder<>(BeanToValidate.class);
        item = new BeanToValidate();
        item.setFirstname("Johannes");
        item.setAge(32);
    }

    @After
    public void tearDown() {
        UI.setCurrent(null);
    }

    @Test(expected = IllegalStateException.class)
    public void bindInstanceFields_does_not_automatically_bind_incomplete_forField_bindings() {
        Binder<TestBean> otherBinder = new Binder<>(TestBean.class);
        TestClass testClass = new TestClass();

        otherBinder.forField(testClass.number)
                .withConverter(new StringToIntegerConverter(""));

        // Should throw an IllegalStateException since the binding for number is
        // not completed with bind
        otherBinder.bindInstanceFields(testClass);
    }

    @Test(expected = IllegalStateException.class)
    public void bindInstanceFields_throw_if_no_fields_bound() {
        Binder<TestBean> otherBinder = new Binder<>(TestBean.class);
        TestClassWithoutFields testClass = new TestClassWithoutFields();

        // Should throw an IllegalStateException no fields are bound
        otherBinder.bindInstanceFields(testClass);
    }

    @Test
    public void bindInstanceFields_does_not_throw_if_fields_are_bound_manually() {
        PersonForm form = new PersonForm();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.forMemberField(form.mydate)
                .withConverter(str -> LocalDate.now(), date -> "Hello")
                .bind("mydate");
        binder.bindInstanceFields(form);

    }

    @Test
    public void bindInstanceFields_does_not_throw_if_there_are_incomplete_bindings() {
        PersonForm form = new PersonForm();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.forMemberField(form.mydate).withConverter(str -> LocalDate.now(),
                date -> "Hello");
        binder.bindInstanceFields(form);
    }

    @Test(expected = IllegalStateException.class)
    public void incomplete_forMemberField_bindings() {
        Binder<TestBean> otherBinder = new Binder<>(TestBean.class);
        TestClass testClass = new TestClass();

        otherBinder.forMemberField(testClass.number)
                .withConverter(new StringToIntegerConverter(""));

        // Should throw an IllegalStateException since the forMemberField
        // binding has not been completed
        otherBinder.setBean(new TestBean());
    }

    @Test
    public void fieldBound_bindBean_fieldValueUpdated() {
        binder.bind(nameField, "firstname");
        binder.setBean(item);

        assertEquals("Johannes", nameField.getValue());
    }

    @Test
    public void beanBound_bindField_fieldValueUpdated() {
        binder.setBean(item);
        binder.bind(nameField, "firstname");

        assertEquals("Johannes", nameField.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void bindInvalidPropertyName_throws() {
        binder.bind(nameField, "firstnaem");
    }

    @Test(expected = NullPointerException.class)
    public void bindNullPropertyName_throws() {
        binder.bind(nameField, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bindNonReadableProperty_throws() {
        binder.bind(nameField, "writeOnlyProperty");
    }

    @Test
    public void beanBound_setValidFieldValue_propertyValueChanged() {
        binder.setBean(item);
        binder.bind(nameField, "firstname");

        nameField.setValue("Henri");

        assertEquals("Henri", item.getFirstname());
    }

    @Test
    public void readOnlyPropertyBound_setFieldValue_ignored() {
        binder.bind(nameField, "readOnlyProperty");
        binder.setBean(item);

        String propertyValue = item.getReadOnlyProperty();
        nameField.setValue("Foo");

        assertEquals(propertyValue, item.getReadOnlyProperty());
    }

    @Test
    public void bindReadOnlyPropertyShouldMarkFieldAsReadonly() {
        binder.bind(nameField, "readOnlyProperty");

        assertTrue("Name field should be readonly", nameField.isReadOnly());
    }

    @Test
    public void setReadonlyShouldIgnoreBindingsForReadOnlyProperties() {
        binder.bind(nameField, "readOnlyProperty");

        binder.setReadOnly(true);
        assertTrue("Name field should be ignored and be readonly",
                nameField.isReadOnly());

        binder.setReadOnly(false);
        assertTrue("Name field should be ignored and be readonly",
                nameField.isReadOnly());

        nameField.setReadOnly(false);
        binder.setReadOnly(true);
        assertFalse("Name field should be ignored and not be readonly",
                nameField.isReadOnly());

        binder.setReadOnly(false);
        assertFalse("Name field should be ignored and not be readonly",
                nameField.isReadOnly());
    }

    @Test
    public void beanBound_setInvalidFieldValue_validationError() {
        binder.setBean(item);
        binder.bind(nameField, "firstname");

        nameField.setValue("H"); // too short

        assertEquals("Johannes", item.getFirstname());
        assertInvalid(nameField, "size must be between 3 and 16");
    }

    @Test
    public void beanNotBound_setInvalidFieldValue_validationError() {
        binder.bind(nameField, "firstname");

        nameField.setValue("H"); // too short

        assertInvalid(nameField, "size must be between 3 and 16");
    }

    @Test
    public void explicitValidatorAdded_setInvalidFieldValue_explicitValidatorRunFirst() {
        binder.forField(nameField).withValidator(name -> name.startsWith("J"),
                "name must start with J").bind("firstname");

        nameField.setValue("A");

        assertInvalid(nameField, "name must start with J");
    }

    @Test
    public void explicitValidatorAdded_setInvalidFieldValue_beanValidatorRun() {
        binder.forField(nameField).withValidator(name -> name.startsWith("J"),
                "name must start with J").bind("firstname");

        nameField.setValue("J");

        assertInvalid(nameField, "size must be between 3 and 16");
    }

    @Test(expected = ClassCastException.class)
    public void fieldWithIncompatibleTypeBound_bindBean_throws() {
        binder.bind(ageField, "age");
        binder.setBean(item);
    }

    @Test(expected = ClassCastException.class)
    public void fieldWithIncompatibleTypeBound_loadBean_throws() {
        binder.bind(ageField, "age");
        binder.readBean(item);
    }

    @Test(expected = ClassCastException.class)
    public void fieldWithIncompatibleTypeBound_saveBean_throws()
            throws Throwable {
        try {
            binder.bind(ageField, "age");
            binder.writeBean(item);
        } catch (RuntimeException e) {
            throw e.getCause();
        }
    }

    @Test
    public void fieldWithConverterBound_bindBean_fieldValueUpdated() {
        binder.forField(ageField)
                .withConverter(Integer::valueOf, String::valueOf).bind("age");
        binder.setBean(item);

        assertEquals("32", ageField.getValue());
    }

    @Test(expected = ClassCastException.class)
    public void fieldWithInvalidConverterBound_bindBean_fieldValueUpdated() {
        binder.forField(ageField).withConverter(Float::valueOf, String::valueOf)
                .bind("age");
        binder.setBean(item);

        assertEquals("32", ageField.getValue());
    }

    @Test
    public void beanBinderWithBoxedType() {
        binder.forField(ageField)
                .withConverter(Integer::valueOf, String::valueOf).bind("age");
        binder.setBean(item);

        ageField.setValue(String.valueOf(20));
        assertEquals(20, item.getAge());
    }

    @Test
    public void firstName_isNotNullConstraint_nullableFieldIsRequired() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        RequiredConstraints bean = new RequiredConstraints();

        TestTextField field = new TestTextField() {
            @Override
            public String getEmptyValue() {
                return null;
            }
        };
        binder.bind(field, "firstname");
        binder.setBean(bean);

        Assert.assertTrue(
                "@NotNull field with default value null should be required",
                field.isRequiredIndicatorVisible());
        testSerialization(binder);
    }

    @Test
    public void firstName_isNotNullConstraint_textFieldIsNotRequired() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        RequiredConstraints bean = new RequiredConstraints();

        TestTextField field = new TestTextField();
        binder.bind(field, "firstname");
        binder.setBean(bean);

        Assert.assertFalse(
                "@NotNull field with default value \"\" should not be required",
                field.isRequiredIndicatorVisible());
        testSerialization(binder);
    }

    @Test
    public void age_minSizeConstraint_fieldIsRequired() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        RequiredConstraints bean = new RequiredConstraints();

        TestTextField field = new TestTextField();
        binder.bind(field, "age");
        binder.setBean(bean);

        Assert.assertTrue(field.isRequiredIndicatorVisible());
        testSerialization(binder);
    }

    @Test
    public void lastName_minSizeConstraint_fieldIsRequired() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        RequiredConstraints bean = new RequiredConstraints();

        TestTextField field = new TestTextField();
        binder.bind(field, "lastname");
        binder.setBean(bean);

        Assert.assertTrue(field.isRequiredIndicatorVisible());
        testSerialization(binder);
    }

    @Test
    public void subfield_name_fieldIsRequired() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        RequiredConstraints bean = new RequiredConstraints();
        bean.setSubfield(new RequiredConstraints.SubConstraint());

        TestTextField field = new TestTextField();
        binder.bind(field, "subfield.name");
        binder.setBean(bean);

        Assert.assertTrue(field.isRequiredIndicatorVisible());
        testSerialization(binder);
    }

    @Test
    public void subsubfield_name_fieldIsRequired() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        RequiredConstraints bean = new RequiredConstraints();
        RequiredConstraints.SubConstraint subfield = new RequiredConstraints.SubConstraint();
        subfield.setSubsub(new SubSubConstraint());
        bean.setSubfield(subfield);

        TestTextField field = new TestTextField();
        binder.bind(field, "subfield.subsub.value");
        binder.setBean(bean);

        Assert.assertTrue(field.isRequiredIndicatorVisible());
        testSerialization(binder);
    }

    @Test
    public void subfield_name_valueCanBeValidated() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        TestTextField field = new TestTextField();

        binder.bind(field, "subfield.name");
        RequiredConstraints bean = new RequiredConstraints();
        bean.setSubfield(new SubConstraint());
        binder.setBean(bean);
        Assert.assertFalse(binder.validate().isOk());
        field.setValue("overfive");
        Assert.assertTrue(binder.validate().isOk());
    }

    @Test
    public void subSubfield_name_valueCanBeValidated() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        TestTextField field = new TestTextField();

        binder.bind(field, "subfield.subsub.value");
        RequiredConstraints bean = new RequiredConstraints();
        SubConstraint subfield = new SubConstraint();
        bean.setSubfield(subfield);
        subfield.setSubsub(new SubSubConstraint());
        binder.setBean(bean);

        Assert.assertFalse(binder.validate().isOk());
        field.setValue("overtencharacters");
        Assert.assertTrue(binder.validate().isOk());
    }

    private void assertInvalid(TestTextField field, String message) {
        BinderValidationStatus<?> status = binder.validate();
        List<BindingValidationStatus<?>> errors = status
                .getFieldValidationErrors();
        assertEquals(1, errors.size());
        assertSame(field, errors.get(0).getField());
        assertEquals(message, errors.get(0).getMessage().get());
        assertInvalidField(message, field);
    }
}

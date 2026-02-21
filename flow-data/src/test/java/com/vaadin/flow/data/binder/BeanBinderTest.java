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

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.BeanBinderTest.RequiredConstraints.SubConstraint;
import com.vaadin.flow.data.binder.BeanBinderTest.RequiredConstraints.SubSubConstraint;
import com.vaadin.flow.data.binder.testcomponents.TestSelectComponent;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.tests.data.bean.BeanToValidate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanBinderTest
        extends BinderTestBase<Binder<BeanToValidate>, BeanToValidate> {

    private enum TestEnum {
    }

    private class TestClass {
        private TestSelectComponent<TestEnum> enums;
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

    @BeforeEach
    void setUp() {
        ui = new UI();
        ui.setLocale(Locale.ENGLISH);
        UI.setCurrent(ui);

        binder = new BeanValidationBinder<>(BeanToValidate.class);
        item = new BeanToValidate();
        item.setFirstname("Johannes");
        item.setAge(32);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void bindInstanceFields_parameters_type_erased() {
        Binder<TestBean> otherBinder = new Binder<>(TestBean.class);
        TestClass testClass = new TestClass();
        otherBinder.forField(testClass.number)
                .withConverter(new StringToIntegerConverter("")).bind("number");

        // Should correctly bind the enum field without throwing
        otherBinder.bindInstanceFields(testClass);
        testSerialization(otherBinder);
    }

    @Test
    void bindInstanceFields_automatically_binds_incomplete_forMemberField_bindings() {
        Binder<TestBean> otherBinder = new Binder<>(TestBean.class);
        TestClass testClass = new TestClass();

        otherBinder.forMemberField(testClass.number)
                .withConverter(new StringToIntegerConverter(""));
        otherBinder.bindInstanceFields(testClass);

        TestBean bean = new TestBean();
        otherBinder.setBean(bean);
        testClass.number.setValue("50");
        assertEquals(50, bean.number);
        testSerialization(otherBinder);
    }

    @Test
    void bindInstanceFields_does_not_automatically_bind_incomplete_forField_bindings() {
        assertThrows(IllegalStateException.class, () -> {
            Binder<TestBean> otherBinder = new Binder<>(TestBean.class);
            TestClass testClass = new TestClass();

            otherBinder.forField(testClass.number)
                    .withConverter(new StringToIntegerConverter(""));

            // bindInstanceFields does not throw exceptions for incomplete
            // bindings
            // because bindings they can be completed after the call.
            otherBinder.bindInstanceFields(testClass);
            // Should throw an IllegalStateException since the binding for
            // number is
            // not completed with bind
            otherBinder.setBean(new TestBean());
        });
    }

    @Test
    void bindInstanceFields_throw_if_no_fields_bound() {
        assertThrows(IllegalStateException.class, () -> {
            Binder<TestBean> otherBinder = new Binder<>(TestBean.class);
            TestClassWithoutFields testClass = new TestClassWithoutFields();

            // Should throw an IllegalStateException no fields are bound
            otherBinder.bindInstanceFields(testClass);
        });
    }

    @Test
    void bindInstanceFields_does_not_throw_if_fields_are_bound_manually() {
        PersonForm form = new PersonForm();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.forMemberField(form.mydate)
                .withConverter(str -> LocalDate.now(), date -> "Hello")
                .bind("mydate");
        binder.bindInstanceFields(form);

    }

    @Test
    void bindInstanceFields_does_not_throw_if_there_are_incomplete_bindings() {
        PersonForm form = new PersonForm();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.forMemberField(form.mydate).withConverter(str -> LocalDate.now(),
                date -> "Hello");
        binder.bindInstanceFields(form);
    }

    @Test
    void incomplete_forMemberField_bindings() {
        assertThrows(IllegalStateException.class, () -> {
            Binder<TestBean> otherBinder = new Binder<>(TestBean.class);
            TestClass testClass = new TestClass();

            otherBinder.forMemberField(testClass.number)
                    .withConverter(new StringToIntegerConverter(""));

            // Should throw an IllegalStateException since the forMemberField
            // binding has not been completed
            otherBinder.setBean(new TestBean());
        });
    }

    @Test
    void fieldBound_bindBean_fieldValueUpdated() {
        binder.bind(nameField, "firstname");
        binder.setBean(item);

        assertEquals("Johannes", nameField.getValue());
    }

    @Test
    void beanBound_bindField_fieldValueUpdated() {
        binder.setBean(item);
        binder.bind(nameField, "firstname");

        assertEquals("Johannes", nameField.getValue());
    }

    @Test
    void bindInvalidPropertyName_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> binder.bind(nameField, "firstnaem"));
    }

    @Test
    void bindNullPropertyName_throws() {
        assertThrows(NullPointerException.class,
                () -> binder.bind(nameField, null));
    }

    @Test
    void bindNonReadableProperty_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> binder.bind(nameField, "writeOnlyProperty"));
    }

    @Test
    void beanBound_setValidFieldValue_propertyValueChanged() {
        binder.setBean(item);
        binder.bind(nameField, "firstname");

        nameField.setValue("Henri");

        assertEquals("Henri", item.getFirstname());
    }

    @Test
    void readOnlyPropertyBound_setFieldValue_ignored() {
        binder.bind(nameField, "readOnlyProperty");
        binder.setBean(item);

        String propertyValue = item.getReadOnlyProperty();
        nameField.setValue("Foo");

        assertEquals(propertyValue, item.getReadOnlyProperty());
    }

    @Test
    void bindReadOnlyPropertyShouldMarkFieldAsReadonly() {
        binder.bind(nameField, "readOnlyProperty");

        assertTrue(nameField.isReadOnly(), "Name field should be readonly");
    }

    @Test
    void setReadonlyShouldIgnoreBindingsForReadOnlyProperties() {
        binder.bind(nameField, "readOnlyProperty");

        binder.setReadOnly(true);
        assertTrue(nameField.isReadOnly(),
                "Name field should be ignored and be readonly");

        binder.setReadOnly(false);
        assertTrue(nameField.isReadOnly(),
                "Name field should be ignored and be readonly");

        nameField.setReadOnly(false);
        binder.setReadOnly(true);
        assertFalse(nameField.isReadOnly(),
                "Name field should be ignored and not be readonly");

        binder.setReadOnly(false);
        assertFalse(nameField.isReadOnly(),
                "Name field should be ignored and not be readonly");
    }

    @Test
    void beanBound_setInvalidFieldValue_validationError() {
        binder.setBean(item);
        binder.bind(nameField, "firstname");

        nameField.setValue("H"); // too short

        assertEquals("Johannes", item.getFirstname());
        assertInvalid(nameField, "size must be between 3 and 16");
    }

    @Test
    void beanNotBound_setInvalidFieldValue_validationError() {
        binder.bind(nameField, "firstname");

        nameField.setValue("H"); // too short

        assertInvalid(nameField, "size must be between 3 and 16");
    }

    @Test
    void explicitValidatorAdded_setInvalidFieldValue_explicitValidatorRunFirst() {
        binder.forField(nameField).withValidator(name -> name.startsWith("J"),
                "name must start with J").bind("firstname");

        nameField.setValue("A");

        assertInvalid(nameField, "name must start with J");
    }

    @Test
    void explicitValidatorAdded_setInvalidFieldValue_beanValidatorRun() {
        binder.forField(nameField).withValidator(name -> name.startsWith("J"),
                "name must start with J").bind("firstname");

        nameField.setValue("J");

        assertInvalid(nameField, "size must be between 3 and 16");
    }

    @Test
    void fieldWithIncompatibleTypeBound_bindBean_throws() {
        assertThrows(ClassCastException.class, () -> {
            binder.bind(ageField, "age");
            binder.setBean(item);
        });
    }

    @Test
    void fieldWithIncompatibleTypeBound_loadBean_throws() {
        assertThrows(ClassCastException.class, () -> {
            binder.bind(ageField, "age");
            binder.readBean(item);
        });
    }

    @Test
    void fieldWithIncompatibleTypeBound_saveBean_throws() throws Throwable {
        assertThrows(ClassCastException.class, () -> {
            try {
                binder.bind(ageField, "age");
                binder.writeBean(item);
            } catch (RuntimeException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    void fieldWithConverterBound_bindBean_fieldValueUpdated() {
        binder.forField(ageField)
                .withConverter(Integer::valueOf, String::valueOf).bind("age");
        binder.setBean(item);

        assertEquals("32", ageField.getValue());
    }

    @Test
    void fieldWithInvalidConverterBound_bindBean_fieldValueUpdated() {
        assertThrows(ClassCastException.class, () -> {
            binder.forField(ageField)
                    .withConverter(Float::valueOf, String::valueOf).bind("age");
            binder.setBean(item);

            assertEquals("32", ageField.getValue());
        });
    }

    @Test
    void beanBinderWithBoxedType() {
        binder.forField(ageField)
                .withConverter(Integer::valueOf, String::valueOf).bind("age");
        binder.setBean(item);

        ageField.setValue(String.valueOf(20));
        assertEquals(20, item.getAge());
    }

    @Test
    void firstName_isNotNullConstraint_nullableFieldIsRequired() {
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

        assertTrue(field.isRequiredIndicatorVisible(),
                "@NotNull field with default value null should be required");
        testSerialization(binder);
    }

    @Test
    void firstName_isNotNullConstraint_textFieldIsNotRequired() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        RequiredConstraints bean = new RequiredConstraints();

        TestTextField field = new TestTextField();
        binder.bind(field, "firstname");
        binder.setBean(bean);

        assertFalse(field.isRequiredIndicatorVisible(),
                "@NotNull field with default value \"\" should not be required");
        testSerialization(binder);
    }

    @Test
    void age_minSizeConstraint_fieldIsRequired() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        RequiredConstraints bean = new RequiredConstraints();

        TestTextField field = new TestTextField();
        binder.bind(field, "age");
        binder.setBean(bean);

        assertTrue(field.isRequiredIndicatorVisible());
        testSerialization(binder);
    }

    @Test
    void lastName_minSizeConstraint_fieldIsRequired() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        RequiredConstraints bean = new RequiredConstraints();

        TestTextField field = new TestTextField();
        binder.bind(field, "lastname");
        binder.setBean(bean);

        assertTrue(field.isRequiredIndicatorVisible());
        testSerialization(binder);
    }

    @Test
    void subfield_name_fieldIsRequired() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        RequiredConstraints bean = new RequiredConstraints();
        bean.setSubfield(new RequiredConstraints.SubConstraint());

        TestTextField field = new TestTextField();
        binder.bind(field, "subfield.name");
        binder.setBean(bean);

        assertTrue(field.isRequiredIndicatorVisible());
        testSerialization(binder);
    }

    @Test
    void subsubfield_name_fieldIsRequired() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        RequiredConstraints bean = new RequiredConstraints();
        RequiredConstraints.SubConstraint subfield = new RequiredConstraints.SubConstraint();
        subfield.setSubsub(new SubSubConstraint());
        bean.setSubfield(subfield);

        TestTextField field = new TestTextField();
        binder.bind(field, "subfield.subsub.value");
        binder.setBean(bean);

        assertTrue(field.isRequiredIndicatorVisible());
        testSerialization(binder);
    }

    @Test
    void subfield_name_valueCanBeValidated() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        TestTextField field = new TestTextField();

        binder.bind(field, "subfield.name");
        RequiredConstraints bean = new RequiredConstraints();
        bean.setSubfield(new SubConstraint());
        binder.setBean(bean);
        assertFalse(binder.validate().isOk());
        field.setValue("overfive");
        assertTrue(binder.validate().isOk());
    }

    @Test
    void subSubfield_name_valueCanBeValidated() {
        BeanValidationBinder<RequiredConstraints> binder = new BeanValidationBinder<>(
                RequiredConstraints.class);
        TestTextField field = new TestTextField();

        binder.bind(field, "subfield.subsub.value");
        RequiredConstraints bean = new RequiredConstraints();
        SubConstraint subfield = new SubConstraint();
        bean.setSubfield(subfield);
        subfield.setSubsub(new SubSubConstraint());
        binder.setBean(bean);

        assertFalse(binder.validate().isOk());
        field.setValue("overtencharacters");
        assertTrue(binder.validate().isOk());
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

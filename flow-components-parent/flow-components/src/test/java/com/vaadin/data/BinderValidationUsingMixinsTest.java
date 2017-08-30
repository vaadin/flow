/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.data;

import org.junit.Test;

import com.vaadin.ui.TextField;

/**
 * @author Vaadin Ltd.
 */
public class BinderValidationUsingMixinsTest {
    private final TextField firstNameField = new TextField();

    private static class TestBean {
        private String firstName;

        private TestBean(String firstName) {
            this.firstName = firstName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
    }

    @Test
    public void poc() {
        Binder<TestBean> binder = new Binder<>(TestBean.class);
        binder.forField(firstNameField)
                .withValidator(
                        firstName -> firstName != null && !firstName.isEmpty()
                                && firstName.toLowerCase().equals(firstName),
                        "First name should not be empty")
                .bind("firstName");

        TestBean testBean = new TestBean("one");
        binder.setBean(testBean);
        System.out.println("binder.validate().isOk()         = " + binder.validate().isOk());
        System.out.println("firstNameField.getValue()        = " + firstNameField.getValue());
        System.out.println("firstNameField.getErrorMessage() = " + firstNameField.getErrorMessage());
        System.out.println("firstNameField.isInvalid()       = " + firstNameField.isInvalid());
    }
}

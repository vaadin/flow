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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.vaadin.ui.TextField;

/**
 * @author Vaadin Ltd.
 */
public class BinderComponentsValidationTest {
    private static final String VALIDATION_ERROR_MESSAGE = "Text should not start with 2";

    private final TextField textField = new TextField();

    private static class TestBean {
        private String text;

        private TestBean(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    private Binder<TestBean> getTestBeanBinder() {
        Binder<TestBean> binder = new Binder<>(TestBean.class);
        binder.forField(textField)
                .withValidator(text -> text == null || !text.startsWith("2"),
                        VALIDATION_ERROR_MESSAGE)
                .bind("text");
        return binder;
    }

    private void assertBean(Binder<TestBean> binder,
            boolean valid) {
        assertThat("Unexpected binder state", binder.isValid(), is(valid));
        assertThat("Unexpected field state", textField.isInvalid(), is(!valid));
        if (valid) {
            assertThat("Expected web component to have no error message",
                    textField.getErrorMessage(), isEmptyOrNullString());
        } else {
            assertThat(
                    "Expected web component to have predefined error message",
                    textField.getErrorMessage(), is(VALIDATION_ERROR_MESSAGE));
        }
    }

    @Test
    public void setValidValue() {
        Binder<TestBean> binder = getTestBeanBinder();

        binder.setBean(new TestBean("one"));

        assertBean(binder, true);
    }

    @Test
    public void setInvalidValue() {
        Binder<TestBean> binder = getTestBeanBinder();

        binder.setBean(new TestBean("2"));

        assertBean(binder, false);
    }

    @Test
    public void switchValidation() {
        Binder<TestBean> binder = getTestBeanBinder();
        TestBean validBean = new TestBean("one");
        TestBean invalidBean = new TestBean("2");

        binder.setBean(validBean);
        assertBean(binder, true);

        binder.setBean(invalidBean);
        assertBean(binder, false);

        binder.setBean(validBean);
        assertBean(binder, true);
    }

    @Test
    public void nullBeanShouldClearValidation() {
        Binder<TestBean> binder = getTestBeanBinder();
        binder.setBean(new TestBean("2"));
        assertBean(binder, false);

        binder.setBean(null);

        assertBean(binder, true);
    }
}

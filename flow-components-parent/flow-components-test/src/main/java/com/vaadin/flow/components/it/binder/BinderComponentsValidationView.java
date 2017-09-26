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

package com.vaadin.flow.components.it.binder;

import com.vaadin.data.Binder;
import com.vaadin.flow.components.it.TestView;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.textfield.TextField;

/**
 * @author Vaadin Ltd.
 */
public class BinderComponentsValidationView extends TestView {
    public static final String VALIDATION_ERROR_MESSAGE = "Text should not start with 2";
    public static final String TEXT_FIELD_ID = "textField";
    public static final String CHANGE_CORRECT_BUTTON_ID = "changeCorrect";
    public static final String INITIAL_TEXT = "one";
    public static final String CORRECT_TEXT = "two";

    /**
     * A bean that is used to test a binder.
     */
    public static class TestBean {
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

    /**
     * Creates a view with a binder that has a verification rule.
     */
    public BinderComponentsValidationView() {
        Binder<TestBean> binder = new Binder<>(TestBean.class);
        TextField field = new TextField();
        field.setId(TEXT_FIELD_ID);

        binder.forField(field)
                .withValidator(text -> text == null || !text.startsWith("2"),
                        VALIDATION_ERROR_MESSAGE)
                .bind("text");
        binder.setBean(new TestBean(INITIAL_TEXT));

        Button setCorrectText = new Button("Set correct text into input",
                event -> binder.setBean(new TestBean(CORRECT_TEXT)));
        setCorrectText.setId(CHANGE_CORRECT_BUTTON_ID);

        add(field, setCorrectText);
    }
}

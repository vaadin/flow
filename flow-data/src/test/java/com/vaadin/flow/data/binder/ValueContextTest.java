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

import java.util.Locale;
import java.util.Objects;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.testcomponents.TestDatePicker;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;

public class ValueContextTest extends UI {

    private static final Locale UI_LOCALE = Locale.GERMAN;
    private static final Locale COMPONENT_LOCALE = Locale.FRENCH;
    private TestTextField textField;

    private Binder<PasswordBean> binder;
    private TestTextField passwordField;
    private TestTextField confirmPasswordField;

    @Test
    public void locale_from_component() {
        setLocale(COMPONENT_LOCALE);
        ValueContext fromComponent = new ValueContext(textField);
        Locale locale = fromComponent.getLocale().orElse(null);
        Objects.requireNonNull(locale);
        Assert.assertEquals("Unexpected locale from component",
                COMPONENT_LOCALE, locale);
    }

    @Test
    public void locale_from_ui() {
        ValueContext fromComponent = new ValueContext(textField);
        Locale locale = fromComponent.getLocale().orElse(null);
        Objects.requireNonNull(locale);
        Assert.assertEquals("Unexpected locale from component", UI_LOCALE,
                locale);
    }

    @Test
    public void default_locale() {
        setLocale(Locale.getDefault());
        ValueContext fromComponent = new ValueContext(textField);
        Locale locale = fromComponent.getLocale().orElse(null);
        Objects.requireNonNull(locale);
        Assert.assertEquals("Unexpected locale from component",
                Locale.getDefault(), locale);
    }

    @Test
    public void testHasValue1() {
        setLocale(Locale.getDefault());
        ValueContext fromComponent = new ValueContext(textField);
        Assert.assertEquals(textField, fromComponent.getHasValue().get());
    }

    @Test
    public void testHasValue2() {
        setLocale(Locale.getDefault());
        ValueContext fromComponent = new ValueContext(new TestDatePicker(),
                textField);
        Assert.assertEquals(textField, fromComponent.getHasValue().get());
    }

    @Test
    public void testHasValue3() {
        setLocale(Locale.getDefault());
        ValueContext fromComponent = new ValueContext(new TestDatePicker(),
                textField, Locale.CANADA);
        Assert.assertEquals(textField, fromComponent.getHasValue().get());
        Assert.assertEquals(Locale.CANADA, fromComponent.getLocale().get());
    }

    @Test
    public void getLocale_localeComesFromComponentUI() {
        UI.setCurrent(null);

        UI ui = new UI();
        ui.setLocale(Locale.GERMAN);

        Text text = new Text("");
        ui.add(text);
        ValueContext context = new ValueContext(text);

        Assert.assertEquals(Locale.GERMAN, context.getLocale().get());
    }

    @Test
    public void testWithBinder() {

        // Test password mismatch
        PasswordBean passwordBean = new PasswordBean();
        binder.setBean(passwordBean);
        passwordField.setValue("abc123");
        confirmPasswordField.setValue("def456");
        BinderValidationStatus<PasswordBean> status = binder.validate();
        Assert.assertEquals(1, status.getFieldValidationErrors().size());
        Assert.assertEquals(status.getFieldValidationErrors().iterator().next()
                .getMessage().get(), "Passwords must match");

        // Test password match
        confirmPasswordField.setValue("abc123");
        status = binder.validate();
        Assert.assertEquals(0, status.getFieldValidationErrors().size());
    }

    @Before
    public void setUp() {
        setLocale(UI_LOCALE);
        UI.setCurrent(this);
        textField = new TestTextField();
        add(textField);

        binder = new Binder<>(PasswordBean.class);
        passwordField = new TestTextField();
        confirmPasswordField = new TestTextField();
        binder.forField(passwordField).bind("password");
        binder.forField(confirmPasswordField)
                .withValidator((confirmValue, valueContext) -> {
                    Binder<?> ctxBinder = valueContext.getBinder().get();
                    Assert.assertSame(ctxBinder, binder);
                    TestTextField passwordField = (TestTextField) ctxBinder
                            .getBinding("password").get().getField();
                    return !Objects.equals(confirmValue,
                            passwordField.getValue())
                                    ? ValidationResult
                                            .error("Passwords must match")
                                    : ValidationResult.ok();
                }).bind("confirmPassword");
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Override
    public void init(VaadinRequest request) {
    }

    // PasswordBean

    public static class PasswordBean {

        private String password;
        private String confirmPassword;

        public String getPassword() {
            return this.password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public String getConfirmPassword() {
            return this.confirmPassword;
        }

        public void setConfirmPassword(final String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}

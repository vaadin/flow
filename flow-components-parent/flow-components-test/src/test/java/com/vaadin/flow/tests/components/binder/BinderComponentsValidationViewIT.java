///*
// * Copyright 2000-2017 Vaadin Ltd.
// *
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not
// * use this file except in compliance with the License. You may obtain a copy of
// * the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// * License for the specific language governing permissions and limitations under
// * the License.
// */
//
//package com.vaadin.flow.tests.components.binder;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.isEmptyOrNullString;
//
//import org.junit.Test;
//import org.openqa.selenium.WebElement;
//
//import com.vaadin.flow.components.it.binder.BinderComponentsValidationView;
//import com.vaadin.flow.tests.components.AbstractComponentIT;
//import org.openqa.selenium.By;
//
///**
// * @author Vaadin Ltd.
// */
//public class BinderComponentsValidationViewIT extends AbstractComponentIT {
//
//    // each test creates a new Chrome instance, so it's easier to verify a
//    // component this way
//    @Test
//    public void inputCorrectValue() {
//        open();
//        WebElement textField = findElement(
//                By.id(BinderComponentsValidationView.TEXT_FIELD_ID));
//        assertFieldValue(textField,
//                BinderComponentsValidationView.INITIAL_TEXT);
//
//        String correctInput = "bbbc90ef149427d9093dc8db60a5af9777a26c4a";
//        inputAndValidate(textField, correctInput, true);
//
//        // see BinderComponentsValidationView for validation details
//        inputAndValidate(textField, '2' + correctInput, false);
//
//        // disabled due to the bug https://github.com/vaadin/flow/issues/2460 -
//        // invalid fields can not be updated by Binder
//        // updateFromServerAndValidate(textField);
//    }
//
//    private void inputAndValidate(WebElement textField, String input,
//            boolean valid) {
//        cleanInputField(textField);
//        textField.sendKeys(input);
//        assertValid(textField, valid);
//    }
//
//    private void cleanInputField(WebElement textField) {
//        getCommandExecutor().executeScript(
//                "arguments[0][arguments[1]]=arguments[3]", textField, "value",
//                "");
//        assertFieldValue(textField, null);
//        assertValid(textField, true);
//    }
//
//    private void assertFieldValue(WebElement textField, String expectedValue) {
//        assertThat("Unexpected text in the text field component",
//                textField.getAttribute("value"), is(expectedValue));
//    }
//
//    private void assertValid(WebElement textField, boolean valid) {
//        if (valid) {
//            assertThat("Unexpected text in the text field component",
//                    Boolean.parseBoolean(textField.getAttribute("invalid")),
//                    is(!valid));
//        }
//
//        if (valid) {
//            assertThat("Unexpected text in the text field component",
//                    textField.getAttribute("errorMessage"),
//                    isEmptyOrNullString());
//        } else {
//            assertThat("Unexpected text in the text field component",
//                    textField.getAttribute("errorMessage"),
//                    is(BinderComponentsValidationView.VALIDATION_ERROR_MESSAGE));
//        }
//    }
//
//    private void updateFromServerAndValidate(WebElement textField) {
//        findElement(
//                By.id(BinderComponentsValidationView.CHANGE_CORRECT_BUTTON_ID))
//                        .click();
//
//        waitUntil(driver -> {
//            return BinderComponentsValidationView.CORRECT_TEXT
//                    .equals(textField.getAttribute("value"));
//        });
//        assertValid(textField, true);
//    }
//}

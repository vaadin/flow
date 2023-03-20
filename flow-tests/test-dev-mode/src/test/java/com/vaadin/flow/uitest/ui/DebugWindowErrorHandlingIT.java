/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.testutil.DevModeGizmoElement;
import com.vaadin.testbench.TestBenchElement;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

public class DebugWindowErrorHandlingIT extends ChromeBrowserTest {

    @Test
    public void clientSideErrorsReported() {
        open();
        clientSideError();
        DevModeGizmoElement gizmo = $(DevModeGizmoElement.class).first();
        gizmo.waitForErrorMessage(text -> text.equals("Client side error"));
        Assert.assertTrue(gizmo.isExpanded());
    }

    @Test
    public void clientSideExceptionReported() {
        open();
        clientSideException();
        DevModeGizmoElement gizmo = $(DevModeGizmoElement.class).first();
        gizmo.waitForErrorMessage(text -> text.matches(
                "Uncaught TypeError: Cannot read properties of null \\(reading 'foo'\\).*"));
    }

    @Test
    public void clientSidePromiseRejectionReported() {
        open();
        clientSidePromiseRejection();
        DevModeGizmoElement gizmo = $(DevModeGizmoElement.class).first();
        gizmo.waitForErrorMessage(text -> text.matches(
                "TypeError: Failed to fetch dynamically imported module: .*this-file-does-not-exist.js"));
    }

    @Test
    public void execJSExceptionReported() {
        open();
        execJSException();
        DevModeGizmoElement gizmo = $(DevModeGizmoElement.class).first();
        gizmo.waitForErrorMessage(text -> text.equals(
                "Exception is thrown during JavaScript execution. Stacktrace will be dumped separately."));
        gizmo.waitForErrorMessage(text -> text
                .equals("The error has occurred in the JS code: 'null.foo'"));
        gizmo.waitForErrorMessage(text -> text.matches(
                "Uncaught TypeError: Cannot read properties of null \\(reading 'foo'\\).*"));
    }

    @Test
    public void numberOfLogRowsLimited() {
        open();
        DevModeGizmoElement gizmo = $(DevModeGizmoElement.class).first();
        gizmo.expand();
        causeErrors("1001");
        gizmo.waitForLastErrorMessageToMatch(msg -> msg.equals("Error 1001"));

        Assert.assertEquals("Error 2", gizmo.getFirstErrorLogRow());
        Assert.assertEquals("Error 1001", gizmo.getLastErrorLogRow());
        Assert.assertEquals(1000, gizmo.getNumberOfErrorLogRows());

        causeErrors("2");
        gizmo.waitForLastErrorMessageToMatch(msg -> msg.equals("Error 2"));
        Assert.assertEquals("Error 4", gizmo.getFirstErrorLogRow());
        Assert.assertEquals("Error 2", gizmo.getLastErrorLogRow());
    }

    private void causeErrors(String nr) {
        TestBenchElement input = $("input")
                .id(DebugWindowErrorHandlingView.NUMBER_OF_ERRORS_ID);
        // input.setProperty("value", i);
        input.clear();
        input.sendKeys(nr);
        input.sendKeys(Keys.TAB);
        $(NativeButtonElement.class)
                .id(DebugWindowErrorHandlingView.CAUSE_ERRORS_ID).click();
    }

    private void clientSideException() {
        findElement(
                By.id(DebugWindowErrorHandlingView.CLIENT_SIDE_EXCEPTION_ID))
                        .click();
    }

    private void clientSidePromiseRejection() {
        findElement(By.id(
                DebugWindowErrorHandlingView.CLIENT_SIDE_PROMISE_REJECTION_ID))
                        .click();
    }

    private void execJSException() {
        findElement(By.id(DebugWindowErrorHandlingView.EXEC_JS_EXCEPTION_ID))
                .click();
    }

    private void clientSideError() {
        findElement(By.id(DebugWindowErrorHandlingView.CLIENT_SIDE_ERROR_ID))
                .click();
    }

}

/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.NativeDetailsElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class AllowInertSynchronizedPropertyIT extends ChromeBrowserTest {

    private TestBenchElement modalDialogButton;

    @Override
    protected void open(String... parameters) {
        super.open(parameters);
        modalDialogButton = $(NativeButtonElement.class)
                .id(ModalDialogView.OPEN_MODAL_BUTTON);
    }

    @Test
    public void modalDialogOpened_toggleNativeDetailsVisibility_allowInertSynchronizedPropertyShouldChange() {
        open();

        modalDialogButton.click();

        $(NativeButtonElement.class).id(
                AllowInertSynchronizedPropertyView.READ_NATIVE_DETAILS_STATE_BUTTON)
                .click();

        Assert.assertEquals("closed", getStateText());

        $(NativeDetailsElement.class).first().findElement(By
                .id(AllowInertSynchronizedPropertyView.NATIVE_DETAILS_SUMMARY))
                .click();
        $(NativeButtonElement.class).id(
                AllowInertSynchronizedPropertyView.READ_NATIVE_DETAILS_STATE_BUTTON)
                .click();

        Assert.assertEquals("opened", getStateText());
    }

    private String getStateText() {
        return $(SpanElement.class)
                .id(AllowInertSynchronizedPropertyView.NATIVE_DETAILS_STATE)
                .getText();
    }
}

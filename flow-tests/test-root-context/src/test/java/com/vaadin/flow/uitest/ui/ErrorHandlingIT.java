package com.vaadin.flow.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ErrorHandlingIT extends ChromeBrowserTest {

    @Test
    public void exceptionInClickHandlerDoesNotCauseInternalError() {
        open();
        $(NativeButtonElement.class).id("errorButton").click();

        assertNoSystemErrors();

        List<DivElement> errors = $(DivElement.class)
                .attributeContains("class", "error").all();
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(
                "An error occurred: java.lang.IllegalStateException: Intentional fail in click handler",
                errors.get(0).getText());
    }

    private void assertNoSystemErrors() {
        Assert.assertEquals(0,
                findElements(By.className("v-system-error")).size());

    }

}

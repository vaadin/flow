package com.vaadin.flow.uitest.ui.push;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.TestTag;
import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;

@Tag(TestTag.PUSH_TESTS)
public class PushErrorHandlingIT extends ChromeBrowserTest {

    @BrowserTest
    public void errorHandling() {
        open();
        findElement(By.id("npeButton")).click();
        Assertions.assertEquals(
                "An error! class java.lang.NullPointerException",
                findElement(By.className("error")).getText());
    }
}

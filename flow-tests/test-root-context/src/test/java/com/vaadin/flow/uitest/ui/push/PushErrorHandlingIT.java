package com.vaadin.flow.uitest.ui.push;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(PushTests.class)
public class PushErrorHandlingIT extends ChromeBrowserTest {

    @Test
    public void errorHandling() {
        open();
        findElement(By.id("npeButton")).click();
        Assert.assertEquals("An error! class java.lang.NullPointerException",
                findElement(By.className("error")).getText());
    }
}

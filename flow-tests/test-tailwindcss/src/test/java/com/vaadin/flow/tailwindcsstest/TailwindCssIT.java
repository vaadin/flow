package com.vaadin.flow.tailwindcsstest;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class TailwindCssIT extends ChromeBrowserTest {

    @Test
    public void tailwindCssWorks_builtin() {
        var view = openView();
        String viewBackground = view.getCssValue("backgroundColor");
        Assert.assertEquals("oklch(0.967 0.003 264.542)", viewBackground);

        var h1 = view.findElement(By.tagName("h1"));
        Assert.assertEquals("Tailwind CSS does work!", h1.getText());
    }

    private WebElement openView() {
        open();
        waitForDevServer();
        return findElement(By.cssSelector(".bg-gray-100"));
    }
}

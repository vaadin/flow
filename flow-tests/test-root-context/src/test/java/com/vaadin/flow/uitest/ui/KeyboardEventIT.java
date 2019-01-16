package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 *
 * @author Vaadin Ltd.
 * @since
 */
public class KeyboardEventIT extends ChromeBrowserTest {
    @Test
    public void verify_both_key_and_code_are_set_correctly() {
        open();

        // make sure both elements are present
        Assert.assertTrue(isElementPresent(By.id("input")));
        Assert.assertTrue(isElementPresent(By.id("paragraph")));

        WebElement paragraph = findElement(By.id("paragraph"));

        String innerHTML = paragraph.getText();

        // Both .key and .code are correctly set
        Assert.assertEquals(
                "Q:KeyQ;Q:;Q:;",
                innerHTML
        );
    }
}

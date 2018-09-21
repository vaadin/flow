package com.vaadin.flow.uitest.ui.template.collections;

import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ToggleNullListIT extends ChromeBrowserTest {

    @Test
    public void shouldBeToggledWithNoClientErrors() {
        open();

        WebElement toggleButton = findElement(
                By.id(ToggleNullListView.TOGGLE_BUTTON_ID));

        for (int i = 0; i < 100; i++) {
            assertFalse(String.format(
                    "Failed %s the template with null list in the model after '%s' button click(s)",
                    i % 2 == 0 ? "attaching" : "reattaching", i),
                    isElementPresent(By.className("v-system-error")));
            toggleButton.click();
        }
    }
}

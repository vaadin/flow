package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class MultipleNpmPackageAnnotationsIT extends ChromeBrowserTest {
    @Before
    public void init() {
        open();
    }

    @Test
    public void pageShouldContainTwoPaperComponents() {
        WebElement paperInput = findElement(By.tagName("paper-input"));
        WebElement paperCheckbox = findElement(By.tagName("paper-checkbox"));

        Assert.assertNotNull(paperInput);
        Assert.assertNotNull(paperCheckbox);

        // check that the elements are upgraded by checking their shadow-roots
        WebElement paperContainer = getInShadowRoot(paperInput, By.id(
                "container"));

        WebElement checkboxContainer = getInShadowRoot(paperCheckbox, By.id(
                "checkboxContainer"));

        Assert.assertNotNull(paperContainer);
        Assert.assertNotNull(checkboxContainer);
    }
}

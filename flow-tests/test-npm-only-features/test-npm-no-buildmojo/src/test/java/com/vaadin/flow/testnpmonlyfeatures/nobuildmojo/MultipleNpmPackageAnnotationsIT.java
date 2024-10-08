package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class MultipleNpmPackageAnnotationsIT extends ChromeBrowserTest {
    @Before
    public void init() {
        open();
    }

    @Test
    public void pageShouldContainTwoPaperComponents() {
        TestBenchElement paperInput = $("paper-input").first();
        TestBenchElement paperCheckbox = $("paper-checkbox").first();

        // check that elements are on the page
        Assert.assertNotNull(paperInput);
        Assert.assertNotNull(paperCheckbox);

        // verify that the paper components are upgraded
        Assert.assertNotNull(paperInput.$("paper-input-container"));
        Assert.assertNotNull(paperCheckbox.$("checkboxContainer"));
    }

    // Tests funtionaity of TaskCopyLocalFrontendFiles
    @Test
    public void lazyComponentShouldExistInBody() {
        waitForElementPresent(By.id("lazy-element"));
        WebElement element = findElement(By.id("lazy-element"));

        Assert.assertTrue("Lazy created element should be displayed", element.isDisplayed());
    }
}

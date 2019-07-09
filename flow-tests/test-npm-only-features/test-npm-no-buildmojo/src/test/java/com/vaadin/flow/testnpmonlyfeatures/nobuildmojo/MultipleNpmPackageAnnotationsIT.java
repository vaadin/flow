package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class MultipleNpmPackageAnnotationsIT extends ChromeBrowserTest {
    @Before
    public void init() {
        open();
    }

    @Test
    public void pageShouldContainTwoPaperComponents() {
        TestBenchElement paperInput = $(TestBenchElement.class).id("input");
        TestBenchElement paperCheckbox = $(TestBenchElement.class).id(
                "checkbox");

        Assert.assertNotNull(paperInput);
        Assert.assertNotNull(paperCheckbox);

        // check that the elements are upgraded by checking their shadow-roots

        TestBenchElement paperContainer = paperInput.findElement(By.id(
                "container"));

        TestBenchElement checkboxContainer = paperCheckbox.findElement(By.id(
                "checkboxContainer"));

        Assert.assertNotNull(paperContainer);
        Assert.assertNotNull(checkboxContainer);
    }
}

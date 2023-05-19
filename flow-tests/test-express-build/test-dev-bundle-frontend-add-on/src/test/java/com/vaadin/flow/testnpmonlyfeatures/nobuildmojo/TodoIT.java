package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class TodoIT extends ChromeBrowserTest {

    @Before
    public void init() {
        open();
    }

    @Override
    protected String getTestPath() {
        return "/view/com.vaadin.flow.frontend.TodoView";
    }

    @Test
    public void testAddOn() {
        TestBenchElement template = $(TestBenchElement.class).id("template");

        TestBenchElement createTemplate = template.$(TestBenchElement.class)
                .id("creator");

        TestBenchElement todo = createTemplate.$(TestBenchElement.class)
                .id("task-input");
        todo.sendKeys("Important task");

        TestBenchElement user = createTemplate.$(TestBenchElement.class)
                .id("user-name-input");
        user.sendKeys("Teuvo testi");

        TestBenchElement createButton = createTemplate.$(TestBenchElement.class)
                .id("create-button");
        createButton.click();

        TestBenchElement todoElement = template
                .findElement(By.tagName("todo-element"));
        Assert.assertEquals("Important task",
                todoElement.$(TestBenchElement.class).id("task").getText());

    }

    @Test
    public void bundleCreated() {
        File baseDir = new File(System.getProperty("user.dir", "."));

        // should create a dev-bundle
        Assert.assertTrue("New devBundle should be generated",
                new File(baseDir, Constants.DEV_BUNDLE_LOCATION).exists());
    }
}

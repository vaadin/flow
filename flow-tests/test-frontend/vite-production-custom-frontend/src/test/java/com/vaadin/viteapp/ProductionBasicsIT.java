package com.vaadin.viteapp;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ProductionBasicsIT extends ChromeBrowserTest {

    @BeforeClass
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @Test
    public void applicationStarts() {
        getDriver().get(getRootURL());
        waitForDevServer();
        TestBenchElement testForm = $("test-form").first();
        Assert.assertEquals("foo", testForm.getText());
    }

}
